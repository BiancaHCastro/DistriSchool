package com.unifor.br.distrischool.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountEndpoints {
    private final AccountLifecycle lifecycle;
    private final UserRepository users;
    private final byte[] internalKey;

    public AccountEndpoints(AccountLifecycle lifecycle, UserRepository users,
                            @Value("${NOTIFICATION_LOOKUP_KEY:}") String internalKey) {
        this.lifecycle = lifecycle;
        this.users = users;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    public record Registration(@NotBlank @Size(max=255) String name,
                               @NotBlank @Email @Size(max=255) String email, @NotBlank String password) {}
    public record Token(@NotBlank @Size(max=128) String token) {}
    public record ResetRequest(@NotBlank @Email @Size(max=255) String email) {}
    public record Reset(@NotBlank @Size(max=128) String token, @NotBlank String password) {}

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody Registration input) {
        lifecycle.register(input.name(), input.email(), input.password());
    }

    @PostMapping("/api/auth/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody Token input) { lifecycle.verify(input.token()); }

    @PostMapping("/api/auth/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody ResetRequest input) {
        lifecycle.resendVerification(input.email());
    }

    @PostMapping("/api/auth/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestReset(@Valid @RequestBody ResetRequest input) { lifecycle.requestReset(input.email()); }

    @PostMapping("/api/auth/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReset(@Valid @RequestBody Reset input) { lifecycle.confirmReset(input.token(), input.password()); }

    @GetMapping("/internal/notifications/student/{id}")
    public ResponseEntity<String> studentEmail(@PathVariable Long id,
                         @RequestHeader(value="X-Notification-Key", defaultValue="") String supplied) {
        if (internalKey.length == 0 || !MessageDigest.isEqual(internalKey, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ResponseEntity.of(users.findFirstByAcademicIdAndRoleAndEmailVerifiedTrueOrderByIdAsc(id, "ROLE_ALUNO")
                .map(user -> user.email));
    }
}
