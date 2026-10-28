package com.unifor.br.distrischool.nota_service.repository;

import com.unifor.br.distrischool.nota_service.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface NotaRepository extends JpaRepository<Nota, Long> {
    List<Nota> findByAlunoId(Long alunoId);
}