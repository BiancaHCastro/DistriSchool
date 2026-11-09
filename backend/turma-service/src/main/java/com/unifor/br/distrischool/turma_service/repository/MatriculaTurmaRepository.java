package com.unifor.br.distrischool.turma_service.repository;

import com.unifor.br.distrischool.turma_service.model.MatriculaTurma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatriculaTurmaRepository extends JpaRepository<MatriculaTurma, Long> {
    List<MatriculaTurma> findByTurmaId(Long turmaId);
    List<MatriculaTurma> findByAlunoId(Long alunoId);
    boolean existsByTurmaIdAndAlunoId(Long turmaId, Long alunoId);
    void deleteByTurmaIdAndAlunoId(Long turmaId, Long alunoId);
}
