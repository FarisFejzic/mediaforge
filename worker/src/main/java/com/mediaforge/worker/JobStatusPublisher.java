package com.mediaforge.worker;

import tools.jackson.databind.ObjectMapper;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.messaging.JobStatusEvent;
import com.mediaforge.common.messaging.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobStatusPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobStatusPublisher(RedisTemplate<String, Object> redisTemplate,
                              RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void publish(Job job) {
        try {
            JobStatusEvent event = new JobStatusEvent(
                    job.getId(), job.getUploadId(), job.getType(), job.getStatus());
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(properties.jobStatusChannel(), json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish job status event", e);
        }
    }
}