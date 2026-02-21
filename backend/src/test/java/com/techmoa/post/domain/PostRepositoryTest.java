package com.techmoa.post.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Test
    void findFeed_filtersByMultipleSourcesWithOrCondition() {
        Source sourceA = sourceRepository.save(new Source(
                "소스A",
                "https://a.example.com",
                "https://a.example.com/feed.xml",
                ParserType.RSS,
                30,
                true
        ));
        Source sourceB = sourceRepository.save(new Source(
                "소스B",
                "https://b.example.com",
                "https://b.example.com/feed.xml",
                ParserType.RSS,
                30,
                true
        ));
        Source sourceC = sourceRepository.save(new Source(
                "소스C",
                "https://c.example.com",
                "https://c.example.com/feed.xml",
                ParserType.RSS,
                30,
                true
        ));

        postRepository.save(new Post(
                sourceA,
                "https://a.example.com/post-1",
                "A 글",
                null,
                null,
                null,
                LocalDateTime.of(2026, 2, 21, 9, 0)
        ));
        postRepository.save(new Post(
                sourceB,
                "https://b.example.com/post-1",
                "B 글",
                null,
                null,
                null,
                LocalDateTime.of(2026, 2, 21, 9, 1)
        ));
        postRepository.save(new Post(
                sourceC,
                "https://c.example.com/post-1",
                "C 글",
                null,
                null,
                null,
                LocalDateTime.of(2026, 2, 21, 9, 2)
        ));

        List<Post> posts = postRepository.findFeed(
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(posts).hasSize(3);

        posts = postRepository.findFeedBySourceIds(
                null,
                List.of(sourceA.getId(), sourceB.getId()),
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(posts)
                .extracting(post -> post.getSource().getName())
                .containsExactlyInAnyOrder("소스A", "소스B");
    }
}
