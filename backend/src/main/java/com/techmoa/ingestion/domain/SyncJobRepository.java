package com.techmoa.ingestion.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

    @Query("select max(job.startedAt) from SyncJob job where job.source.id = :sourceId")
    Optional<LocalDateTime> findLatestStartedAtBySourceId(@Param("sourceId") Long sourceId);
}
