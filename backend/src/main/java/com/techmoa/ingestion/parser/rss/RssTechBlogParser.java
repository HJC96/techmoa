package com.techmoa.ingestion.parser.rss;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techmoa.common.url.UrlResolver;
import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import com.techmoa.ingestion.parser.TechBlogParser;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RssTechBlogParser implements TechBlogParser {

    private static final Logger log = LoggerFactory.getLogger(RssTechBlogParser.class);

    @Override
    public boolean supports(ParserType parserType) {
        return ParserType.RSS == parserType;
    }

    @Override
    public List<ParsedPost> fetch(SourceProfile sourceProfile) {
        if (sourceProfile.feedUrl() == null || sourceProfile.feedUrl().isBlank()) {
            log.warn("RSS source has no feedUrl. sourceName={}", sourceProfile.sourceName());
            return Collections.emptyList();
        }

        try (XmlReader reader = new XmlReader(new URL(sourceProfile.feedUrl()))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            return feed.getEntries().stream()
                    .map(entry -> toParsedPost(entry, sourceProfile))
                    .filter(post -> post.canonicalUrl() != null && !post.canonicalUrl().isBlank())
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse RSS feed. sourceName=" + sourceProfile.sourceName(),
                    e
            );
        }
    }

    private ParsedPost toParsedPost(SyndEntry entry, SourceProfile sourceProfile) {
        String canonicalUrl = entry.getLink();

        return new ParsedPost(
                entry.getTitle(),
                canonicalUrl,
                entry.getDescription() == null ? null : entry.getDescription().getValue(),
                entry.getAuthor(),
                resolveThumbnailUrl(entry, canonicalUrl, sourceProfile.baseUrl()),
                resolvePublishedAt(entry.getPublishedDate(), entry.getUpdatedDate()),
                resolveTags(entry.getCategories())
        );
    }

    private LocalDateTime resolvePublishedAt(Date publishedDate, Date updatedDate) {
        Date resolvedDate = publishedDate == null ? updatedDate : publishedDate;
        if (resolvedDate == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(resolvedDate.toInstant(), ZoneId.systemDefault());
    }

    private List<String> resolveTags(List<SyndCategory> categories) {
        if (categories == null) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(SyndCategory::getName)
                .filter(Objects::nonNull)
                .toList();
    }

    private String resolveThumbnailUrl(SyndEntry entry, String canonicalUrl, String sourceBaseUrl) {
        String enclosureImage = resolveThumbnailFromEnclosures(entry.getEnclosures(), canonicalUrl, sourceBaseUrl);
        if (enclosureImage != null) {
            return enclosureImage;
        }

        String descriptionImage = resolveThumbnailFromHtml(
                entry.getDescription() == null ? null : entry.getDescription().getValue(),
                canonicalUrl,
                sourceBaseUrl
        );
        if (descriptionImage != null) {
            return descriptionImage;
        }

        if (entry.getContents() == null) {
            return null;
        }

        for (SyndContent content : entry.getContents()) {
            String contentImage = resolveThumbnailFromHtml(
                    content == null ? null : content.getValue(),
                    canonicalUrl,
                    sourceBaseUrl
            );
            if (contentImage != null) {
                return contentImage;
            }
        }

        return null;
    }

    private String resolveThumbnailFromEnclosures(
            List<SyndEnclosure> enclosures,
            String canonicalUrl,
            String sourceBaseUrl
    ) {
        if (enclosures == null) {
            return null;
        }

        return enclosures.stream()
                .filter(Objects::nonNull)
                .filter(enclosure -> enclosure.getUrl() != null && !enclosure.getUrl().isBlank())
                .filter(this::isImageEnclosure)
                .map(SyndEnclosure::getUrl)
                .map(String::trim)
                .map(url -> UrlResolver.resolveAbsoluteUrl(url, canonicalUrl, sourceBaseUrl))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean isImageEnclosure(SyndEnclosure enclosure) {
        if (enclosure.getType() == null || enclosure.getType().isBlank()) {
            return true;
        }
        return enclosure.getType().toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private String resolveThumbnailFromHtml(String html, String canonicalUrl, String sourceBaseUrl) {
        if (html == null || html.isBlank()) {
            return null;
        }

        return Jsoup.parse(html)
                .select("img")
                .stream()
                .map(this::resolveImageCandidate)
                .filter(Objects::nonNull)
                .map(url -> UrlResolver.resolveAbsoluteUrl(url, canonicalUrl, sourceBaseUrl))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String resolveImageCandidate(Element imageElement) {
        String[] candidateAttributes = {"data-src", "data-original", "data-lazy-src", "src"};
        for (String attribute : candidateAttributes) {
            String value = imageElement.attr(attribute);
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("data:") || lower.startsWith("javascript:")) {
                continue;
            }
            return trimmed;
        }
        return null;
    }
}
