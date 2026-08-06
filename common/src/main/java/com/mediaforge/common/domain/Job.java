package com.mediaforge.common.domain;

import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.domain.enums.JobType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.QUEUED;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private  Integer maxAttempts = 3;

    @Column(columnDefinition = "text")
    private  String error;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "started_at")
    private  OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private  OffsetDateTime finishedAt;

    protected Job(){
    }

    public static Job create(UUID uploadId, JobType type){
        Job job = new Job();
        job.id = UUID.randomUUID();
        job.uploadId = uploadId;
        job.type = type;
        job.status = JobStatus.QUEUED;
        job.attempts = 0;
        job.maxAttempts = 3;
        return job;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUploadId() {
        return uploadId;
    }

    public void setUploadId(UUID uploadId) {
        this.uploadId = uploadId;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(OffsetDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
