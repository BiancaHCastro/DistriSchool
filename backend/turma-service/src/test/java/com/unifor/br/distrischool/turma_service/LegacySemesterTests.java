package com.unifor.br.distrischool.turma_service;

import com.unifor.br.distrischool.turma_service.controller.TurmaController;
import com.unifor.br.distrischool.turma_service.model.Turma;
import com.unifor.br.distrischool.turma_service.repository.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LegacySemesterTests {
    @Test
    void ignoraTurmaLegadaSemSemestreNaVerificacaoDeConflito() {
        var turmas = mock(TurmaRepository.class);
        var controller = new TurmaController(turmas, mock(MatriculaTurmaRepository.class), mock(RabbitTemplate.class));
        String schedule = "Seg / Qua 07:30 - 09:10 (Manha A)";
        var legacy = new Turma("LEG-1", "Legado", null, schedule, 10, 1L);
        var current = new Turma("NEW-1", "Nova", "2026.2", schedule, 10, 1L);
        when(turmas.findAll()).thenReturn(List.of(legacy));
        when(turmas.save(current)).thenReturn(current);
        assertEquals(HttpStatus.OK, controller.cadastrar(current).getStatusCode());
    }
}
