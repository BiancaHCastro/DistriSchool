package com.unifor.br.distrischool.alunoservice.service;

import com.unifor.br.distrischool.alunoservice.model.Aluno;
import com.unifor.br.distrischool.alunoservice.repository.AlunoRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlunoService {

    private final AlunoRepository repository;
    private final RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "student.created";

    public AlunoService(AlunoRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Aluno cadastrar(Aluno aluno) {
        Aluno saved = repository.save(aluno);
        try {
            rabbitTemplate.convertAndSend(QUEUE_NAME, saved);
        } catch (Exception e) {
            System.err.println("Erro ao publicar evento no RabbitMQ: " + e.getMessage());
        }
        return saved;
    }

    public List<Aluno> listar() {
        return repository.findAll();
    }

    public Optional<Aluno> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }

    public Optional<Aluno> atualizar(Long id, Aluno novosDados) {
        return repository.findById(id).map(aluno -> {
            aluno.setMatricula(novosDados.getMatricula());
            aluno.setNome(novosDados.getNome());
            aluno.setDataNascimento(novosDados.getDataNascimento());
            aluno.setEndereco(novosDados.getEndereco());
            aluno.setContato(novosDados.getContato());
            return repository.save(aluno);
        });
    }
}
