package com.mediaforge.common.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mediaforge.redis")
public record RedisProperties(String jobStatusChannel) {
}