package com.techmoa.source.presentation;

import com.techmoa.source.application.SourceService;
import com.techmoa.source.presentation.dto.SourceResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping
    public List<SourceResponse> getSources() {
        return sourceService.findActiveSources().stream()
                .map(SourceResponse::from)
                .toList();
    }
}
