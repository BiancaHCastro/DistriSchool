package com.unifor.br.distrischool.turma_service.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "turmas")
public class Turma implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nomeDisciplina;

    @Column(nullable = false)
    private String semestre;

    private String horario;

    private Integer capacidadeMaxima;

    private Long teacherId;

    public Turma() {}

    public Turma(String codigo, String nomeDisciplina, String semestre, String horario, Integer capacidadeMaxima, Long teacherId) {
        this.codigo = codigo;
        this.nomeDisciplina = nomeDisciplina;
        this.semestre = semestre;
        this.horario = horario;
        this.capacidadeMaxima = capacidadeMaxima;
        this.teacherId = teacherId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNomeDisciplina() { return nomeDisciplina; }
    public void setNomeDisciplina(String nomeDisciplina) { this.nomeDisciplina = nomeDisciplina; }

    public String getSemestre() { return semestre; }
    public void setSemestre(String semestre) { this.semestre = semestre; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    public Integer getCapacidadeMaxima() { return capacidadeMaxima; }
    public void setCapacidadeMaxima(Integer capacidadeMaxima) { this.capacidadeMaxima = capacidadeMaxima; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}
