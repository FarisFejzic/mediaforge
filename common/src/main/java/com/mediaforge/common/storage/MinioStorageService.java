package com.mediaforge.common.storage;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.util.StoreException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService implements  StorageService{

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(MinioProperties properties){
        this.client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
        this.bucket = properties.bucket();
    }

    @PostConstruct
    void  ensureBucketExists(){
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists){
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new StoreException("Failed to ensure bucket exists: " + bucket, e);
        }
    }

    @Override
    public void store(String key, InputStream data, long size, String contentType){
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(data,size,-1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to store object: " + key, e);
        }
    }

    @Override
    public InputStream retrieve(String key){
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve object: " + key, e);
        }
    }

    @Override
    public void delete(String key){
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete object: " + key, e);
        }
    }

    @Override
    public String presignedGetUrl(String key, int expirySeconds) {
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Failed to presign object: " + key, e);
        }
    }
}
