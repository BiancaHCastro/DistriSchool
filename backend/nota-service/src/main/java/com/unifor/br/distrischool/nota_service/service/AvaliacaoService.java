package com.unifor.br.distrischool.nota_service.service;

import com.unifor.br.distrischool.nota_service.model.Avaliacao;
import com.unifor.br.distrischool.nota_service.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;


@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Avaliacao cadastrar(Avaliacao avaliacao) {
        return avaliacaoRepository.save(avaliacao);
    }

    public List<Avaliacao> listarTodas() {
        return avaliacaoRepository.findAll();
    }

    public Optional<Avaliacao> buscarPorId(Long id) {
        return avaliacaoRepository.findById(id);
    }

    public Optional<Avaliacao> atualizar(Long id, Avaliacao dados) {
        return avaliacaoRepository.findById(id).map(avaliacao -> {
            avaliacao.setTitulo(dados.getTitulo());
            avaliacao.setDescricao(dados.getDescricao());
            avaliacao.setDataAvaliacao(dados.getDataAvaliacao());
            avaliacao.setPeso(dados.getPeso());

            return avaliacaoRepository.save(avaliacao);
        });
    }

    public boolean excluir(Long id) {
        if (avaliacaoRepository.existsById(id)) {
            avaliacaoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
