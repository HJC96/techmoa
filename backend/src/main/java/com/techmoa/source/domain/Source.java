package com.techmoa.source.domain;

import com.techmoa.ingestion.parser.ParserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private String baseUrl;

    private String feedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ParserType parserType;

    @Column(nullable = false)
    private Integer intervalMin;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Source() {
    }

    public Source(
            String name,
            String baseUrl,
            String feedUrl,
            ParserType parserType,
            Integer intervalMin,
            Boolean active
    ) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.feedUrl = feedUrl;
        this.parserType = parserType;
        this.intervalMin = intervalMin;
        this.active = active;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public ParserType getParserType() {
        return parserType;
    }

    public Integer getIntervalMin() {
        return intervalMin;
    }

    public Boolean getActive() {
        return active;
    }
}
