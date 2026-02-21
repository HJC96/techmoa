package com.techmoa.post.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.post.domain.Post;
import com.techmoa.source.domain.Source;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PostResponseThumbnailUrlTest {

    @Test
    void postItemResponse_resolvesRelativeThumbnailUrlToAbsolute() {
        Source source = new Source(
                "네이버테크",
                "https://d2.naver.com",
                "https://d2.naver.com/d2.atom",
                ParserType.RSS,
                30,
                true
        );
        Post post = new Post(
                source,
                "https://d2.naver.com/helloworld/1111",
                "title",
                "summary",
                "author",
                "/content/images/sample.png",
                LocalDateTime.now()
        );

        PostItemResponse response = PostItemResponse.from(post);

        assertThat(response.thumbnailUrl()).isEqualTo("https://d2.naver.com/content/images/sample.png");
    }

    @Test
    void postDetailResponse_resolvesRelativeThumbnailUrlToAbsolute() {
        Source source = new Source(
                "인프런테크",
                "https://tech.inflab.com",
                "https://tech.inflab.com/rss.xml",
                ParserType.RSS,
                30,
                true
        );
        Post post = new Post(
                source,
                "https://tech.inflab.com/2026-01-01",
                "title",
                "summary",
                "author",
                "/static/sample.png",
                LocalDateTime.now()
        );

        PostDetailResponse response = PostDetailResponse.from(post);

        assertThat(response.thumbnailUrl()).isEqualTo("https://tech.inflab.com/static/sample.png");
    }
}
