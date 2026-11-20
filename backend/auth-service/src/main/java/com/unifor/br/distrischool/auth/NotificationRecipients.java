package com.unifor.br.distrischool.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NotificationRecipients {
    private final UserRepository users;
    private final byte[] internalKey;

    public NotificationRecipients(UserRepository users, @Value("${NOTIFICATION_LOOKUP_KEY:}") String internalKey) {
        this.users = users;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/internal/notifications/student/{id}")
    public ResponseEntity<String> studentEmail(@PathVariable Long id,
            @RequestHeader(value = "X-Notification-Key", defaultValue = "") String supplied) {
        if (internalKey.length == 0 || !MessageDigest.isEqual(internalKey, supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.of(users.findFirstByAcademicIdAndRoleAndEnabledTrueOrderByIdAsc(id, "ROLE_ALUNO")
                .map(user -> user.email));
    }
}
