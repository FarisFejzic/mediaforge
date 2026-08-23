package com.mediaforge.common.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mediaforge.rabbitmq")
public record RabbitProperties(
        String exchange,
        String thumbnailQueue,
        String thumbnailRoutingKey,
        String posterQueue,
        String posterRoutingKey,
        String metadataQueue,
        String metadataRoutingKey,
        String transcodeQueue,
        String transcodeRoutingKey,
        String previewQueue,
        String previewRoutingKey,
        String waveformQueue,
        String waveformRoutingKey,
        String audioTranscodeQueue,
        String audioTranscodeRoutingKey,
        String deadLetterExchange,
        String deadLetterQueue,
        String deadLetterRoutingKey,
        String thumbnailWaitQueue,
        String posterWaitQueue,
        String transcodeWaitQueue,
        String previewWaitQueue,
        String metadataWaitQueue,
        String waveformWaitQueue,
        String audioTranscodeWaitQueue,
        long retryDelayMs
) {
}
