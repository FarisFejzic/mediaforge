package com.mediaforge.worker;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.mediaforge.common.messaging.JobMessage;

@Component
public class JobListener {

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    @RabbitListener(queues = "${mediaforge.rabbitmq.thumbnail-queue}")
    public void handleThumbnailJob(JobMessage message) {
        log.info("Received thumbnail job: {}", message.jobId());
    }
}