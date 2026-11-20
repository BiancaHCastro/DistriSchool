package com.unifor.br.distrischool.turma_service.repository;
import com.unifor.br.distrischool.turma_service.model.SolicitacaoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SolicitacaoRepository extends JpaRepository<SolicitacaoMatricula,Long>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select r from SolicitacaoMatricula r where r.id = :id")
 java.util.Optional<SolicitacaoMatricula> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
boolean existsByAlunoIdAndTurmaId(Long alunoId,Long turmaId);}