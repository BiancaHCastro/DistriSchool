package com.unifor.br.distrischool.attendance_service.repository;

import com.unifor.br.distrischool.attendance_service.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByTurmaIdAndData(String turmaId, LocalDate data);
    List<Attendance> findByAlunoId(Long alunoId);
    List<Attendance> findByAlunoIdAndTurmaId(Long alunoId, String turmaId);
}
