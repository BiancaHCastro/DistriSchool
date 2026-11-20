package com.unifor.br.distrischool.turma_service.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import com.unifor.br.distrischool.turma_service.security.Identity;

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
    @org.springframework.beans.factory.annotation.Autowired private com.unifor.br.distrischool.turma_service.security.TurmaAccess access;
    private final MatriculaTurmaRepository matriculaTurmaRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "schedule.updated";

    public TurmaController(TurmaRepository turmaRepository, MatriculaTurmaRepository matriculaTurmaRepository, RabbitTemplate rabbitTemplate) {
        this.turmaRepository = turmaRepository;
        this.matriculaTurmaRepository = matriculaTurmaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','ALUNO')")
    @GetMapping
    public List<Turma> listar() {
        return turmaRepository.findAll().stream().filter(t->!Identity.role("PROFESSOR")||Identity.own(t.getTeacherId())).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Turma turma) {
        if (turma.getTeacherId() != null && turma.getHorario() != null && !turma.getHorario().trim().isEmpty() && turma.getSemestre() != null) {
            boolean conflict = temConflitoHorarioProfessor(turma.getTeacherId(), turma.getHorario(), turma.getSemestre(), null);
            if (conflict) {
                return ResponseEntity.badRequest().body("{\"error\": \"O professor já possui turma alocada neste mesmo horário e semestre!\"}");
            }
        }

        if (turma.getCodigo() == null || turma.getCodigo().trim().isEmpty()) {
            turma.setCodigo(gerarCodigoDinamico(turma.getNomeDisciplina()));
        }

        Turma saved = turmaRepository.save(turma);
        try {
            rabbitTemplate.convertAndSend(QUEUE_NAME, saved);
        } catch (Exception e) {
            System.err.println("Erro ao publicar evento no RabbitMQ para turma: " + e.getMessage());
        }
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Turma novosDados) {
        if (novosDados.getTeacherId() != null && novosDados.getHorario() != null && !novosDados.getHorario().trim().isEmpty() && novosDados.getSemestre() != null) {
            boolean conflict = temConflitoHorarioProfessor(novosDados.getTeacherId(), novosDados.getHorario(), novosDados.getSemestre(), id);
            if (conflict) {
                return ResponseEntity.badRequest().body("{\"error\": \"O professor já possui turma alocada neste mesmo horário e semestre!\"}");
            }
        }

        return turmaRepository.findById(id).map(turma -> {
            if (novosDados.getCodigo() != null && !novosDados.getCodigo().trim().isEmpty()) {
                turma.setCodigo(novosDados.getCodigo());
            } else if (novosDados.getNomeDisciplina() != null && !novosDados.getNomeDisciplina().equals(turma.getNomeDisciplina())) {
                turma.setCodigo(gerarCodigoDinamico(novosDados.getNomeDisciplina()));
            }
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

    private boolean temConflitoHorarioProfessor(Long teacherId, String novoHorario, String semestre, Long ignoreId) {
        if (novoHorario == null || novoHorario.trim().isEmpty() || teacherId == null || semestre == null) return false;

        List<Turma> turmasProfessor = turmaRepository.findAll().stream()
                .filter(t -> teacherId.equals(t.getTeacherId()) && t.getSemestre() != null && semestre.trim().equalsIgnoreCase(t.getSemestre().trim()))
                .filter(t -> ignoreId == null || !ignoreId.equals(t.getId()))
                .toList();

        String slotNovo = extrairSlotHorario(novoHorario);
        java.util.Set<String> diasNovo = extrairSetDiasExatos(novoHorario);

        for (Turma t : turmasProfessor) {
            String slotExistente = extrairSlotHorario(t.getHorario());
            java.util.Set<String> diasExistente = extrairSetDiasExatos(t.getHorario());

            // Se o slot de horario for o mesmo (ex: MANHA_A) E houver pelo menos um dia em comum
            if (!slotNovo.isEmpty() && slotNovo.equalsIgnoreCase(slotExistente)) {
                for (String d : diasNovo) {
                    if (diasExistente.contains(d)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String extrairSlotHorario(String h) {
        if (h == null) return "";
        String upper = h.toUpperCase();
        if (upper.contains("07:30")) return "MANHA_A";
        if (upper.contains("09:20")) return "MANHA_B";
        if (upper.contains("11:10")) return "MANHA_C";
        if (upper.contains("13:30")) return "TARDE_A";
        if (upper.contains("15:20")) return "TARDE_B";
        if (upper.contains("18:30")) return "NOITE_A";
        if (upper.contains("20:20")) return "NOITE_B";
        return upper;
    }

    private java.util.Set<String> extrairSetDiasExatos(String h) {
        java.util.Set<String> dias = new java.util.HashSet<>();
        if (h == null) return dias;
        String lower = h.toLowerCase();
        // Verificar tokens exatos antes da hora
        if (lower.contains("seg")) dias.add("seg");
        if (lower.contains("ter")) dias.add("ter");
        if (lower.contains("qua")) dias.add("qua");
        if (lower.contains("qui")) dias.add("qui");
        if (lower.contains("sex")) dias.add("sex");
        return dias;
    }

    private String gerarCodigoDinamico(String nomeDisciplina) {
        if (nomeDisciplina == null || nomeDisciplina.trim().isEmpty()) {
            return "TUR-01";
        }
        // Remover acentos, converter maiusculas e remover espacos ou caracteres especiais para pegar letras limpas
        String limpo = java.text.Normalizer.normalize(nomeDisciplina, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^A-Za-z]", "")
                .toUpperCase()
                .trim();

        if (limpo.isEmpty()) {
            limpo = "TUR";
        }

        String prefix = "TUR";
        if (limpo.length() >= 3) {
            prefix = limpo.substring(0, 3);
        } else {
            prefix = limpo;
        }

        List<Turma> existing = turmaRepository.findByCodigoStartingWith(prefix);
        int nextNum = existing.size() + 1;

        String codigo;
        while (true) {
            final String candidate = String.format("%s-%02d", prefix, nextNum);
            boolean exists = turmaRepository.findAll().stream().anyMatch(t -> candidate.equalsIgnoreCase(t.getCodigo()));
            if (!exists) {
                codigo = candidate;
                break;
            }
            nextNum++;
        }

        return codigo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (turmaRepository.existsById(id)) {
            turmaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/permissoes")
    public java.util.Map<String,Object> permissions(){return access.scope();}
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    @GetMapping("/autorizacao")
    public ResponseEntity<Void> authorize(@RequestParam String turma,@RequestParam Long alunoId){return access.enrolled(turma,alunoId)?ResponseEntity.ok().build():ResponseEntity.status(403).build();}

    // --- Matrícula de Alunos em Turmas (Muitos para Muitos) ---

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<?> desmatricularAluno(@PathVariable Long turmaId, @PathVariable Long alunoId) {
        if (!matriculaTurmaRepository.existsByTurmaIdAndAlunoId(turmaId, alunoId)) {
            return ResponseEntity.notFound().build();
        }
        matriculaTurmaRepository.deleteByTurmaIdAndAlunoId(turmaId, alunoId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@turmaAccess.teacher(#turmaId)")
    @GetMapping("/{turmaId}/alunos")
    public List<MatriculaTurma> listarAlunosDaTurma(@PathVariable Long turmaId) {
        return matriculaTurmaRepository.findByTurmaId(turmaId);
    }
}
