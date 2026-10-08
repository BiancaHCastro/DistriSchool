package com.distrischool.alunoservice.repository;

import com.distrischool.alunoservice.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

}