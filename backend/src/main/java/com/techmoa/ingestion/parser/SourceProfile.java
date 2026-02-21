package com.techmoa.ingestion.parser;

public record SourceProfile(
        Long sourceId,
        String sourceName,
        String baseUrl,
        String feedUrl,
        ParserType parserType
) {
}
