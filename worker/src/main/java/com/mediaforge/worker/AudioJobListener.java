package com.mediaforge.worker;

import com.mediaforge.common.messaging.JobMessage;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${worker.type:all}' == 'all' or '${worker.type:all}' == 'audio'")
public class AudioJobListener extends AbstractJobListener {

    private final MetadataProcessor metadataProcessor;
    private final WaveformProcessor waveformProcessor;
    private final AudioTranscodeProcessor audioTranscodeProcessor;

    public AudioJobListener(JobRepository jobRepository,
                            JobStatusPublisher jobStatusPublisher,
                            RetryPublisher retryPublisher,
                            MeterRegistry meterRegistry,
                            UploadRepository uploadRepository,
                            UploadStatusPublisher uploadStatusPublisher,
                            MetadataProcessor metadataProcessor,
                            WaveformProcessor waveformProcessor,
                            AudioTranscodeProcessor audioTranscodeProcessor) {
        super(jobRepository, jobStatusPublisher, retryPublisher, meterRegistry, uploadRepository,uploadStatusPublisher);
        this.metadataProcessor = metadataProcessor;
        this.waveformProcessor = waveformProcessor;
        this.audioTranscodeProcessor = audioTranscodeProcessor;
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.metadata-queue}")
    public void handleMetadata(JobMessage message) {
        runJob(message.jobId(), job ->
                metadataProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.waveform-queue}")
    public void handleWaveform(JobMessage message) {
        runJob(message.jobId(), job ->
                waveformProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.audio-transcode-queue}")
    public void handleAudioTranscode(JobMessage message) {
        runJob(message.jobId(), job ->
                audioTranscodeProcessor.process(job.getId(), job.getUploadId()));
    }
}
