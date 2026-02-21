package com.techmoa.ingestion.domain;

import com.techmoa.source.domain.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_jobs")
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SyncJobStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Integer successCount;

    @Column(nullable = false)
    private Integer failureCount;

    @Column
    private String errorMessage;

    protected SyncJob() {
    }

    private SyncJob(Source source) {
        this.source = source;
        this.status = SyncJobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.successCount = 0;
        this.failureCount = 0;
    }

    public static SyncJob start(Source source) {
        return new SyncJob(source);
    }

    public void complete(int successCount) {
        this.status = SyncJobStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
        this.successCount = successCount;
        this.failureCount = 0;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = SyncJobStatus.FAILED;
        this.endedAt = LocalDateTime.now();
        this.failureCount = 1;
        this.errorMessage = truncate(errorMessage, 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
