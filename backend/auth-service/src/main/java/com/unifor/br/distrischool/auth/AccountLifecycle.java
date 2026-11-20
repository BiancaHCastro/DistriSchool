package com.unifor.br.distrischool.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountLifecycle {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final Notifications notifications;
    private final String publicUrl;

    public AccountLifecycle(UserRepository users, ApplicationEventPublisher events, Notifications notifications,
                            @Value("${APP_PUBLIC_URL:http://localhost:5173}") String publicUrl) {
        this.users = users;
        this.events = events;
        this.notifications = notifications;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    record Mail(String type, String email, Long academicId, String subject, String message) {}

    @Transactional
    public void register(String name, String email, String password) {
        var user = new UserAccount();
        user.email = normalize(email);
        user.name = name;
        user.role = "ROLE_ALUNO";
        user.emailVerified = false;
        if (users.findByEmail(user.email).isPresent()) throw duplicate();
        user.passwordHash = encode(password);
        String token = token();
        user.verificationHash = hash(token);
        user.verificationExpires = Instant.now().plus(VERIFICATION_TTL);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException failure) {
            throw duplicate();
        }
        publishVerification(user, token);
    }

    @Transactional
    public void resendVerification(String email) {
        users.findFirstByEmail(normalize(email)).ifPresent(user -> {
            Instant now = Instant.now();
            if (user.emailVerified || issuedRecently(user.verificationExpires, VERIFICATION_TTL, now)) return;
            String token = token();
            user.verificationHash = hash(token);
            user.verificationExpires = now.plus(VERIFICATION_TTL);
            publishVerification(user, token);
        });
    }

    private void publishVerification(UserAccount user, String token) {
        events.publishEvent(new Mail("VERIFICACAO_EMAIL", user.email, user.academicId, "Verifique seu e-mail",
                publicUrl + "/verify-email?token=" + token));
    }

    @Transactional
    public void verify(String token) {
        String hash = hash(token);
        var user = users.findFirstByVerificationHash(hash).orElseThrow(AccountLifecycle::invalid);
        if (!hash.equals(user.verificationHash) || !valid(user.verificationExpires)) throw invalid();
        user.emailVerified = true;
        user.verificationHash = null;
        user.verificationExpires = null;
    }

    @Transactional
    public void requestReset(String email) {
        users.findFirstByEmail(normalize(email)).ifPresent(user -> {
            Instant now = Instant.now();
            if (issuedRecently(user.resetExpires, RESET_TTL, now)) return;
            String token = token();
            user.resetHash = hash(token);
            user.resetExpires = now.plus(RESET_TTL);
            events.publishEvent(new Mail("RECUPERACAO_SENHA", user.email, user.academicId, "Redefina sua senha",
                    publicUrl + "/password-reset?token=" + token));
        });
    }

    @Transactional
    public void confirmReset(String token, String password) {
        String hash = hash(token);
        var user = users.findFirstByResetHash(hash).orElseThrow(AccountLifecycle::invalid);
        if (!hash.equals(user.resetHash) || !valid(user.resetExpires)) throw invalid();
        user.passwordHash = encode(password);
        user.resetHash = null;
        user.resetExpires = null;
    }

    @TransactionalEventListener
    public void deliver(Mail mail) {
        notifications.send(mail.type(), mail.email(), mail.academicId(), mail.subject(), mail.message());
    }

    private String encode(String password) {
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve conter de 8 caracteres a 72 bytes.");
        return passwords.encode(password);
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static boolean valid(Instant expires) { return expires != null && expires.isAfter(Instant.now()); }
    private static boolean issuedRecently(Instant expires, Duration lifetime, Instant now) {
        return expires != null && expires.minus(lifetime).plus(RESEND_INTERVAL).isAfter(now);
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido ou expirado.");
    }
    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado.");
    }
    private static String token() {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
