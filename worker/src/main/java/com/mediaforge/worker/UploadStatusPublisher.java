package com.mediaforge.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.messaging.RedisProperties;
import com.mediaforge.common.messaging.UploadStatusEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UploadStatusPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UploadStatusPublisher(RedisTemplate<String, Object> redisTemplate,
                                 RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void publish(Upload upload) {
        try {
            UploadStatusEvent event = new UploadStatusEvent(
                    "UPLOAD", upload.getId(), upload.getStatus());
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(properties.jobStatusChannel(), json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish upload status event", e);
        }
    }
}