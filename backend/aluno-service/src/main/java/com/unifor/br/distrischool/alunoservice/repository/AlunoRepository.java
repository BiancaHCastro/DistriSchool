package com.unifor.br.distrischool.alunoservice.repository;

import com.unifor.br.distrischool.alunoservice.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

}