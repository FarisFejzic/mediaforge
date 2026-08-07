package com.mediaforge.api.asset;

public record DownloadResponse(
        String url,
        int expiresIn
) {
}