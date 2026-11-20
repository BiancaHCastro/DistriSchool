package com.unifor.br.distrischool.auth;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountLifecycleTests {
    private final UserRepository users = mock(UserRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AccountLifecycle lifecycle = new AccountLifecycle(users, events, mock(Notifications.class), "http://localhost:5173");

    @Test
    void cadastroUsaHashEVerificacaoConsomeToken() {
        lifecycle.register("Aluno", "ALUNO@example.org", "password123");
        var account = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(account.capture());
        var user = account.getValue();
        String token = lastToken(1);
        assertEquals("ROLE_ALUNO", user.role);
        assertFalse(user.emailVerified);
        assertNotEquals(token, user.verificationHash);
        assertTrue(user.verificationExpires.isAfter(Instant.now().plusSeconds(86_000)));
        when(users.findFirstByVerificationHash(user.verificationHash)).thenReturn(Optional.of(user));

        lifecycle.verify(token);
        assertTrue(user.emailVerified);
        assertNull(user.verificationHash);
        assertThrows(ResponseStatusException.class, () -> lifecycle.verify(token));
    }

    @Test
    void recuperacaoSubstituiTokenEConsomeOTokenVigente() {
        var user = new UserAccount();
        user.email = "aluno@example.org";
        when(users.findFirstByEmail(user.email)).thenReturn(Optional.of(user));
        lifecycle.requestReset(user.email);
        String oldToken = lastToken(1);
        String oldHash = user.resetHash;
        lifecycle.requestReset(user.email);
        assertEquals(oldHash, user.resetHash);
        user.resetExpires = user.resetExpires.minusSeconds(61);
        lifecycle.requestReset(user.email);
        String newToken = lastToken(2);
        assertNotEquals(oldHash, user.resetHash);
        assertTrue(user.resetExpires.isAfter(Instant.now().plusSeconds(1_700)));
        when(users.findFirstByResetHash(user.resetHash)).thenReturn(Optional.of(user));
        assertThrows(ResponseStatusException.class, () -> lifecycle.confirmReset(oldToken, "new-password"));
        lifecycle.confirmReset(newToken, "new-password");
        assertTrue(new BCryptPasswordEncoder().matches("new-password", user.passwordHash));
        assertNull(user.resetHash);
        assertThrows(ResponseStatusException.class, () -> lifecycle.confirmReset(newToken, "another-password"));
    }

    @Test
    void reenviaVerificacaoSomenteParaContaPendenteAposIntervalo() {
        var user = new UserAccount();
        user.email = "aluno@example.org";
        user.emailVerified = true;
        when(users.findFirstByEmail(user.email)).thenReturn(Optional.of(user));
        lifecycle.resendVerification("desconhecido@example.org");
        lifecycle.resendVerification(user.email);
        verifyNoInteractions(events);

        user.emailVerified = false;
        lifecycle.resendVerification(user.email);
        String oldHash = user.verificationHash;
        lifecycle.resendVerification(user.email);
        assertEquals(oldHash, user.verificationHash);
        user.verificationExpires = user.verificationExpires.minusSeconds(61);
        lifecycle.resendVerification(user.email);
        assertNotEquals(oldHash, user.verificationHash);
        assertNotNull(lastToken(2));
    }

    @Test
    void tokenExpiradoNaoAlteraSenha() {
        var user = new UserAccount();
        user.email = "aluno@example.org";
        when(users.findFirstByEmail(user.email)).thenReturn(Optional.of(user));
        lifecycle.requestReset(user.email);
        user.resetExpires = Instant.now().minusSeconds(1);
        when(users.findFirstByResetHash(user.resetHash)).thenReturn(Optional.of(user));
        String token = lastToken(1);
        assertThrows(ResponseStatusException.class, () -> lifecycle.confirmReset(token, "new-password"));
        assertNull(user.passwordHash);
    }

    private String lastToken(int times) {
        var mail = ArgumentCaptor.forClass(AccountLifecycle.Mail.class);
        verify(events, times(times)).publishEvent(mail.capture());
        return mail.getValue().message().split("token=", 2)[1];
    }
}
