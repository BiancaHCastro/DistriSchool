package com.unifor.br.distrischool.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("smtp-notifications")
public class SmtpNotificationWorker {
    private static final Logger LOG = LoggerFactory.getLogger(SmtpNotificationWorker.class);
    private final ObjectMapper json = new ObjectMapper();
    private final JavaMailSender sender;
    private final String from;

    public SmtpNotificationWorker(JavaMailSender sender, @Value("${app.mail.from}") String from) {
        if (from.isBlank()) throw new IllegalArgumentException("Configure SMTP_FROM para enviar e-mails.");
        this.sender = sender;
        this.from = from;
    }

    @RabbitListener(queues = "notification.sent")
    public void consume(Message message) {
        Notifications.Event event;
        try {
            event = json.readValue(message.getBody(), Notifications.Event.class);
        } catch (IOException failure) {
            LOG.warn("Evento de notificação inválido.");
            return;
        }
        if (event == null || event.type() == null) {
            LOG.warn("Evento de notificação sem tipo.");
            return;
        }

        String instructions = switch (event.type()) {
            case "VERIFICACAO_EMAIL" -> "Confirme seu endereço para ativar a conta. O link é válido por 24 horas.";
            case "RECUPERACAO_SENHA" -> "Abra o link para definir uma nova senha. Ele é válido por 30 minutos.";
            default -> null;
        };
        if (instructions == null) return;
        if (event.email() == null || event.email().isBlank() || event.message() == null || event.message().isBlank()) {
            LOG.warn("Evento de conta sem destinatário ou link.");
            return;
        }

        var email = new SimpleMailMessage();
        email.setFrom(from);
        email.setTo(event.email());
        email.setSubject("DistriSchool – " + event.subject());
        email.setText("Olá!\n\n" + instructions + "\n\n" + event.message()
                + "\n\nSe você não fez essa solicitação, pode ignorar este e-mail.");
        try {
            sender.send(email);
        } catch (MailException failure) {
            // MailSendException can contain the full failed message, including the token.
            LOG.warn("Falha no envio SMTP do evento {} ({})", event.type(), failure.getClass().getSimpleName());
            throw new IllegalStateException("Falha no envio SMTP de mensagem de conta.");
        }
    }

    @Bean
    MessageRecoverer smtpMessageRecoverer() {
        return (message, failure) -> LOG.error("Tentativas SMTP esgotadas. O usuário pode solicitar outro link.");
    }
}
