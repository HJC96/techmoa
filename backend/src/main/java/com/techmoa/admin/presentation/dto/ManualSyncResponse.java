package com.techmoa.admin.presentation.dto;

public record ManualSyncResponse(
        Long sourceId,
        String sourceName,
        Integer parsedCount,
        Integer savedCount,
        String status
) {
}
