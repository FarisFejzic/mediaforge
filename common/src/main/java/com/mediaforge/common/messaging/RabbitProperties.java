package com.mediaforge.common.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mediaforge.rabbitmq")
public record RabbitProperties(
        String exchange,
        String thumbnailQueue,
        String thumbnailRoutingKey
) {
}
