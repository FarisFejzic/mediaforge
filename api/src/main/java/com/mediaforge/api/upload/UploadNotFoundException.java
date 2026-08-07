package com.mediaforge.api.upload;

import java.util.UUID;

public class UploadNotFoundException extends RuntimeException {
    public UploadNotFoundException(UUID id) {
        super("Upload not found: " + id);
    }
}