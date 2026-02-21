package com.techmoa.ingestion.parser.sitemap;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SitemapTechBlogParserTest {

    private final SitemapTechBlogParser parser = new SitemapTechBlogParser();

    @Test
    void fetch_parsesSitemapIndexAndPostPages() throws Exception {
        Path tempDir = Files.createTempDirectory("sitemap-parser-test");
        String canonicalA = "https://example.com/post-a-canonical";
        String canonicalB = "https://example.com/post-b-canonical";

        Path postAFile = tempDir.resolve("post-a.html");
        Files.writeString(postAFile, """
                <html>
                  <head>
                    <title>Fallback Title A</title>
                    <meta property="og:title" content="Post A"/>
                    <meta property="og:url" content="%s"/>
                    <meta name="description" content="Summary A"/>
                    <meta name="author" content="Author A"/>
                    <meta property="og:image" content="https://images.example.com/a.png"/>
                    <meta property="article:published_time" content="2025-01-01T09:30:00Z"/>
                    <meta property="article:tag" content="Java"/>
                    <meta property="article:tag" content="Spring"/>
                  </head>
                  <body><article><h1>Ignored h1</h1></article></body>
                </html>
                """.formatted(canonicalA));

        Path postBFile = tempDir.resolve("post-b.html");
        Files.writeString(postBFile, """
                <html>
                  <head>
                    <title>Post B Title</title>
                    <link rel="canonical" href="%s"/>
                    <meta name="keywords" content="Backend, Architecture"/>
                  </head>
                  <body>
                    <article>
                      <h1>Post B Heading</h1>
                      <p>Summary B from article body.</p>
                      <time datetime="2025-02-10T08:45:00+09:00">2025-02-10</time>
                    </article>
                  </body>
                </html>
                """.formatted(canonicalB));

        Path postsSitemap = tempDir.resolve("posts.xml");
        Files.writeString(postsSitemap, """
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url><loc>%s</loc></url>
                  <url><loc>%s</loc></url>
                </urlset>
                """.formatted(postAFile.toUri(), postBFile.toUri()));

        Path rootSitemap = tempDir.resolve("sitemap.xml");
        Files.writeString(rootSitemap, """
                <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <sitemap><loc>%s</loc></sitemap>
                </sitemapindex>
                """.formatted(postsSitemap.toUri()));

        SourceProfile sourceProfile = new SourceProfile(
                1L,
                "테스트소스",
                tempDir.toUri().toString(),
                rootSitemap.toUri().toString(),
                ParserType.SITEMAP
        );

        List<ParsedPost> parsedPosts = parser.fetch(sourceProfile);

        assertThat(parsedPosts).hasSize(2);

        ParsedPost first = parsedPosts.getFirst();
        assertThat(first.title()).isEqualTo("Post A");
        assertThat(first.canonicalUrl()).isEqualTo(canonicalA);
        assertThat(first.summary()).isEqualTo("Summary A");
        assertThat(first.author()).isEqualTo("Author A");
        assertThat(first.thumbnailUrl()).isEqualTo("https://images.example.com/a.png");
        assertThat(first.tags()).containsExactly("Java", "Spring");
        assertThat(first.publishedAt()).isEqualTo(
                LocalDateTime.ofInstant(Instant.parse("2025-01-01T09:30:00Z"), ZoneId.systemDefault())
        );

        ParsedPost second = parsedPosts.get(1);
        assertThat(second.title()).isEqualTo("Post B Title");
        assertThat(second.canonicalUrl()).isEqualTo(canonicalB);
        assertThat(second.summary()).isEqualTo("Summary B from article body.");
        assertThat(second.author()).isNull();
        assertThat(second.tags()).containsExactly("Backend", "Architecture");
        assertThat(second.publishedAt()).isEqualTo(
                OffsetDateTime.parse("2025-02-10T08:45:00+09:00")
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()
        );
    }

    @Test
    void discoverSitemapUrlFromRobots_extractsSitemapEntry() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/robots.txt", exchange -> respondText(exchange, """
                User-agent: *
                Allow: /
                Sitemap: /dynamic-sitemap.xml
                """));

        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            assertThat(parser.discoverSitemapUrlFromRobots(baseUrl))
                    .contains(baseUrl + "/dynamic-sitemap.xml");
        } finally {
            server.stop(0);
        }
    }

    private void respondXml(HttpExchange exchange, String body) throws IOException {
        respond(exchange, "application/xml; charset=utf-8", body);
    }

    private void respondHtml(HttpExchange exchange, String body) throws IOException {
        respond(exchange, "text/html; charset=utf-8", body);
    }

    private void respondText(HttpExchange exchange, String body) throws IOException {
        respond(exchange, "text/plain; charset=utf-8", body);
    }

    private void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }
}
