package com.techmoa.tag.presentation.dto;

import com.techmoa.tag.domain.Tag;

public record TagResponse(
        Long id,
        String name
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
