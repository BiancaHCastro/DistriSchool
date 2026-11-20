package com.unifor.br.distrischool.nota_service.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notas")
public class Nota {
    private String turmaId;
    private Double nota1;
    private Double nota2;
    private Double notaFinal;
    public CriterioAvaliacao.Situacao getSituacao() { return CriterioAvaliacao.avaliar(this).situacao(); }
    public Double getMedia() { return CriterioAvaliacao.avaliar(this).media(); }
    public Double getMediaFinal() { return CriterioAvaliacao.avaliar(this).mediaFinal(); }

    public String getTurmaId(){return turmaId;}
    public void setTurmaId(String value){turmaId=value;}
    public Double getNota1(){return nota1;}
    public void setNota1(Double value){nota1=value;}
    public Double getNota2(){return nota2;}
    public void setNota2(Double value){nota2=value;}
    public Double getNotaFinal(){return notaFinal;}
    public void setNotaFinal(Double value){notaFinal=value;}


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