package com.unifor.br.distrischool.nota_service.service;

import com.unifor.br.distrischool.nota_service.repository.NotaRepository;
import org.springframework.stereotype.Service;
import com.unifor.br.distrischool.nota_service.model.Nota;
import java.util.List;
import com.unifor.br.distrischool.nota_service.model.Avaliacao;
import com.unifor.br.distrischool.nota_service.repository.AvaliacaoRepository;
import com.unifor.br.distrischool.nota_service.messaging.MediaProducer;


@Service
public class NotaService {

    private final NotaRepository notaRepository;

    private final AvaliacaoRepository avaliacaoRepository;

    private final MediaProducer mediaProducer;

    public NotaService(NotaRepository notaRepository,
                       AvaliacaoRepository avaliacaoRepository,
                       MediaProducer mediaProducer) {
        this.notaRepository = notaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.mediaProducer = mediaProducer;
    }

    public Nota cadastrar(Nota nota) {

        if (nota.getValor() == null ||
                nota.getValor() < 0 ||
                nota.getValor() > 10) {

            throw new IllegalArgumentException(
                    "A nota deve estar entre 0 e 10."
            );
        }
        if (nota.getAvaliacao() == null ||
                nota.getAvaliacao().getId() == null) {

            throw new IllegalArgumentException(
                    "É necessário informar uma avaliação."
            );
        }

        Long avaliacaoId = nota.getAvaliacao().getId();

        if (nota.getAlunoId() == null || nota.getAlunoId() <= 0) {
            throw new IllegalArgumentException(
                    "É necessário informar um ID de aluno válido."
            );
        }

        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Avaliação não encontrada."
                ));

        nota.setAvaliacao(avaliacao);

        Nota notaSalva = notaRepository.save(nota);

        mediaProducer.solicitarCalculo(notaSalva.getAlunoId());

        return notaSalva;


    }

    public List<Nota> listarTodas() {
        return notaRepository.findAll();
    }

    public List<Nota> listarPorAluno(Long alunoId) {
        return notaRepository.findByAlunoId(alunoId);
    }

    public Double calcularMedia(Long alunoId) {
        List<Nota> notas = notaRepository.findByAlunoId(alunoId);

        if (notas.isEmpty()) {
            return null;
        }

        double somaPonderada = 0;
        double somaPesos = 0;

        for (Nota nota : notas) {
            double peso = nota.getAvaliacao().getPeso();

            somaPonderada += nota.getValor() * peso;
            somaPesos += peso;
        }

        if (somaPesos == 0) {
            return null;
        }

        return somaPonderada / somaPesos;
    }

}