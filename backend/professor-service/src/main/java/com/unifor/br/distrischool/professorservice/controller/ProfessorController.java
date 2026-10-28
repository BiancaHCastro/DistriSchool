package com.unifor.br.distrischool.professorservice.controller;

import com.unifor.br.distrischool.professorservice.model.Professor;
import com.unifor.br.distrischool.professorservice.service.ProfessorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professores")
public class ProfessorController {

    private final ProfessorService service;

    public ProfessorController(ProfessorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody Professor professor) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(professor));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping
    public List<Professor> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Professor> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Professor> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody Professor professor) {
        return service.atualizar(id, professor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return service.excluir(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
