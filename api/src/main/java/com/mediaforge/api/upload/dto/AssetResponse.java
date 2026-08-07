package com.mediaforge.api.upload.dto;

import com.mediaforge.common.domain.enums.AssetKind;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        UUID jobId,
        AssetKind kind,
        long sizeBytes,
        OffsetDateTime createdAt
) {
}
