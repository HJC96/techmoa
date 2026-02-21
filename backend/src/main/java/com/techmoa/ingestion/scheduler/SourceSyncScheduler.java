package com.techmoa.ingestion.scheduler;

import com.techmoa.ingestion.application.SourceSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SourceSyncScheduler {

    private final SourceSyncService sourceSyncService;

    public SourceSyncScheduler(SourceSyncService sourceSyncService) {
        this.sourceSyncService = sourceSyncService;
    }

    @Scheduled(fixedDelayString = "${techmoa.sync.fixed-delay-ms:600000}")
    public void syncActiveSources() {
        sourceSyncService.syncActiveSources();
    }
}
