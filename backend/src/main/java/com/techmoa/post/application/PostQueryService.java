package com.techmoa.post.application;

import com.techmoa.post.domain.Post;
import com.techmoa.post.domain.PostRepository;
import com.techmoa.post.presentation.dto.PostDetailResponse;
import com.techmoa.post.presentation.dto.PostFeedResponse;
import com.techmoa.post.presentation.dto.PostItemResponse;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final PostRepository postRepository;

    public PostQueryService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public PostFeedResponse getFeed(
            Long cursor,
            Integer size,
            String sourceName,
            String tagName,
            String q
    ) {
        int normalizedSize = normalizeSize(size);
        String normalizedSource = normalizeText(sourceName);
        String normalizedTagName = normalizeText(tagName);
        String normalizedKeyword = normalizeText(q);

        List<Post> loaded = postRepository.findFeed(
                cursor,
                normalizedSource,
                normalizedTagName,
                normalizedKeyword,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = loaded.size() > normalizedSize;
        List<Post> page = hasNext ? loaded.subList(0, normalizedSize) : loaded;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;

        return new PostFeedResponse(
                page.stream().map(PostItemResponse::from).toList(),
                nextCursor,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + postId));
        return PostDetailResponse.from(post);
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
