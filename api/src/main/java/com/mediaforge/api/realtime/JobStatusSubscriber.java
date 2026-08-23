package com.mediaforge.api.realtime;

import com.mediaforge.common.messaging.JobStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JobStatusSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(JobStatusSubscriber.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpMessagingTemplate messagingTemplate;

    public JobStatusSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            JobStatusEvent event = objectMapper.readValue(json, JobStatusEvent.class);

            String destination = "/topic/uploads/" + event.uploadId();
            messagingTemplate.convertAndSend(destination, event);

            log.info("Forwarded job status to {}: status={}", destination, event.status());
        } catch (Exception e) {
            log.error("Failed to handle job status event", e);
        }
    }
}