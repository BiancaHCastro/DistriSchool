package com.unifor.br.distrischool.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class Notifications {
    private static final String QUEUE = "notification.sent";
    private static final Logger LOG = LoggerFactory.getLogger(Notifications.class);
    private final RabbitTemplate rabbit;
    private final ObjectMapper json = new ObjectMapper();

    public Notifications(RabbitTemplate rabbit) {
        this.rabbit = rabbit;

    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    record Event(String event, String type, String email, Long academicId,
                 String subject, String message, String occurredAt) {}

    public void send(String type, String email, Long academicId, String subject, String message) {
        try {
            var event = new Event(QUEUE, type, email, academicId, subject, message, Instant.now().toString());
            rabbit.send(QUEUE, MessageBuilder.withBody(json.writeValueAsBytes(event))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT).build());
        } catch (Exception failure) {
            LOG.warn("Falha ao publicar notificacao {}", type);
        }
    }
}
