package com.unifor.br.distrischool.turma_service.repository;
import com.unifor.br.distrischool.turma_service.model.SolicitacaoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SolicitacaoRepository extends JpaRepository<SolicitacaoMatricula,Long>{boolean existsByAlunoIdAndTurmaId(Long alunoId,Long turmaId);}