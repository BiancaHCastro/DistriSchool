package com.unifor.br.distrischool.attendance_service.service;

import com.unifor.br.distrischool.attendance_service.dto.AttendanceRequestDTO;
import com.unifor.br.distrischool.attendance_service.model.Attendance;
import com.unifor.br.distrischool.attendance_service.model.AttendanceStatus;
import com.unifor.br.distrischool.attendance_service.repository.AttendanceRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final com.unifor.br.distrischool.attendance_service.Notifications notifications;

    private final AttendanceRepository repository;
    private final RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "attendance.recorded";

    public AttendanceService(AttendanceRepository repository, RabbitTemplate rabbitTemplate,
                       com.unifor.br.distrischool.attendance_service.Notifications notifications) {
        this.notifications = notifications;
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @org.springframework.transaction.annotation.Transactional
    public Attendance registrarChamada(AttendanceRequestDTO dto) {
        Attendance attendance = new Attendance(
                dto.getAlunoId(),
                dto.getTurmaId(),
                dto.getData() != null ? dto.getData() : LocalDate.now(),
                dto.getStatus()
        );
        var previous = repository.findFirstByAlunoIdAndTurmaIdAndDataOrderByIdAsc(
                attendance.getAlunoId(), attendance.getTurmaId(), attendance.getData());
        AttendanceStatus previousStatus = previous.map(Attendance::getStatus).orElse(null);
        previous.ifPresent(record -> attendance.setId(record.getId()));
        Attendance saved = repository.saveAndFlush(attendance);

        try {
            rabbitTemplate.convertAndSend(QUEUE_NAME, saved);
        } catch (Exception e) {
            System.err.println("Erro ao publicar evento no RabbitMQ: " + e.getMessage());
        }

        if (saved.getStatus() == AttendanceStatus.FALTA
                && previousStatus != AttendanceStatus.FALTA) {
            notifications.sendAcademic("FALTA_REGISTRADA", saved.getAlunoId(), "Falta registrada",
                    "Turma " + saved.getTurmaId() + ", data " + saved.getData());
        }
        return saved;
    }

    public List<Attendance> listarPorTurmaEData(String turmaId, LocalDate data) {
        if (turmaId != null && data != null) {
            return repository.findByTurmaIdAndData(turmaId, data);
        } else if (turmaId != null) {
            return repository.findAll().stream().filter(a -> a.getTurmaId().equals(turmaId)).toList();
        }
        return data == null ? repository.findAll() : repository.findByData(data);
    }

    public Map<String, Object> consultarRelatorio(Long alunoId, String turmaId) {
        List<Attendance> attendances;
        if (alunoId != null && turmaId != null) {
            attendances = repository.findByAlunoIdAndTurmaId(alunoId, turmaId);
        } else if (alunoId != null) {
            attendances = repository.findByAlunoId(alunoId);
        } else if (turmaId != null) {
            attendances = repository.findAll().stream().filter(a -> a.getTurmaId().equals(turmaId)).toList();
        } else {
            attendances = repository.findAll();
        }

        long total = attendances.size();
        long presencas = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENCA || a.getStatus() == AttendanceStatus.ATRASO).count();
        long faltas = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.FALTA).count();
        double percentualPresenca = total > 0 ? ((double) presencas / total) * 100 : 100.0;

        Map<String, Object> report = new HashMap<>();
        report.put("totalAulas", total);
        report.put("presencas", presencas);
        report.put("faltas", faltas);
        report.put("percentualPresenca", percentualPresenca);
        report.put("historico", attendances);
        return report;
    }
}
