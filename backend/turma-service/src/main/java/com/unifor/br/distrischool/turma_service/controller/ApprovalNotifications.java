package com.unifor.br.distrischool.turma_service.controller;

import com.unifor.br.distrischool.turma_service.Notifications;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ApprovalNotifications {
    public record Approved(Long alunoId, Long turmaId) {}
    private final Notifications notifications;

    public ApprovalNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void notifyApproval(Approved event) {
        notifications.sendAcademic("MATRICULA_APROVADA", event.alunoId(), "Matricula aprovada",
                "Sua matricula na turma " + event.turmaId() + " foi aprovada.");
    }
}
