package com.unifor.br.distrischool.turma_service.controller;
import java.util.List;
import com.unifor.br.distrischool.turma_service.model.Disciplina;
import com.unifor.br.distrischool.turma_service.repository.DisciplinaRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
@RestController @RequestMapping("/api/disciplinas")
public class DisciplinaController {
 private final DisciplinaRepository repo;
 public DisciplinaController(DisciplinaRepository repo){this.repo=repo;}
 @GetMapping public List<Disciplina> list(){return repo.findAll();}
 @GetMapping("/{id}") public ResponseEntity<Disciplina> get(@PathVariable Long id){return ResponseEntity.of(repo.findById(id));}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED) public Disciplina create(@RequestBody Disciplina d){validate(d);d.setId(null);return repo.save(d);}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Disciplina> update(@PathVariable Long id,@RequestBody Disciplina d){validate(d);if(!repo.existsById(id))return ResponseEntity.notFound().build();d.setId(id);return ResponseEntity.ok(repo.save(d));}
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){repo.deleteById(id);}
 private void validate(Disciplina d){if(d.getNome()==null||d.getNome().isBlank()||d.getCargaHoraria()==null||d.getCargaHoraria()<=0)throw new IllegalArgumentException("Informe nome e carga horária positiva");}
}