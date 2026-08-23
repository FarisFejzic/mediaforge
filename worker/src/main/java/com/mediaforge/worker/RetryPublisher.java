package com.mediaforge.worker;

import com.mediaforge.common.domain.enums.JobType;
import com.mediaforge.common.messaging.JobMessage;
import com.mediaforge.common.messaging.RabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RetryPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties properties;

    public RetryPublisher(RabbitTemplate rabbitTemplate, RabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /** Tier 1: republish straight back to the work queue for an immediate retry. */
    public void requeueImmediate(UUID jobId, JobType type) {
        rabbitTemplate.convertAndSend(
                properties.exchange(),
                routingKeyFor(type),
                new JobMessage(jobId));
    }

    /** Tier 2: publish to the wait queue; it returns to the work queue after the TTL. */
    public void requeueDelayed(UUID jobId, JobType type) {
        rabbitTemplate.convertAndSend(waitQueueFor(type), new JobMessage(jobId));
    }

    private String waitQueueFor(JobType type) {
        return switch (type) {
            case THUMBNAIL -> properties.thumbnailWaitQueue();
            case POSTER -> properties.posterWaitQueue();
            case TRANSCODE -> properties.transcodeWaitQueue();
            case PREVIEW -> properties.previewWaitQueue();
            case METADATA -> properties.metadataWaitQueue();
            case WAVEFORM -> properties.waveformWaitQueue();
            case AUDIO_TRANSCODE -> properties.audioTranscodeWaitQueue();
        };
    }
    private String routingKeyFor(JobType type) {
        return switch (type) {
            case THUMBNAIL -> properties.thumbnailRoutingKey();
            case POSTER -> properties.posterRoutingKey();
            case TRANSCODE -> properties.transcodeRoutingKey();
            case PREVIEW -> properties.previewRoutingKey();
            case METADATA -> properties.metadataRoutingKey();
            case WAVEFORM -> properties.waveformRoutingKey();
            case AUDIO_TRANSCODE -> properties.audioTranscodeRoutingKey();
        };
    }
}