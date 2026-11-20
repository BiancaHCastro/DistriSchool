package com.unifor.br.distrischool.turma_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Component
public class Notifications {
    private static final String QUEUE = "notification.sent";
    private static final Logger LOG = LoggerFactory.getLogger(Notifications.class);
    private final RabbitTemplate rabbit;
    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http = new RestTemplate(requestFactory());
    private static SimpleClientHttpRequestFactory requestFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(400));
        factory.setReadTimeout(Duration.ofMillis(400));
        return factory;
    }
    private final String authUrl;
    private final String internalKey;
    public Notifications(RabbitTemplate rabbit, @Value("${AUTH_SERVICE_URL:http://auth-service:8081}") String authUrl,
                         @Value("${NOTIFICATION_LOOKUP_KEY:}") String internalKey) {
        this.rabbit = rabbit;
        this.authUrl = authUrl;
        this.internalKey = internalKey;
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    record Event(String event, String type, String email, Long academicId,
                 String subject, String message, String occurredAt) {}

    public void sendAcademic(String type, Long academicId, String subject, String message) {
        String email = null;
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Notification-Key", internalKey);
            email = http.exchange(authUrl + "/internal/notifications/student/{id}",
                    org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers),
                    String.class, academicId).getBody();
        } catch (Exception failure) {
            LOG.warn("Destinatario indisponivel para {} (aluno {})", type, academicId);
        }
        send(type, email, academicId, subject, message);
    }

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
