package com.techmoa.post.presentation;

import com.techmoa.post.application.PostQueryService;
import com.techmoa.post.presentation.dto.PostDetailResponse;
import com.techmoa.post.presentation.dto.PostFeedResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostQueryService postQueryService;

    public PostController(PostQueryService postQueryService) {
        this.postQueryService = postQueryService;
    }

    @GetMapping
    public PostFeedResponse getPosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, name = "sourceId") List<Long> sourceIds,
            @RequestParam(required = false, name = "tag") String tagName,
            @RequestParam(required = false) String q
    ) {
        return postQueryService.getFeed(cursor, size, sourceIds, tagName, q);
    }

    @GetMapping("/{id}")
    public PostDetailResponse getPostDetail(@PathVariable("id") Long postId) {
        return postQueryService.getPostDetail(postId);
    }
}
