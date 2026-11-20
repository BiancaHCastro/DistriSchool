package com.unifor.br.distrischool.attendance_service;

import com.unifor.br.distrischool.attendance_service.dto.AttendanceRequestDTO;
import com.unifor.br.distrischool.attendance_service.model.*;
import com.unifor.br.distrischool.attendance_service.repository.AttendanceRepository;
import com.unifor.br.distrischool.attendance_service.service.AttendanceService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttendanceRulesTests {
    private final LocalDate date = LocalDate.of(2026, 11, 20);
    private final AttendanceRepository repository = mock(AttendanceRepository.class);
    private final Notifications notifications = mock(Notifications.class);
    private final AttendanceService service = new AttendanceService(repository, mock(RabbitTemplate.class), notifications);

    @Test
    void filtraSomentePelaData() {
        var expected = List.of(new Attendance(1L, "T-1", date, AttendanceStatus.PRESENCA));
        when(repository.findByData(date)).thenReturn(expected);
        assertEquals(expected, service.listarPorTurmaEData(null, date));
        verify(repository, never()).findAll();
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(AttendanceStatus.class)
    void publicaFaltaSomenteNaTransicao(AttendanceStatus previous) {
        var existing = Optional.ofNullable(previous).map(status -> new Attendance(1L, "T-1", date, status));
        when(repository.findFirstByAlunoIdAndTurmaIdAndDataOrderByIdAsc(1L, "T-1", date)).thenReturn(existing);
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var attendance = new AttendanceRequestDTO();
        attendance.setAlunoId(1L);
        attendance.setTurmaId("T-1");
        attendance.setData(date);
        attendance.setStatus(AttendanceStatus.FALTA);
        service.registrarChamada(attendance);
        verify(notifications, times(previous == AttendanceStatus.FALTA ? 0 : 1))
                .sendAcademic(eq("FALTA_REGISTRADA"), eq(1L), anyString(), anyString());
    }
}
