package com.unifor.br.distrischool.turma_service.controller;

import com.unifor.br.distrischool.turma_service.model.Horario;
import com.unifor.br.distrischool.turma_service.service.HorarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/turmas/horarios")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @PostMapping
    public ResponseEntity<Horario> criarHorario(@RequestBody Horario horario) {
        Horario novoHorario = horarioService.alocarHorario(horario);
        return ResponseEntity.ok(novoHorario);
    }
}
