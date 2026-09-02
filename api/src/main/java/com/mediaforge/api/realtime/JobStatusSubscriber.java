package com.mediaforge.api.realtime;

import com.mediaforge.common.messaging.JobStatusEvent;
import com.mediaforge.common.messaging.UploadStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

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
            JsonNode node = objectMapper.readTree(json);
            String eventType = node.path("eventType").asText();
            UUID uploadId = UUID.fromString(node.path("uploadId").asText());

            String destination = "/topic/uploads/" + uploadId;

            if ("UPLOAD".equals(eventType)) {
                UploadStatusEvent event = objectMapper.treeToValue(node, UploadStatusEvent.class);
                messagingTemplate.convertAndSend(destination, event);
                log.info("Forwarded upload status to {}: status={}", destination, event.status());
            } else {
                JobStatusEvent event = objectMapper.treeToValue(node, JobStatusEvent.class);
                messagingTemplate.convertAndSend(destination, event);
                log.info("Forwarded job status to {}: status={}", destination, event.status());
            }
        } catch (Exception e) {
            log.error("Failed to handle status event", e);
        }
    }
}