package com.techmoa.source.presentation.dto;

import com.techmoa.source.domain.Source;

public record SourceResponse(
        Long id,
        String name,
        String baseUrl,
        String feedUrl,
        String parserType,
        Boolean active
) {
    public static SourceResponse from(Source source) {
        return new SourceResponse(
                source.getId(),
                source.getName(),
                source.getBaseUrl(),
                source.getFeedUrl(),
                source.getParserType().name(),
                source.getActive()
        );
    }
}
