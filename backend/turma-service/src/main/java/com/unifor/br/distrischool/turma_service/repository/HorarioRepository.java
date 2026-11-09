package com.unifor.br.distrischool.turma_service.repository;

import com.unifor.br.distrischool.turma_service.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    @Query("SELECT h FROM Horario h WHERE h.diaSemana = :diaSemana AND (h.professorId = :professorId OR h.turma.id = :turmaId) AND (h.horaInicio < :horaFim AND h.horaFim > :horaInicio)")
    List<Horario> encontrarConflitos(
            @Param("professorId") Long professorId,
            @Param("turmaId") Long turmaId,
            @Param("diaSemana") String diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFim") LocalTime horaFim);
}
