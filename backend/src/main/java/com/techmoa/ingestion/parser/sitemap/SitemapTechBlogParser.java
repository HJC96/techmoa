package com.techmoa.ingestion.parser.sitemap;

import com.techmoa.common.url.UrlResolver;
import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import com.techmoa.ingestion.parser.TechBlogParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SitemapTechBlogParser implements TechBlogParser {

    private static final Logger log = LoggerFactory.getLogger(SitemapTechBlogParser.class);

    private static final int CONNECTION_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_SITEMAP_FILES = 300;
    private static final int MAX_POST_URLS = 20_000;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public boolean supports(ParserType parserType) {
        return ParserType.SITEMAP == parserType;
    }

    @Override
    public List<ParsedPost> fetch(SourceProfile sourceProfile) {
        String sitemapUrl = resolveSitemapUrl(sourceProfile);
        Set<String> postUrls = collectPostUrls(sitemapUrl);

        List<ParsedPost> parsedPosts = new ArrayList<>();
        for (String postUrl : postUrls) {
            fetchPost(postUrl, sourceProfile.baseUrl()).ifPresent(parsedPosts::add);
        }
        return parsedPosts;
    }

    private String resolveSitemapUrl(SourceProfile sourceProfile) {
        if (sourceProfile.feedUrl() != null && !sourceProfile.feedUrl().isBlank()) {
            return sourceProfile.feedUrl().trim();
        }
        if (sourceProfile.baseUrl() == null || sourceProfile.baseUrl().isBlank()) {
            throw new IllegalArgumentException(
                    "Sitemap parser requires feedUrl or baseUrl. sourceName=" + sourceProfile.sourceName()
            );
        }
        return sourceProfile.baseUrl().replaceAll("/+$", "") + "/sitemap.xml";
    }

    private Set<String> collectPostUrls(String rootSitemapUrl) {
        Deque<String> pending = new ArrayDeque<>();
        Set<String> visitedSitemaps = new LinkedHashSet<>();
        Set<String> postUrls = new LinkedHashSet<>();
        pending.add(rootSitemapUrl);

        while (!pending.isEmpty()) {
            String sitemapUrl = pending.removeFirst();
            if (!visitedSitemaps.add(sitemapUrl)) {
                continue;
            }

            if (visitedSitemaps.size() > MAX_SITEMAP_FILES) {
                log.warn("Too many sitemap files. rootSitemapUrl={}", rootSitemapUrl);
                break;
            }

            Document sitemapDocument;
            try {
                sitemapDocument = fetchDocument(sitemapUrl, Parser.xmlParser());
            } catch (Exception e) {
                log.warn("Failed to read sitemap. sitemapUrl={}, message={}", sitemapUrl, e.getMessage());
                continue;
            }

            List<String> childSitemaps = extractLocValues(sitemapDocument, "sitemap").stream()
                    .map(value -> resolveUrl(sitemapUrl, value))
                    .toList();
            if (!childSitemaps.isEmpty()) {
                pending.addAll(childSitemaps);
                continue;
            }

            List<String> urls = extractLocValues(sitemapDocument, "url").stream()
                    .map(value -> resolveUrl(sitemapUrl, value))
                    .toList();

            for (String url : urls) {
                if (postUrls.size() >= MAX_POST_URLS) {
                    log.warn("Too many post URLs. rootSitemapUrl={}", rootSitemapUrl);
                    return postUrls;
                }
                postUrls.add(url);
            }
        }

        return postUrls;
    }

    private Optional<ParsedPost> fetchPost(String postUrl, String sourceBaseUrl) {
        try {
            Document document = fetchDocument(postUrl, Parser.htmlParser());
            String title = firstNonBlank(
                    metaContent(document, "meta[property=og:title]"),
                    metaContent(document, "meta[name=twitter:title]"),
                    document.title(),
                    text(document, "article h1"),
                    text(document, "main h1"),
                    text(document, "h1")
            );
            if (title == null) {
                log.debug("Skipping post with empty title. postUrl={}", postUrl);
                return Optional.empty();
            }

            String canonicalUrl = firstNonBlank(
                    attr(document, "link[rel=canonical]", "href"),
                    metaContent(document, "meta[property=og:url]"),
                    postUrl
            );
            canonicalUrl = firstNonBlank(
                    UrlResolver.resolveAbsoluteUrl(canonicalUrl, postUrl, sourceBaseUrl),
                    postUrl
            );

            String summary = firstNonBlank(
                    metaContent(document, "meta[name=description]"),
                    metaContent(document, "meta[property=og:description]"),
                    text(document, "article p"),
                    text(document, "main p")
            );
            String author = firstNonBlank(
                    metaContent(document, "meta[name=author]"),
                    metaContent(document, "meta[property=article:author]"),
                    metaContent(document, "meta[name=twitter:creator]")
            );
            String thumbnailUrl = firstNonBlank(
                    metaContent(document, "meta[property=og:image]"),
                    metaContent(document, "meta[name=twitter:image]"),
                    attr(document, "article img[src]", "src"),
                    attr(document, "main img[src]", "src")
            );
            thumbnailUrl = UrlResolver.resolveAbsoluteUrl(thumbnailUrl, canonicalUrl, postUrl, sourceBaseUrl);
            LocalDateTime publishedAt = resolvePublishedAt(document).orElse(LocalDateTime.now());
            List<String> tags = resolveTags(document);

            return Optional.of(new ParsedPost(
                    title,
                    canonicalUrl,
                    summary,
                    author,
                    thumbnailUrl,
                    publishedAt,
                    tags
            ));
        } catch (Exception e) {
            log.warn("Failed to parse post page. postUrl={}, message={}", postUrl, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> resolvePublishedAt(Document document) {
        List<String> candidates = new ArrayList<>();
        candidates.add(metaContent(document, "meta[property=article:published_time]"));
        candidates.add(metaContent(document, "meta[name=article:published_time]"));
        candidates.add(metaContent(document, "meta[name=publish_date]"));
        candidates.add(metaContent(document, "meta[name=pubdate]"));
        candidates.add(attr(document, "time[datetime]", "datetime"));

        for (String raw : candidates) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            LocalDateTime parsed = parseDateTime(raw);
            if (parsed != null) {
                return Optional.of(parsed);
            }
        }

        return Optional.empty();
    }

    private LocalDateTime parseDateTime(String raw) {
        String value = raw.trim();
        if (value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(Instant.parse(value), DEFAULT_ZONE);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME_SECONDS);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME_MINUTES);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception ignored) {
        }

        return null;
    }

    private List<String> resolveTags(Document document) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        document.select("meta[property=article:tag], meta[name=article:tag], meta[name=keywords]")
                .forEach(element -> {
                    String content = normalizeText(element.attr("content"));
                    if (content == null) {
                        return;
                    }
                    String[] tokens = content.split("[,|]");
                    for (String token : tokens) {
                        String normalized = normalizeText(token);
                        if (normalized != null) {
                            result.add(normalized);
                        }
                    }
                });
        return List.copyOf(result);
    }

    private String metaContent(Document document, String selector) {
        return normalizeText(attr(document, selector, "content"));
    }

    private String text(Document document, String selector) {
        return normalizeText(document.select(selector).stream()
                .map(element -> element.text())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null));
    }

    private String attr(Document document, String selector, String attrName) {
        return normalizeText(document.select(selector).stream()
                .map(element -> element.attr(attrName))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\u00A0', ' ').trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private Document fetchDocument(String url, Parser parser) throws IOException {
        URLConnection connection = openConnection(url);
        try (InputStream inputStream = connection.getInputStream()) {
            return Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), url, parser);
        }
    }

    private URLConnection openConnection(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        try {
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
        } catch (UnsupportedOperationException ignored) {
        }
        if (connection instanceof HttpURLConnection http) {
            http.setRequestProperty("User-Agent", "TechmoaBot/1.0");
            http.setRequestProperty("Accept-Language", Locale.KOREAN.toLanguageTag());
            http.setInstanceFollowRedirects(true);
        }
        return connection;
    }

    private String resolveUrl(String baseUrl, String rawUrl) {
        try {
            return URI.create(baseUrl).resolve(rawUrl).toString();
        } catch (Exception e) {
            return rawUrl;
        }
    }

    private List<String> extractLocValues(Document document, String containerTagName) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Element element : document.getAllElements()) {
            if (!hasTagName(element, containerTagName)) {
                continue;
            }

            for (Element child : element.children()) {
                if (!hasTagName(child, "loc")) {
                    continue;
                }
                String value = normalizeText(child.text());
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private boolean hasTagName(Element element, String expectedName) {
        String tagName = element.tagName().toLowerCase(Locale.ROOT);
        String lowerExpected = expectedName.toLowerCase(Locale.ROOT);
        return tagName.equals(lowerExpected) || tagName.endsWith(":" + lowerExpected);
    }

    public Optional<String> discoverSitemapUrlFromRobots(String baseUrl) {
        String robotsUrl = baseUrl.replaceAll("/+$", "") + "/robots.txt";
        try {
            URLConnection connection = openConnection(robotsUrl);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.toLowerCase(Locale.ROOT).startsWith("sitemap:")) {
                        continue;
                    }

                    String raw = trimmed.substring("sitemap:".length()).trim();
                    if (!raw.isBlank()) {
                        return Optional.of(resolveUrl(baseUrl, raw));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to discover sitemap from robots. robotsUrl={}, message={}", robotsUrl, e.getMessage());
        }
        return Optional.empty();
    }
}
