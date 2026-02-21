package com.techmoa.ingestion.parser.rss;

import static org.assertj.core.api.Assertions.assertThat;

import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

class RssTechBlogParserTest {

    private final RssTechBlogParser parser = new RssTechBlogParser();

    @Test
    void fetch_parsesRssFeed() {
        URL resource = getClass().getClassLoader().getResource("fixtures/sample-rss.xml");
        assertThat(resource).isNotNull();

        SourceProfile sourceProfile = new SourceProfile(
                1L,
                "test",
                "https://example.com",
                resource.toString(),
                ParserType.RSS
        );

        List<ParsedPost> parsedPosts = parser.fetch(sourceProfile);

        assertThat(parsedPosts).hasSize(2);
        assertThat(parsedPosts.get(0).title()).isEqualTo("Post A");
        assertThat(parsedPosts.get(0).canonicalUrl()).isEqualTo("https://example.com/a");
        assertThat(parsedPosts.get(0).thumbnailUrl()).isEqualTo("https://images.example.com/a.png");
        assertThat(parsedPosts.get(0).tags()).containsExactly("Java", "Spring");
        assertThat(parsedPosts.get(1).title()).isEqualTo("Post B");
        assertThat(parsedPosts.get(1).thumbnailUrl()).isEqualTo("https://images.example.com/b.jpg");
        assertThat(parsedPosts.get(1).tags()).containsExactly("React");
    }
}
