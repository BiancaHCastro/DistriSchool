package com.unifor.br.distrischool.nota_service.service;

import com.unifor.br.distrischool.nota_service.repository.NotaRepository;
import org.springframework.stereotype.Service;
import com.unifor.br.distrischool.nota_service.model.Nota;
import java.util.List;
import com.unifor.br.distrischool.nota_service.model.CriterioAvaliacao;
import com.unifor.br.distrischool.nota_service.model.Avaliacao;
import com.unifor.br.distrischool.nota_service.repository.AvaliacaoRepository;
import com.unifor.br.distrischool.nota_service.messaging.MediaProducer;


@Service
public class NotaService {

    private final com.unifor.br.distrischool.nota_service.Notifications notifications;

    private final NotaRepository notaRepository;

    private final AvaliacaoRepository avaliacaoRepository;

    private final MediaProducer mediaProducer;

    public NotaService(NotaRepository notaRepository,
                       AvaliacaoRepository avaliacaoRepository,
                       MediaProducer mediaProducer,
                       com.unifor.br.distrischool.nota_service.Notifications notifications) {
        this.notifications = notifications;
        this.notaRepository = notaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.mediaProducer = mediaProducer;
    }

    @org.springframework.transaction.annotation.Transactional
    public Nota cadastrar(Nota nota) {
        if(nota.getTurmaId()!=null){
            if(nota.getAlunoId()==null||nota.getTurmaId().isBlank())throw new IllegalArgumentException("Informe aluno e turma");
            for(Double value:new Double[]{nota.getNota1(),nota.getNota2(),nota.getNotaFinal()})if(value!=null&&(!Double.isFinite(value)||value<0||value>10))throw new IllegalArgumentException("Notas devem estar entre 0 e 10");
            var existing=notaRepository.findFirstByAlunoIdAndTurmaIdOrderByIdAsc(nota.getAlunoId(), nota.getTurmaId());
            Nota merged = existing.orElseGet(Nota::new);
            var previous = CriterioAvaliacao.avaliar(merged).situacao();
            merged.setAlunoId(nota.getAlunoId());
            merged.setTurmaId(nota.getTurmaId());
            if (nota.getNota1() != null) merged.setNota1(nota.getNota1());
            if (nota.getNota2() != null) merged.setNota2(nota.getNota2());
            if (nota.getNotaFinal() != null) merged.setNotaFinal(nota.getNotaFinal());
            Nota saved = notaRepository.save(merged);
            try{mediaProducer.solicitarCalculo(saved.getAlunoId());}catch(Exception e){org.slf4j.LoggerFactory.getLogger(NotaService.class).warn("Cálculo assíncrono indisponível");}
            var result = CriterioAvaliacao.avaliar(saved);
            if (previous == CriterioAvaliacao.Situacao.PENDENTE && result.situacao() != previous) {
                notifications.sendAcademic("RESULTADO_NOTA", saved.getAlunoId(), "Resultado de nota",
                        "Turma " + saved.getTurmaId() + ": " + result.situacao());
            }
            return saved;
        }


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

        try{mediaProducer.solicitarCalculo(notaSalva.getAlunoId());}catch(Exception e){org.slf4j.LoggerFactory.getLogger(NotaService.class).warn("Cálculo assíncrono indisponível");}

        return notaSalva;


    }

    public List<Nota> listarTodas() {
        return notaRepository.findAll();
    }

    public List<Nota> listarPorAluno(Long alunoId) {
        return notaRepository.findByAlunoId(alunoId);
    }

    public Double calcularMedia(Long alunoId) {
        var average = notaRepository.findByAlunoId(alunoId).stream()
                .filter(nota -> nota.getTurmaId() != null)
                .map(CriterioAvaliacao::avaliar)
                .map(CriterioAvaliacao.Resultado::mediaFinal)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

}