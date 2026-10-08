package com.distrischool.alunoservice.service;

import com.distrischool.alunoservice.model.Aluno;
import com.distrischool.alunoservice.repository.AlunoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlunoService {

    private final AlunoRepository repository;

    public AlunoService(AlunoRepository repository) {
        this.repository = repository;
    }

    public Aluno cadastrar(Aluno aluno) {
        return repository.save(aluno);
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