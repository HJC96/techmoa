package com.techmoa.post.presentation.dto;

import com.techmoa.post.domain.Post;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String title,
        String summary,
        String thumbnailUrl,
        String sourceName,
        String canonicalUrl,
        String author,
        List<String> tags,
        String publishedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getThumbnailUrl(),
                post.getSource().getName(),
                post.getCanonicalUrl(),
                post.getAuthor(),
                post.getTags().stream().map(tag -> tag.getName()).sorted().toList(),
                post.getPublishedAt().format(FORMATTER)
        );
    }
}
