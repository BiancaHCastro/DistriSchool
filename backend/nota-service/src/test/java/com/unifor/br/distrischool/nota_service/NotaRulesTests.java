package com.unifor.br.distrischool.nota_service;

import com.unifor.br.distrischool.nota_service.messaging.MediaProducer;
import com.unifor.br.distrischool.nota_service.model.*;
import com.unifor.br.distrischool.nota_service.repository.*;
import com.unifor.br.distrischool.nota_service.service.NotaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotaRulesTests {
    @ParameterizedTest
    @CsvSource(value = {
        "5,NULL,10,PENDENTE,NULL", "3,4,10,REPROVADO,3.5",
        "7,7,0,APROVADO,7", "4,4,NULL,EM_AVF,NULL",
        "4,4,6,APROVADO,5", "6,6,3,REPROVADO,4.5"
    }, nullValues = "NULL")
    void calculaSituacao(Double av1, Double av2, Double avf, String situacao, Double mediaFinal) {
        var result = CriterioAvaliacao.avaliar(nota(av1, av2, avf));
        assertEquals(situacao, result.situacao().name());
        assertEquals(mediaFinal, result.mediaFinal());
    }

    @Test
    void aoSalvarAv2PreservaAv1ENotificaSomenteNaPrimeiraTransicao() {
        var repository = mock(NotaRepository.class);
        var notifications = mock(Notifications.class);
        var service = new NotaService(repository, mock(AvaliacaoRepository.class), mock(MediaProducer.class), notifications);
        var first = nota(6.0, null, null);
        first.setId(9L);
        when(repository.findFirstByAlunoIdAndTurmaIdOrderByIdAsc(1L, "T-1"))
                .thenReturn(java.util.Optional.of(first));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertSame(first, service.cadastrar(nota(null, 8.0, null)));
        assertEquals(6.0, first.getNota1());
        service.cadastrar(nota(null, null, 1.0));
        verify(notifications, times(1)).sendAcademic(eq("RESULTADO_NOTA"), eq(1L), anyString(), anyString());
    }

    @Test
    void mediaGeralConsideraSomenteDisciplinasFinalizadas() {
        var repository = mock(NotaRepository.class);
        var service = new NotaService(repository, mock(AvaliacaoRepository.class),
                mock(MediaProducer.class), mock(Notifications.class));
        when(repository.findByAlunoId(1L)).thenReturn(List.of(nota(5.0, null, null),
                nota(6.0, 6.0, null), nota(2.0, 4.0, 10.0), nota(6.0, 8.0, 0.0)));
        assertEquals(5.0, service.calcularMedia(1L));
    }

    private static Nota nota(Double av1, Double av2, Double avf) {
        var nota = new Nota();
        nota.setAlunoId(1L);
        nota.setTurmaId("T-1");
        nota.setNota1(av1);
        nota.setNota2(av2);
        nota.setNotaFinal(avf);
        return nota;
    }
}
