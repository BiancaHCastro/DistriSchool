package com.unifor.br.distrischool.nota_service.repository;

import com.unifor.br.distrischool.nota_service.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface NotaRepository extends JpaRepository<Nota, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Nota> findFirstByAlunoIdAndTurmaIdOrderByIdAsc(Long alunoId, String turmaId);

    List<Nota> findByAlunoId(Long alunoId);
}