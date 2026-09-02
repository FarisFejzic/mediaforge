package com.mediaforge.common.messaging;

import com.mediaforge.common.domain.enums.UploadStatus;

import java.util.UUID;

public record UploadStatusEvent(
        String eventType,   // "UPLOAD"
        UUID uploadId,
        UploadStatus status
) {
}