package com.techmoa.admin.presentation;

import com.techmoa.admin.presentation.dto.CreateSourceRequest;
import com.techmoa.admin.presentation.dto.ManualSyncResponse;
import com.techmoa.ingestion.application.SourceSyncService;
import com.techmoa.ingestion.application.SyncResult;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.source.application.SourceService;
import com.techmoa.source.presentation.dto.SourceResponse;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/sources")
public class AdminSourceController {

    private final SourceService sourceService;
    private final SourceSyncService sourceSyncService;

    public AdminSourceController(SourceService sourceService, SourceSyncService sourceSyncService) {
        this.sourceService = sourceService;
        this.sourceSyncService = sourceSyncService;
    }

    @PostMapping
    public SourceResponse createSource(@Valid @RequestBody CreateSourceRequest request) {
        return SourceResponse.from(
                sourceService.createSource(
                        request.name(),
                        request.baseUrl(),
                        request.feedUrl(),
                        parseParserType(request.parserType()),
                        request.intervalMin(),
                        request.active()
                )
        );
    }

    @PostMapping("/{id}/sync")
    public ManualSyncResponse syncSource(@PathVariable("id") Long sourceId) {
        try {
            SyncResult result = sourceSyncService.syncSourceById(sourceId);
            return new ManualSyncResponse(
                    sourceId,
                    result.sourceName(),
                    result.parsedCount(),
                    result.savedCount(),
                    "COMPLETED"
            );
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sync failed");
        }
    }

    private ParserType parseParserType(String parserType) {
        try {
            return ParserType.valueOf(parserType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported parserType: " + parserType
            );
        }
    }
}
