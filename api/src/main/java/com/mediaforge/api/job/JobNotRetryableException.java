package com.mediaforge.api.job;

import com.mediaforge.common.domain.enums.JobStatus;

import java.util.UUID;

public class JobNotRetryableException extends RuntimeException {
    public JobNotRetryableException(UUID id, JobStatus status) {
        super("Job " + id + " is not retryable (status: " + status + ")");
    }
}