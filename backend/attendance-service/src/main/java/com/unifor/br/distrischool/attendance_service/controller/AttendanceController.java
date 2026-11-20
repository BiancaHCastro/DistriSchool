package com.unifor.br.distrischool.attendance_service.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import com.unifor.br.distrischool.attendance_service.security.Identity;

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
@RequestMapping(value = {"/presencas", "/api/academic/presencas", "/api/attendance"}, produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService service;
    @org.springframework.beans.factory.annotation.Autowired private com.unifor.br.distrischool.attendance_service.security.AcademicAccess access;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Attendance> registrar(@Valid @RequestBody AttendanceRequestDTO dto) {
        if(!access.write(dto.getAlunoId(),dto.getTurmaId()))throw new org.springframework.security.access.AccessDeniedException("Acesso restrito às próprias turmas");
        Attendance attendance = service.registrarChamada(dto);
        return ResponseEntity.ok(attendance);
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
    @GetMapping
    public ResponseEntity<List<Attendance>> listar(
            @RequestParam(required = false) String turmaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<Attendance> list = service.listarPorTurmaEData(turmaId, data);
        if(list==null)return ResponseEntity.ok(Collections.emptyList());
        if(Identity.role("ALUNO"))list=list.stream().filter(a->Identity.own(a.getAlunoId())).toList();
        else if(!Identity.role("ADMIN")){var allowed=access.scope().turmaIds();list=list.stream().filter(a->allowed.contains(a.getTurmaId())).toList();}
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
    @GetMapping(value = "/relatorio/export", produces = {"text/csv", "application/pdf"})
    public ResponseEntity<byte[]> exportar(@RequestParam String formato,
            @RequestParam(required = false) String turmaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        var rows = listar(turmaId, data).getBody().stream()
                .map(a -> List.of(String.valueOf(a.getAlunoId()), a.getTurmaId(),
                        a.getData().toString(), a.getStatus().name())).toList();
        return ReportExport.download(formato, "frequencia", "Relatorio de Frequencia",
                List.of("Aluno", "Turma", "Data", "Status"), rows);
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> relatorio(
            @RequestParam(required = false) Long alunoId,
            @RequestParam(required = false) String turmaId) {
        if(Identity.role("ALUNO")){if(alunoId!=null&&!Identity.own(alunoId))throw new org.springframework.security.access.AccessDeniedException("Acesso restrito ao próprio histórico");alunoId=Identity.academicId();if(alunoId==null)return ResponseEntity.ok(Collections.emptyMap());}
        else if(!Identity.role("ADMIN")&&!access.turma(turmaId))throw new org.springframework.security.access.AccessDeniedException("Selecione uma de suas turmas");
        Map<String, Object> rel = service.consultarRelatorio(alunoId, turmaId);
        return ResponseEntity.ok(rel != null ? rel : Collections.emptyMap());
    }
}
