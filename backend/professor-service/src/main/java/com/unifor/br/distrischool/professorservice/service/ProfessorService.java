package com.unifor.br.distrischool.professorservice.service;

import com.unifor.br.distrischool.professorservice.model.Professor;
import com.unifor.br.distrischool.professorservice.repository.ProfessorRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfessorService {

    private final ProfessorRepository repository;
    private final RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "teacher.assigned";

    public ProfessorService(ProfessorRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Professor cadastrar(Professor professor) {
        if (repository.existsByEmail(professor.getEmail())) {
            throw new IllegalArgumentException("Já existe um professor cadastrado com esse e-mail.");
        }
        Professor saved = repository.save(professor);
        try {
            rabbitTemplate.convertAndSend(QUEUE_NAME, saved);
        } catch (Exception e) {
            System.err.println("Erro ao publicar evento no RabbitMQ para professor: " + e.getMessage());
        }
        return saved;
    }

    public List<Professor> listar() {
        return repository.findAll();
    }

    public Optional<Professor> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Optional<Professor> atualizar(Long id, Professor novosDados) {
        return repository.findById(id).map(professor -> {
            professor.setNome(novosDados.getNome());
            professor.setEmail(novosDados.getEmail());
            professor.setTelefone(novosDados.getTelefone());
            professor.setEspecialidade(novosDados.getEspecialidade());
            return repository.save(professor);
        });
    }

    public boolean excluir(Long id) {
        return repository.findById(id).map(professor -> {
            repository.delete(professor);
            return true;
        }).orElse(false);
    }
}
