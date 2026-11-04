package com.unifor.br.distrischool.attendance_service.dto;

import com.unifor.br.distrischool.attendance_service.model.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AttendanceRequestDTO {

    @NotNull(message = "O ID do aluno é obrigatório")
    private Long alunoId;

    @NotNull(message = "O ID da turma é obrigatório")
    private String turmaId;

    @NotNull(message = "A data é obrigatória")
    private LocalDate data;

    @NotNull(message = "O status da presença é obrigatório")
    private AttendanceStatus status;

    public Long getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(Long alunoId) {
        this.alunoId = alunoId;
    }

    public String getTurmaId() {
        return turmaId;
    }

    public void setTurmaId(String turmaId) {
        this.turmaId = turmaId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
