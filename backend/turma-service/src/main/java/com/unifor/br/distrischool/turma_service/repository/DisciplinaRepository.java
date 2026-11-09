package com.unifor.br.distrischool.turma_service.repository;

import com.unifor.br.distrischool.turma_service.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
}
