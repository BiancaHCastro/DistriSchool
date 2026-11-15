package com.unifor.br.distrischool.professorservice.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import com.unifor.br.distrischool.professorservice.security.Identity;

import com.unifor.br.distrischool.professorservice.model.Professor;
import com.unifor.br.distrischool.professorservice.service.ProfessorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/professores", "/api/teachers"})
@CrossOrigin(origins = "*")
public class ProfessorController {

    private final ProfessorService service;

    public ProfessorController(ProfessorService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody Professor professor) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(professor));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    @GetMapping
    public List<Professor> listar() {
        return service.listar().stream().filter(p->Identity.role("ADMIN")||Identity.own(p.getId())).toList();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROFESSOR') and T(com.unifor.br.distrischool.professorservice.security.Identity).own(#id))")
    @GetMapping("/{id}")
    public ResponseEntity<Professor> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROFESSOR') and T(com.unifor.br.distrischool.professorservice.security.Identity).own(#id))")
    @PutMapping("/{id}")
    public ResponseEntity<Professor> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody Professor professor) {
        return service.atualizar(id, professor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return service.excluir(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
