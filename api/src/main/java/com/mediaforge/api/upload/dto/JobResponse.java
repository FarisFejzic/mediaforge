package com.mediaforge.api.upload.dto;

import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.domain.enums.JobType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID uploadId,
        JobType type,
        JobStatus status,
        int attempts,
        String error,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
