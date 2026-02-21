package com.techmoa.ingestion.application;

import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import com.techmoa.ingestion.parser.TechBlogParser;
import com.techmoa.ingestion.domain.SyncJob;
import com.techmoa.ingestion.domain.SyncJobRepository;
import com.techmoa.ingestion.parser.sitemap.SitemapTechBlogParser;
import com.techmoa.post.application.PostUpsertService;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
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
    private final SitemapTechBlogParser sitemapTechBlogParser;

    public SourceSyncService(
            SourceRepository sourceRepository,
            SyncJobRepository syncJobRepository,
            PostUpsertService postUpsertService,
            List<TechBlogParser> parsers,
            SitemapTechBlogParser sitemapTechBlogParser
    ) {
        this.sourceRepository = sourceRepository;
        this.syncJobRepository = syncJobRepository;
        this.postUpsertService = postUpsertService;
        this.parsers = parsers;
        this.sitemapTechBlogParser = sitemapTechBlogParser;
    }

    public void syncActiveSources() {
        List<Source> activeSources = sourceRepository.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now();
        for (Source source : activeSources) {
            if (!isSyncDue(source, now)) {
                log.debug(
                        "Source sync skipped by interval. sourceName={}, intervalMin={}",
                        source.getName(),
                        source.getIntervalMin()
                );
                continue;
            }
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

    public SyncResult backfillSourceById(Long sourceId, String sitemapUrlOverride) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new NoSuchElementException("Source not found: " + sourceId));
        return backfillSource(source, sitemapUrlOverride);
    }

    private boolean isSyncDue(Source source, LocalDateTime now) {
        Integer intervalMin = source.getIntervalMin();
        if (intervalMin == null || intervalMin <= 0) {
            return true;
        }
        return syncJobRepository.findLatestStartedAtBySourceId(source.getId())
                .map(lastStartedAt -> !lastStartedAt.plusMinutes(intervalMin).isAfter(now))
                .orElse(true);
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

    public SyncResult backfillSource(Source source, String sitemapUrlOverride) {
        SyncJob syncJob = syncJobRepository.save(SyncJob.start(source));
        TechBlogParser parser = resolveParser(ParserType.SITEMAP);
        String sitemapUrl = resolveSitemapUrl(source, sitemapUrlOverride);
        SourceProfile profile = new SourceProfile(
                source.getId(),
                source.getName(),
                source.getBaseUrl(),
                sitemapUrl,
                ParserType.SITEMAP
        );

        try {
            List<ParsedPost> parsedPosts = parser.fetch(profile);
            int savedCount = postUpsertService.upsert(source, parsedPosts);
            syncJob.complete(savedCount);
            syncJobRepository.save(syncJob);

            log.info(
                    "Source backfill completed. sourceName={}, sitemapUrl={}, parsedCount={}, savedCount={}",
                    source.getName(),
                    sitemapUrl,
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

    private String resolveSitemapUrl(Source source, String sitemapUrlOverride) {
        if (sitemapUrlOverride != null && !sitemapUrlOverride.isBlank()) {
            return sitemapUrlOverride.trim();
        }

        if (source.getParserType() == ParserType.SITEMAP
                && source.getFeedUrl() != null
                && !source.getFeedUrl().isBlank()) {
            return source.getFeedUrl().trim();
        }

        if (source.getFeedUrl() != null
                && !source.getFeedUrl().isBlank()
                && source.getFeedUrl().toLowerCase(Locale.ROOT).contains("sitemap")) {
            return source.getFeedUrl().trim();
        }

        Optional<String> discoveredSitemap = sitemapTechBlogParser.discoverSitemapUrlFromRobots(source.getBaseUrl());
        if (discoveredSitemap.isPresent()) {
            return discoveredSitemap.get();
        }
        return source.getBaseUrl().replaceAll("/+$", "") + "/sitemap.xml";
    }

    private TechBlogParser resolveParser(ParserType parserType) {
        return parsers.stream()
                .filter(parser -> parser.supports(parserType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No parser for parserType=" + parserType));
    }
}
