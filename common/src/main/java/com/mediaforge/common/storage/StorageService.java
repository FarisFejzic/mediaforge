package com.mediaforge.common.storage;

import java.io.InputStream;

public interface StorageService {

    void store(String key, InputStream data, long size, String contentType);

    InputStream retrieve(String key);

    void delete(String key);

    String presignedGetUrl(String key, int expirySeconds, boolean attachment);
}