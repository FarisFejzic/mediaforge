package com.mediaforge.api.upload.dto;

import com.mediaforge.common.domain.enums.MediaType;
import com.mediaforge.common.domain.enums.UploadStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadResponse(
        UUID id,
        String originalName,
        MediaType mediaType,
        long sizeBytes,
        UploadStatus status,
        OffsetDateTime createdAt
) {
}
