package com.distrischool.alunoservice.controller;

import com.distrischool.alunoservice.model.Aluno;
import com.distrischool.alunoservice.service.AlunoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoService service;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    @PostMapping
    public Aluno cadastrar(@RequestBody Aluno aluno) {
        return service.cadastrar(aluno);
    }

    @GetMapping
    public List<Aluno> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Aluno> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (service.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Aluno> atualizar(
            @PathVariable Long id,
            @RequestBody Aluno aluno) {

        return service.atualizar(id, aluno)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}