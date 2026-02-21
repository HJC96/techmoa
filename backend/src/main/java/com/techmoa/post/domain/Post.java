package com.techmoa.post.domain;

import com.techmoa.source.domain.Source;
import com.techmoa.tag.domain.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false)
    private String canonicalUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column
    private String summary;

    @Column
    private String author;

    @Column
    private String thumbnailUrl;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    protected Post() {
    }

    public Post(
            Source source,
            String canonicalUrl,
            String title,
            String summary,
            String author,
            String thumbnailUrl,
            LocalDateTime publishedAt
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.source = source;
        this.canonicalUrl = canonicalUrl;
        this.title = title;
        this.summary = summary;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
        this.fetchedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getAuthor() {
        return author;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void updateFrom(
            String title,
            String summary,
            String author,
            String thumbnailUrl,
            LocalDateTime publishedAt
    ) {
        this.title = title;
        this.summary = summary;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
        this.fetchedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void replaceTags(Set<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
        this.updatedAt = LocalDateTime.now();
    }
}
