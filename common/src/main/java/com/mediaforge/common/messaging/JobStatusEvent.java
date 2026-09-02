package com.mediaforge.common.messaging;

import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.domain.enums.JobType;

import java.util.UUID;

public record JobStatusEvent(
        String eventType,
        UUID jobId,
        UUID uploadId,
        JobType type,
        JobStatus status
) {
}