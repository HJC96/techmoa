package com.techmoa.post.presentation.dto;

import java.util.List;

public record PostFeedResponse(
        List<PostItemResponse> items,
        Long nextCursor,
        boolean hasNext
) {
}
