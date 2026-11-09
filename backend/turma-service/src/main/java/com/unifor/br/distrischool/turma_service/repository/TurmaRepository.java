package com.unifor.br.distrischool.turma_service.repository;

import com.unifor.br.distrischool.turma_service.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurmaRepository extends JpaRepository<Turma, Long> {
}
