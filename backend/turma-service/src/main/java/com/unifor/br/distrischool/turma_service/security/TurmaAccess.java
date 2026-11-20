package com.unifor.br.distrischool.turma_service.security;
import java.util.*;
import org.springframework.stereotype.Component;
import com.unifor.br.distrischool.turma_service.model.Turma;
import com.unifor.br.distrischool.turma_service.repository.*;
@Component("turmaAccess")
public class TurmaAccess {
 private final TurmaRepository turmas; private final MatriculaTurmaRepository matriculas;
 public TurmaAccess(TurmaRepository turmas,MatriculaTurmaRepository matriculas){this.turmas=turmas;this.matriculas=matriculas;}
 public boolean teacher(Long id){return Identity.role("ADMIN")||(Identity.role("PROFESSOR")&&turmas.findById(id).map(t->Identity.own(t.getTeacherId())).orElse(false));}
 public List<Turma> visible(){return turmas.findAll().stream().filter(t->Identity.role("ADMIN")||(Identity.role("PROFESSOR")&&Identity.own(t.getTeacherId()))||(Identity.role("ALUNO")&&Identity.academicId()!=null&&matriculas.existsByTurmaIdAndAlunoId(t.getId(),Identity.academicId()))).toList();}
 public Map<String,Object> scope(){var list=visible();var ids=new ArrayList<String>();var students=new HashSet<Long>();for(var t:list){ids.add(t.getId().toString());if(t.getCodigo()!=null)ids.add(t.getCodigo());for(var m:matriculas.findByTurmaId(t.getId())){if(!Identity.role("ALUNO")||Identity.own(m.getAlunoId()))students.add(m.getAlunoId());}}return Map.of("turmaIds",ids,"alunoIds",students);}
 public boolean enrolled(String code,Long aluno){return visible().stream().anyMatch(t->(String.valueOf(t.getId()).equals(code)||Objects.equals(t.getCodigo(),code))&&matriculas.existsByTurmaIdAndAlunoId(t.getId(),aluno));}
}