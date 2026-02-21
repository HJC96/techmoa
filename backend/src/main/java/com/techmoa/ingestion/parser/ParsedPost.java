package com.techmoa.ingestion.parser;

import java.time.LocalDateTime;
import java.util.List;

public record ParsedPost(
        String title,
        String canonicalUrl,
        String summary,
        String author,
        LocalDateTime publishedAt,
        List<String> tags
) {
}
