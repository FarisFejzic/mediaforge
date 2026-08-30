package com.mediaforge.worker;

import com.mediaforge.common.messaging.JobMessage;
import com.mediaforge.common.repository.JobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${worker.type:all}' == 'all' or '${worker.type:all}' == 'video'")
public class VideoJobListener extends AbstractJobListener {

    private final VideoTranscodeProcessor videoTranscodeProcessor;
    private final PosterProcessor posterProcessor;
    private final PreviewProcessor previewProcessor;

    public VideoJobListener(JobRepository jobRepository,
                            JobStatusPublisher jobStatusPublisher,
                            RetryPublisher retryPublisher,
                            MeterRegistry meterRegistry,
                            VideoTranscodeProcessor videoTranscodeProcessor,
                            PosterProcessor posterProcessor,
                            PreviewProcessor previewProcessor) {
        super(jobRepository, jobStatusPublisher, retryPublisher, meterRegistry);
        this.videoTranscodeProcessor = videoTranscodeProcessor;
        this.posterProcessor = posterProcessor;
        this.previewProcessor = previewProcessor;
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.transcode-queue}")
    public void handleTranscode(JobMessage message) {
        runJob(message.jobId(), job ->
                videoTranscodeProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.poster-queue}")
    public void handlePoster(JobMessage message) {
        runJob(message.jobId(), job ->
                posterProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.preview-queue}")
    public void handlePreview(JobMessage message) {
        runJob(message.jobId(), job ->
                previewProcessor.process(job.getId(), job.getUploadId()));
    }
}
