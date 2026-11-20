package com.unifor.br.distrischool.professorservice.repository;

import com.unifor.br.distrischool.professorservice.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByEmail(String email);
}
