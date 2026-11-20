package com.unifor.br.distrischool.turma_service;

import com.unifor.br.distrischool.turma_service.controller.ApprovalNotifications;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;

class ApprovalNotificationsTests {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class Transactions {}

    @Test
    void publicaSomenteDepoisDoCommit() {
        var notifications = mock(Notifications.class);
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:approval-events", "sa", "");
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Transactions.class);
            context.registerBean(ApprovalNotifications.class, () -> new ApprovalNotifications(notifications));
            context.refresh();
            var approved = new ApprovalNotifications.Approved(1L, 2L);

            transaction.executeWithoutResult(status -> {
                context.publishEvent(approved);
                verifyNoInteractions(notifications);
                status.setRollbackOnly();
            });
            verifyNoInteractions(notifications);

            transaction.executeWithoutResult(status -> {
                context.publishEvent(approved);
                verifyNoInteractions(notifications);
            });
            verify(notifications).sendAcademic(eq("MATRICULA_APROVADA"), eq(1L), anyString(), anyString());
        }
    }
}
