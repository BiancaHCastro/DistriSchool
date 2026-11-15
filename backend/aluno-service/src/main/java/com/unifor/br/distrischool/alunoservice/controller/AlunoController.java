package com.unifor.br.distrischool.alunoservice.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import com.unifor.br.distrischool.alunoservice.security.Identity;

import com.unifor.br.distrischool.alunoservice.model.Aluno;
import com.unifor.br.distrischool.alunoservice.service.AlunoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/alunos", "/api/students"})
@CrossOrigin(origins = "*")
public class AlunoController {

    private final AlunoService service;
    @org.springframework.beans.factory.annotation.Autowired private com.unifor.br.distrischool.alunoservice.security.AcademicAccess access;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Aluno cadastrar(@RequestBody Aluno aluno) {
        return service.cadastrar(aluno);
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
    @GetMapping
    public List<Aluno> listar() {
        if(Identity.role("ADMIN"))return service.listar();
        if(Identity.role("ALUNO"))return service.listar().stream().filter(a->Identity.own(a.getId())).toList();
        var ids=access.scope().alunoIds();return service.listar().stream().filter(a->ids.contains(a.getId())).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
    @GetMapping("/{id}")
    public ResponseEntity<Aluno> buscarPorId(@PathVariable Long id) {
        if(!access.student(id))throw new org.springframework.security.access.AccessDeniedException("Acesso restrito ao vínculo acadêmico");
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (service.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Aluno> atualizar(
            @PathVariable Long id,
            @RequestBody Aluno aluno) {

        return service.atualizar(id, aluno)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}