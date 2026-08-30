package com.mediaforge.worker;

import com.mediaforge.common.messaging.JobMessage;
import com.mediaforge.common.repository.JobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${worker.type:all}' == 'all' or '${worker.type:all}' == 'image'")
public class ImageJobListener extends AbstractJobListener {

    private final ThumbnailProcessor thumbnailProcessor;

    public ImageJobListener(JobRepository jobRepository,
                            JobStatusPublisher jobStatusPublisher,
                            RetryPublisher retryPublisher,
                            MeterRegistry meterRegistry,
                            ThumbnailProcessor thumbnailProcessor) {
        super(jobRepository, jobStatusPublisher, retryPublisher, meterRegistry);
        this.thumbnailProcessor = thumbnailProcessor;
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.thumbnail-queue}")
    public void handleThumbnail(JobMessage message) {
        runJob(message.jobId(), job -> {
            try { thumbnailProcessor.process(job.getId(), job.getUploadId()); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
