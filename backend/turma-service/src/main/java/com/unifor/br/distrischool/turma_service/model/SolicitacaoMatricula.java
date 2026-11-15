package com.unifor.br.distrischool.turma_service.model;
import jakarta.persistence.*;
@Entity @Table(name="solicitacoes_matricula",uniqueConstraints=@UniqueConstraint(columnNames={"alunoId","turmaId"}))
public class SolicitacaoMatricula {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long alunoId;
 @Column(nullable=false) public Long turmaId;
 @Column(nullable=false) public String status="PENDENTE";
}