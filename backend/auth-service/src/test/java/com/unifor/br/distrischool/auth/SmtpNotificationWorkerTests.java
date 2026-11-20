package com.unifor.br.distrischool.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpNotificationWorkerTests {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final SmtpNotificationWorker worker = new SmtpNotificationWorker(sender, "contas@example.org");

    @ParameterizedTest
    @CsvSource({"VERIFICACAO_EMAIL, verify-email, 24 horas", "RECUPERACAO_SENHA, password-reset, 30 minutos"})
    void enviaLinkDeContaComPrazo(String type, String path, String expiration) throws Exception {
        String link = "https://school.example.org/" + path + "?token=example";
        worker.consume(event(type, link));

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        var mail = captor.getValue();
        assertArrayEquals(new String[]{"aluno@example.org"}, mail.getTo());
        assertEquals("contas@example.org", mail.getFrom());
        assertEquals("DistriSchool – Mensagem de conta", mail.getSubject());
        assertTrue(mail.getText().contains(link));
        assertTrue(mail.getText().contains(expiration));
    }

    @Test
    void ignoraEventosAcademicosEInvalidos() throws Exception {
        worker.consume(event("MATRICULA_APROVADA", "Aviso acadêmico"));
        worker.consume(new Message(new byte[0]));
        verifyNoInteractions(sender);
    }

    @Test
    void falhaSmtpPodeSerRepetidaSemExporTokenNaExcecao() throws Exception {
        doThrow(new MailSendException("token=secret")).when(sender).send(any(SimpleMailMessage.class));
        var message = event("RECUPERACAO_SENHA", "https://school.example.org/password-reset?token=secret");
        var failure = assertThrows(IllegalStateException.class, () -> worker.consume(message));
        assertFalse(failure.getMessage().contains("secret"));
        assertNull(failure.getCause());
    }

    private Message event(String type, String content) throws Exception {
        var event = new Notifications.Event("notification.sent", type, "aluno@example.org", null,
                "Mensagem de conta", content, "2026-01-01T00:00:00Z");
        return new Message(new ObjectMapper().writeValueAsBytes(event));
    }
}
