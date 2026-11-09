package com.unifor.br.distrischool.turma_service.controller;

import com.unifor.br.distrischool.turma_service.model.MatriculaTurma;
import com.unifor.br.distrischool.turma_service.model.Turma;
import com.unifor.br.distrischool.turma_service.repository.MatriculaTurmaRepository;
import com.unifor.br.distrischool.turma_service.repository.TurmaRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/turmas", "/api/turmas"})
@CrossOrigin(origins = "*")
public class TurmaController {

    private final TurmaRepository turmaRepository;
    private final MatriculaTurmaRepository matriculaTurmaRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "schedule.updated";

    public TurmaController(TurmaRepository turmaRepository, MatriculaTurmaRepository matriculaTurmaRepository, RabbitTemplate rabbitTemplate) {
        this.turmaRepository = turmaRepository;
        this.matriculaTurmaRepository = matriculaTurmaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public List<Turma> listar() {
        return turmaRepository.findAll();
    }

    @PostMapping
    public Turma cadastrar(@RequestBody Turma turma) {
        Turma saved = turmaRepository.save(turma);
        try {
            rabbitTemplate.convertAndSend(QUEUE_NAME, saved);
        } catch (Exception e) {
            System.err.println("Erro ao publicar evento no RabbitMQ para turma: " + e.getMessage());
        }
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Turma> atualizar(@PathVariable Long id, @RequestBody Turma novosDados) {
        return turmaRepository.findById(id).map(turma -> {
            turma.setCodigo(novosDados.getCodigo());
            turma.setNomeDisciplina(novosDados.getNomeDisciplina());
            turma.setSemestre(novosDados.getSemestre());
            turma.setHorario(novosDados.getHorario());
            turma.setCapacidadeMaxima(novosDados.getCapacidadeMaxima());
            turma.setTeacherId(novosDados.getTeacherId());
            Turma updated = turmaRepository.save(turma);
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME, updated);
            } catch (Exception e) {
                System.err.println("Erro ao publicar evento no RabbitMQ para turma atualizada: " + e.getMessage());
            }
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (turmaRepository.existsById(id)) {
            turmaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- Matrícula de Alunos em Turmas (Muitos para Muitos) ---

    @PostMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<?> matricularAluno(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        Turma turma = turmaRepository.findById(turmaId).orElse(null);
        if (turma == null) {
            return ResponseEntity.notFound().build();
        }

        List<MatriculaTurma> matriculasAtuais = matriculaTurmaRepository.findByTurmaId(turmaId);
        if (turma.getCapacidadeMaxima() != null && matriculasAtuais.size() >= turma.getCapacidadeMaxima()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Turma lotada! Capacidade máxima atingida.\"}");
        }

        if (matriculaTurmaRepository.existsByTurmaIdAndAlunoId(turmaId, alunoId)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Aluno já matriculado nesta turma.\"}");
        }

        MatriculaTurma matricula = new MatriculaTurma(turmaId, alunoId);
        matriculaTurmaRepository.save(matricula);
        return ResponseEntity.ok(matricula);
    }

    @DeleteMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<?> desmatricularAluno(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        if (!matriculaTurmaRepository.existsByTurmaIdAndAlunoId(turmaId, alunoId)) {
            return ResponseEntity.notFound().build();
        }
        matriculaTurmaRepository.deleteByTurmaIdAndAlunoId(turmaId, alunoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{turmaId}/alunos")
    public List<MatriculaTurma> listarAlunosDaTurma(@PathVariable Long turmaId) {
        return matriculaTurmaRepository.findByTurmaId(turmaId);
    }
}
