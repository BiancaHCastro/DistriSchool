package com.unifor.br.distrischool.nota_service.controller;
import com.unifor.br.distrischool.nota_service.service.NotaService;
import com.unifor.br.distrischool.nota_service.model.Nota;
import com.unifor.br.distrischool.nota_service.repository.NotaRepository;
import com.unifor.br.distrischool.nota_service.security.*;
import java.util.List;
import com.unifor.br.distrischool.nota_service.model.CriterioAvaliacao;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
@RestController @RequestMapping({"/notas","/api/notas"})
public class NotaController {
 private final NotaService service;private final NotaRepository repo;private final AcademicAccess access;
 public NotaController(NotaService service,NotaRepository repo,AcademicAccess access){this.service=service;this.repo=repo;this.access=access;}
 @PostMapping @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')") public Nota save(@RequestBody Nota nota){if(!access.write(nota.getAlunoId(),nota.getTurmaId()))throw new org.springframework.security.access.AccessDeniedException("Acesso restrito às próprias turmas");return service.cadastrar(nota);}
 @GetMapping public List<Nota> list(){
  if(Identity.role("ADMIN"))return service.listarTodas();
  if(Identity.role("ALUNO"))return service.listarTodas().stream().filter(n->Identity.own(n.getAlunoId())).toList();
  var turmas=access.scope().turmaIds();return service.listarTodas().stream().filter(n->turmas.contains(n.getTurmaId())).toList();
 }
 @GetMapping("/aluno/{alunoId}") @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')") public List<Nota> student(@PathVariable Long alunoId){if(!access.student(alunoId))throw new org.springframework.security.access.AccessDeniedException("Acesso restrito ao vínculo acadêmico");return list().stream().filter(n->alunoId.equals(n.getAlunoId())).toList();}
 @GetMapping("/aluno/{alunoId}/situacao")
 @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
 public List<CriterioAvaliacao.Resultado> situacao(@PathVariable Long alunoId) {
  return student(alunoId).stream().filter(nota -> nota.getTurmaId() != null)
          .map(CriterioAvaliacao::avaliar).toList();
 }
 @GetMapping("/aluno/{alunoId}/media") @PreAuthorize("hasRole('ADMIN') or (hasRole('ALUNO') and T(com.unifor.br.distrischool.nota_service.security.Identity).own(#alunoId))") public ResponseEntity<Double> average(@PathVariable Long alunoId){return ResponseEntity.ofNullable(service.calcularMedia(alunoId));}
 @GetMapping("/relatorio/export")
 @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
 public ResponseEntity<byte[]> exportar(@RequestParam String formato,
          @RequestParam(required=false) String turmaId, @RequestParam(required=false) Long alunoId) {
  var rows = (alunoId == null ? list() : student(alunoId)).stream()
          .filter(n -> n.getTurmaId() != null && (turmaId == null || turmaId.equals(n.getTurmaId())))
          .map(n -> {
           var r = CriterioAvaliacao.avaliar(n);
           return List.of(String.valueOf(n.getAlunoId()), r.turmaId(), cell(r.nota1()), cell(r.nota2()),
                   cell(r.notaFinal()), cell(r.media()), cell(r.mediaFinal()), r.situacao().name());
          }).toList();
  return ReportExport.download(formato, "notas", "Relatorio de Notas",
          List.of("Aluno", "Turma", "AV1", "AV2", "AVF", "Media", "Media Final", "Situacao"), rows);
 }
 private static String cell(Double value) { return value == null ? "" : value.toString(); }
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){var nota = repo.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND)); repo.delete(nota);}
}