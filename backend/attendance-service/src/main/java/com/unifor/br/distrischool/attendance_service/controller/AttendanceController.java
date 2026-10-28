package com.unifor.br.distrischool.attendance_service.controller;

import com.unifor.br.distrischool.attendance_service.dto.AttendanceRequestDTO;
import com.unifor.br.distrischool.attendance_service.model.Attendance;
import com.unifor.br.distrischool.attendance_service.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/presencas", "/api/academic/presencas"}, produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Attendance> registrar(@Valid @RequestBody AttendanceRequestDTO dto) {
        Attendance attendance = service.registrarChamada(dto);
        return ResponseEntity.ok(attendance);
    }

    @GetMapping
    public ResponseEntity<List<Attendance>> listar(
            @RequestParam(required = false) String turmaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<Attendance> list = service.listarPorTurmaEData(turmaId, data);
        return ResponseEntity.ok(list != null ? list : Collections.emptyList());
    }

    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> relatorio(
            @RequestParam(required = false) Long alunoId,
            @RequestParam(required = false) String turmaId) {
        Map<String, Object> rel = service.consultarRelatorio(alunoId, turmaId);
        return ResponseEntity.ok(rel != null ? rel : Collections.emptyMap());
    }
}
