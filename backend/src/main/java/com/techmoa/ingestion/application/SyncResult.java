package com.techmoa.ingestion.application;

public record SyncResult(
        String sourceName,
        int parsedCount,
        int savedCount
) {
}
