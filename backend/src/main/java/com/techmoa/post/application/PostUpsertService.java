package com.techmoa.post.application;

import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.post.domain.Post;
import com.techmoa.post.domain.PostRepository;
import com.techmoa.source.domain.Source;
import com.techmoa.tag.domain.Tag;
import com.techmoa.tag.domain.TagRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostUpsertService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;

    public PostUpsertService(PostRepository postRepository, TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public int upsert(Source source, List<ParsedPost> parsedPosts) {
        int savedCount = 0;
        for (ParsedPost parsedPost : parsedPosts) {
            if (parsedPost.canonicalUrl() == null || parsedPost.canonicalUrl().isBlank()) {
                continue;
            }
            if (parsedPost.title() == null || parsedPost.title().isBlank()) {
                continue;
            }

            String canonicalUrl = parsedPost.canonicalUrl().trim();
            LocalDateTime publishedAt = parsedPost.publishedAt() == null
                    ? LocalDateTime.now()
                    : parsedPost.publishedAt();
            Set<Tag> tags = resolveTags(parsedPost.tags());

            Post target = postRepository.findByCanonicalUrl(canonicalUrl)
                    .map(existing -> updateExisting(existing, parsedPost, publishedAt))
                    .orElseGet(() -> createNew(source, canonicalUrl, parsedPost, publishedAt));
            target.replaceTags(tags);

            postRepository.save(target);
            savedCount++;
        }
        return savedCount;
    }

    private Post createNew(
            Source source,
            String canonicalUrl,
            ParsedPost parsedPost,
            LocalDateTime publishedAt
    ) {
        return new Post(
                source,
                canonicalUrl,
                parsedPost.title(),
                parsedPost.summary(),
                parsedPost.author(),
                parsedPost.thumbnailUrl(),
                publishedAt
        );
    }

    private Post updateExisting(Post post, ParsedPost parsedPost, LocalDateTime publishedAt) {
        String thumbnailUrl = parsedPost.thumbnailUrl() == null || parsedPost.thumbnailUrl().isBlank()
                ? post.getThumbnailUrl()
                : parsedPost.thumbnailUrl().trim();
        post.updateFrom(
                parsedPost.title(),
                parsedPost.summary(),
                parsedPost.author(),
                thumbnailUrl,
                publishedAt
        );
        return post;
    }

    private Set<Tag> resolveTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalizedNames = rawTags.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        if (normalizedNames.isEmpty()) {
            return Set.of();
        }

        List<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Map<String, Tag> byName = new LinkedHashMap<>();
        for (Tag tag : existingTags) {
            byName.put(tag.getName(), tag);
        }

        List<Tag> result = new ArrayList<>();
        for (String name : normalizedNames) {
            Tag tag = byName.get(name);
            if (tag == null) {
                tag = tagRepository.save(new Tag(name));
                byName.put(name, tag);
            }
            result.add(tag);
        }

        return new LinkedHashSet<>(result);
    }
}
