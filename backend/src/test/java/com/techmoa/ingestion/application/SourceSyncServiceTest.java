package com.techmoa.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techmoa.ingestion.domain.SyncJob;
import com.techmoa.ingestion.domain.SyncJobRepository;
import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import com.techmoa.ingestion.parser.TechBlogParser;
import com.techmoa.ingestion.parser.sitemap.SitemapTechBlogParser;
import com.techmoa.post.application.PostUpsertService;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SourceSyncServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private SyncJobRepository syncJobRepository;

    @Mock
    private PostUpsertService postUpsertService;

    @Mock
    private TechBlogParser parser;

    @Mock
    private SitemapTechBlogParser sitemapTechBlogParser;

    private SourceSyncService sourceSyncService;

    @BeforeEach
    void setUp() {
        sourceSyncService = new SourceSyncService(
                sourceRepository,
                syncJobRepository,
                postUpsertService,
                List.of(parser),
                sitemapTechBlogParser
        );
    }

    @Test
    void syncActiveSources_skipsWhenIntervalNotElapsed() {
        Source source = new Source(
                "카카오테크",
                "https://tech.kakao.com",
                "https://tech.kakao.com/feed.xml",
                ParserType.RSS,
                30,
                true
        );
        ReflectionTestUtils.setField(source, "id", 1L);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of(source));
        when(syncJobRepository.findLatestStartedAtBySourceId(source.getId()))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(5)));

        sourceSyncService.syncActiveSources();

        verify(parser, never()).fetch(any(SourceProfile.class));
        verify(postUpsertService, never()).upsert(any(Source.class), anyList());
        verify(syncJobRepository, never()).save(any(SyncJob.class));
    }

    @Test
    void syncActiveSources_runsWhenIntervalElapsed() {
        Source source = new Source(
                "카카오테크",
                "https://tech.kakao.com",
                "https://tech.kakao.com/feed.xml",
                ParserType.RSS,
                10,
                true
        );
        ReflectionTestUtils.setField(source, "id", 2L);
        when(sourceRepository.findByActiveTrue()).thenReturn(List.of(source));
        when(syncJobRepository.findLatestStartedAtBySourceId(source.getId()))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(15)));
        when(parser.supports(ParserType.RSS)).thenReturn(true);
        when(parser.fetch(any(SourceProfile.class))).thenReturn(List.of(
                new ParsedPost(
                        "title",
                        "https://example.com/post",
                        "summary",
                        "author",
                        null,
                        LocalDateTime.of(2026, 2, 21, 10, 0),
                        List.of()
                )
        ));
        when(postUpsertService.upsert(any(Source.class), anyList())).thenReturn(1);
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sourceSyncService.syncActiveSources();

        verify(parser).fetch(any(SourceProfile.class));
        verify(postUpsertService).upsert(any(Source.class), anyList());
        verify(syncJobRepository, times(2)).save(any(SyncJob.class));
    }

    @Test
    void backfillSourceById_usesSitemapParserAndOverridesSitemapUrl() {
        Source source = new Source(
                "카카오테크",
                "https://tech.kakao.com",
                "https://tech.kakao.com/feed.xml",
                ParserType.RSS,
                30,
                true
        );
        ReflectionTestUtils.setField(source, "id", 3L);

        when(sourceRepository.findById(3L)).thenReturn(Optional.of(source));
        when(parser.supports(ParserType.SITEMAP)).thenReturn(true);
        when(parser.fetch(any(SourceProfile.class))).thenReturn(List.of(
                new ParsedPost(
                        "historic post",
                        "https://tech.kakao.com/posts/1",
                        "summary",
                        "author",
                        null,
                        LocalDateTime.of(2025, 1, 1, 12, 0),
                        List.of("Java")
                )
        ));
        when(postUpsertService.upsert(any(Source.class), anyList())).thenReturn(1);
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncResult result = sourceSyncService.backfillSourceById(3L, "https://tech.kakao.com/sitemap.xml");

        assertThat(result.parsedCount()).isEqualTo(1);
        assertThat(result.savedCount()).isEqualTo(1);
        verify(parser).fetch(argThat(profile ->
                profile.parserType() == ParserType.SITEMAP
                        && "https://tech.kakao.com/sitemap.xml".equals(profile.feedUrl())
        ));
        verify(postUpsertService).upsert(any(Source.class), anyList());
        verify(syncJobRepository, times(2)).save(any(SyncJob.class));
    }
}
