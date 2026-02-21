package com.techmoa.post.presentation.dto;

import com.techmoa.common.url.UrlResolver;
import com.techmoa.post.domain.Post;
import java.time.format.DateTimeFormatter;

public record PostItemResponse(
        Long id,
        String title,
        String summary,
        String thumbnailUrl,
        String sourceName,
        String canonicalUrl,
        String publishedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static PostItemResponse from(Post post) {
        return new PostItemResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                UrlResolver.resolveAbsoluteUrl(
                        post.getThumbnailUrl(),
                        post.getCanonicalUrl(),
                        post.getSource().getBaseUrl()
                ),
                post.getSource().getName(),
                post.getCanonicalUrl(),
                post.getPublishedAt().format(FORMATTER)
        );
    }
}
