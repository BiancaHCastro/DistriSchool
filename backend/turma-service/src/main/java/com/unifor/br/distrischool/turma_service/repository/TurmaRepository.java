package com.unifor.br.distrischool.turma_service.repository;

import com.unifor.br.distrischool.turma_service.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurmaRepository extends JpaRepository<Turma, Long> {
    boolean existsByTeacherIdAndHorarioAndSemestre(Long teacherId, String horario, String semestre);
    boolean existsByTeacherIdAndHorarioAndSemestreAndIdNot(Long teacherId, String horario, String semestre, Long id);
    List<Turma> findByCodigoStartingWith(String prefix);
}
