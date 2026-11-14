package com.unifor.br.distrischool.nota_service.controller;

import com.unifor.br.distrischool.nota_service.service.NotaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.unifor.br.distrischool.nota_service.model.Nota;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;





@RestController
@RequestMapping("/notas")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    @PostMapping
    public Nota cadastrar(@RequestBody Nota nota) {
        return notaService.cadastrar(nota);
    }

    @GetMapping
    public List<Nota> listarTodas() {
        return notaService.listarTodas();
    }

    @GetMapping("/aluno/{alunoId}")
    public List<Nota> listarPorAluno(@PathVariable Long alunoId) {
        return notaService.listarPorAluno(alunoId);
    }

    @GetMapping("/aluno/{alunoId}/media")
    public ResponseEntity<Double> consultarMedia(@PathVariable Long alunoId) {

        Double media = notaService.calcularMedia(alunoId);

        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(media);
    }








}