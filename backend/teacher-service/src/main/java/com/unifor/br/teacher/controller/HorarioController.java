package com.unifor.br.teacher.controller;

import com.unifor.br.teacher.model.Horario;
import com.unifor.br.teacher.service.HorarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers/horarios")
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