package com.techmoa.ingestion.application;

import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import com.techmoa.ingestion.parser.TechBlogParser;
import com.techmoa.ingestion.domain.SyncJob;
import com.techmoa.ingestion.domain.SyncJobRepository;
import com.techmoa.post.application.PostUpsertService;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SourceSyncService {

    private static final Logger log = LoggerFactory.getLogger(SourceSyncService.class);

    private final SourceRepository sourceRepository;
    private final SyncJobRepository syncJobRepository;
    private final PostUpsertService postUpsertService;
    private final List<TechBlogParser> parsers;

    public SourceSyncService(
            SourceRepository sourceRepository,
            SyncJobRepository syncJobRepository,
            PostUpsertService postUpsertService,
            List<TechBlogParser> parsers
    ) {
        this.sourceRepository = sourceRepository;
        this.syncJobRepository = syncJobRepository;
        this.postUpsertService = postUpsertService;
        this.parsers = parsers;
    }

    public void syncActiveSources() {
        List<Source> activeSources = sourceRepository.findByActiveTrue();
        for (Source source : activeSources) {
            try {
                syncSource(source);
            } catch (Exception e) {
                log.error(
                        "Source sync failed. sourceName={}, parserType={}",
                        source.getName(),
                        source.getParserType(),
                        e
                );
            }
        }
    }

    public SyncResult syncSourceById(Long sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new NoSuchElementException("Source not found: " + sourceId));
        return syncSource(source);
    }

    public SyncResult syncSource(Source source) {
        SyncJob syncJob = syncJobRepository.save(SyncJob.start(source));
        TechBlogParser parser = resolveParser(source.getParserType());
        SourceProfile profile = new SourceProfile(
                source.getId(),
                source.getName(),
                source.getBaseUrl(),
                source.getFeedUrl(),
                source.getParserType()
        );

        try {
            List<ParsedPost> parsedPosts = parser.fetch(profile);
            int savedCount = postUpsertService.upsert(source, parsedPosts);
            syncJob.complete(savedCount);
            syncJobRepository.save(syncJob);

            log.info(
                    "Source sync completed. sourceName={}, parserType={}, parsedCount={}, savedCount={}",
                    source.getName(),
                    source.getParserType(),
                    parsedPosts.size(),
                    savedCount
            );
            return new SyncResult(source.getName(), parsedPosts.size(), savedCount);
        } catch (Exception e) {
            syncJob.fail(e.getMessage());
            syncJobRepository.save(syncJob);
            throw e;
        }
    }

    private TechBlogParser resolveParser(ParserType parserType) {
        return parsers.stream()
                .filter(parser -> parser.supports(parserType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No parser for parserType=" + parserType));
    }
}
