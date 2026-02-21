package com.techmoa.post.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.post.domain.Post;
import com.techmoa.post.domain.PostRepository;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import com.techmoa.tag.domain.TagRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import(PostUpsertService.class)
class PostUpsertServiceTest {

    @Autowired
    private PostUpsertService postUpsertService;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Test
    void upsert_savesPostAndTags() {
        Source source = sourceRepository.save(new Source(
                "테스트소스",
                "https://example.com",
                "https://example.com/feed.xml",
                ParserType.RSS,
                30,
                true
        ));

        ParsedPost parsedPost = new ParsedPost(
                "테스트 글",
                "https://example.com/post-1",
                "요약",
                "작성자",
                LocalDateTime.of(2026, 2, 20, 10, 0),
                List.of("Java", "Spring")
        );

        int savedCount = postUpsertService.upsert(source, List.of(parsedPost));

        assertThat(savedCount).isEqualTo(1);
        assertThat(postRepository.findAll()).hasSize(1);
        assertThat(tagRepository.findAll()).hasSize(2);
    }

    @Test
    void upsert_updatesExistingPostByCanonicalUrl() {
        Source source = sourceRepository.save(new Source(
                "테스트소스",
                "https://example.com",
                "https://example.com/feed.xml",
                ParserType.RSS,
                30,
                true
        ));

        ParsedPost first = new ParsedPost(
                "첫 제목",
                "https://example.com/post-1",
                "요약1",
                "작성자1",
                LocalDateTime.of(2026, 2, 20, 10, 0),
                List.of("Java")
        );
        ParsedPost second = new ParsedPost(
                "변경 제목",
                "https://example.com/post-1",
                "요약2",
                "작성자2",
                LocalDateTime.of(2026, 2, 20, 11, 0),
                List.of("Spring")
        );

        postUpsertService.upsert(source, List.of(first));
        postUpsertService.upsert(source, List.of(second));

        List<Post> posts = postRepository.findAll();
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getTitle()).isEqualTo("변경 제목");
        assertThat(tagRepository.findAll()).hasSize(2);
    }
}
