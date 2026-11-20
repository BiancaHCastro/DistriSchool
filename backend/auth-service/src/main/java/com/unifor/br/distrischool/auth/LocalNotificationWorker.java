package com.unifor.br.distrischool.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local-notifications & !smtp-notifications")
public class LocalNotificationWorker {
    private final ObjectMapper json = new ObjectMapper();

    @RabbitListener(queues = "notification.sent")
    public void consume(Message message) throws java.io.IOException {
        var event = json.readTree(message.getBody());
        LoggerFactory.getLogger(getClass()).info("Notificacao local [{}] para {} (cadastro {}) | {} | {}",
                event.path("type").asText(), event.path("email").asText("sem e-mail associado"),
                event.path("academicId").asText("-"),
                event.path("subject").asText(), event.path("message").asText());
    }
}
