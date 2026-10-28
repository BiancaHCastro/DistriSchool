package com.unifor.br.distrischool.nota_service.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notas")
public class Nota {

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Avaliacao getAvaliacao() {
        return avaliacao;
    }

    public void setAvaliacao(Avaliacao avaliacao) {
        this.avaliacao = avaliacao;
    }

    public Long getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(Long alunoId) {
        this.alunoId = alunoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long alunoId;

    @ManyToOne
    @JoinColumn(name = "avaliacao_id")
    private Avaliacao avaliacao;

    private Double valor;
}