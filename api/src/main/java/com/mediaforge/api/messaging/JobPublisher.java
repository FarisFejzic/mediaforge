package com.mediaforge.api.messaging;

import com.mediaforge.common.messaging.JobMessage;
import com.mediaforge.common.messaging.RabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties properties;

    public JobPublisher(RabbitTemplate rabbitTemplate, RabbitProperties properties){
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publishThumbnailJob(UUID jobId){
        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.thumbnailRoutingKey(),
                new JobMessage(jobId));
    }

    public void publishPosterJob(UUID jobId) {
        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.posterRoutingKey(),
                new JobMessage(jobId)
        );
    }

    public void publishMetadataJob(UUID jobId) {
        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.metadataRoutingKey(),
                new JobMessage(jobId)
        );
    }

    public void publishTranscodeJob(UUID jobId) {
        rabbitTemplate.convertAndSend(properties.exchange(),
                properties.transcodeRoutingKey(), new JobMessage(jobId));
    }

    public void publishPreviewJob(UUID jobId) {
        rabbitTemplate.convertAndSend(properties.exchange(),
                properties.previewRoutingKey(), new JobMessage(jobId));
    }

    public void publishWaveformJob(UUID jobId) {
        rabbitTemplate.convertAndSend(properties.exchange(),
                properties.waveformRoutingKey(), new JobMessage(jobId));
    }

    public void publishAudioTranscodeJob(UUID jobId) {
        rabbitTemplate.convertAndSend(properties.exchange(),
                properties.audioTranscodeRoutingKey(), new JobMessage(jobId));
    }
}
