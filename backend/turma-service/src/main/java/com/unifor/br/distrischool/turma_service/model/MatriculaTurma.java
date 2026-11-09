package com.unifor.br.distrischool.turma_service.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "turma_alunos", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"turma_id", "aluno_id"})
})
public class MatriculaTurma implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    public MatriculaTurma() {}

    public MatriculaTurma(Long turmaId, Long alunoId) {
        this.turmaId = turmaId;
        this.alunoId = alunoId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTurmaId() { return turmaId; }
    public void setTurmaId(Long turmaId) { this.turmaId = turmaId; }

    public Long getAlunoId() { return alunoId; }
    public void setAlunoId(Long alunoId) { this.alunoId = alunoId; }
}
