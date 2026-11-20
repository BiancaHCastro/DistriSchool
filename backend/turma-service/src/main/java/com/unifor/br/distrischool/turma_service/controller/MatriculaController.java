package com.unifor.br.distrischool.turma_service.controller;
import java.util.*;
import com.unifor.br.distrischool.turma_service.model.*;
import com.unifor.br.distrischool.turma_service.repository.*;
import com.unifor.br.distrischool.turma_service.security.Identity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/api/matriculas")
public class MatriculaController {
 @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
 private final SolicitacaoRepository requests;private final TurmaRepository turmas;private final MatriculaTurmaRepository matriculas;
 public MatriculaController(SolicitacaoRepository requests,TurmaRepository turmas,MatriculaTurmaRepository matriculas){this.requests=requests;this.turmas=turmas;this.matriculas=matriculas;}
 public record Request(Long turmaId){}
 @GetMapping @PreAuthorize("hasAnyRole('ADMIN','ALUNO')") public List<SolicitacaoMatricula> list(){return requests.findAll().stream().filter(r->Identity.role("ADMIN")||Identity.own(r.alunoId)).toList();}
 @PostMapping @PreAuthorize("hasRole('ALUNO')") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public SolicitacaoMatricula request(@RequestBody Request input){
  if(Identity.academicId()==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Solicite à administração o vínculo do seu cadastro acadêmico");
  if(input.turmaId()==null||!turmas.existsById(input.turmaId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Turma não encontrada");
  if(requests.existsByAlunoIdAndTurmaId(Identity.academicId(),input.turmaId())||matriculas.existsByTurmaIdAndAlunoId(input.turmaId(),Identity.academicId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Matrícula já solicitada ou efetivada");
  var r=new SolicitacaoMatricula();r.alunoId=Identity.academicId();r.turmaId=input.turmaId();return requests.save(r);
 }
 @PutMapping("/{id}/aprovar") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public SolicitacaoMatricula approve(@PathVariable Long id){
  var r=requests.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var t=turmas.findById(r.turmaId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!matriculas.existsByTurmaIdAndAlunoId(t.getId(),r.alunoId)){
   if(t.getCapacidadeMaxima()!=null&&matriculas.findByTurmaId(t.getId()).size()>=t.getCapacidadeMaxima())throw new ResponseStatusException(HttpStatus.CONFLICT,"Turma lotada");
   matriculas.save(new MatriculaTurma(t.getId(),r.alunoId));
  }
  if (!"APROVADA".equals(r.status)) {
   r.status = "APROVADA";
   requests.save(r);
   events.publishEvent(new ApprovalNotifications.Approved(r.alunoId, r.turmaId));
  }
  return r;
 }
}