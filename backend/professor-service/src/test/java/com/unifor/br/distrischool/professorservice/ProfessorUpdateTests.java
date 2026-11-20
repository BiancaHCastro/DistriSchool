package com.unifor.br.distrischool.professorservice;

import com.unifor.br.distrischool.professorservice.controller.ProfessorController;
import com.unifor.br.distrischool.professorservice.model.Professor;
import com.unifor.br.distrischool.professorservice.repository.ProfessorRepository;
import com.unifor.br.distrischool.professorservice.service.ProfessorService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfessorUpdateTests {
    @Test
    void retornaConflitoQuandoEmailPertenceAOutroProfessor() {
        var repository = mock(ProfessorRepository.class);
        var professor = new Professor();
        professor.setEmail("anterior@example.org");
        var update = new Professor();
        update.setEmail("outro@example.org");
        when(repository.findById(1L)).thenReturn(Optional.of(professor));
        when(repository.existsByEmailAndIdNot(update.getEmail(), 1L)).thenReturn(true);
        var controller = new ProfessorController(new ProfessorService(repository, mock(RabbitTemplate.class)));
        assertEquals(HttpStatus.CONFLICT, controller.atualizar(1L, update).getStatusCode());
        assertEquals("anterior@example.org", professor.getEmail());
        verify(repository, never()).save(any());
    }
}
