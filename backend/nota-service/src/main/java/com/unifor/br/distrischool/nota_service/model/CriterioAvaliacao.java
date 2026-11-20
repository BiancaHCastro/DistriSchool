package com.unifor.br.distrischool.nota_service.model;

public final class CriterioAvaliacao {
    private CriterioAvaliacao() {}

    public enum Situacao { APROVADO, EM_AVF, REPROVADO, PENDENTE }

    public record Resultado(String turmaId, Double nota1, Double nota2, Double notaFinal,
                            Double media, Double mediaFinal, Situacao situacao) {}

    public static Resultado avaliar(Nota nota) {
        Double av1 = nota.getNota1();
        Double av2 = nota.getNota2();
        Double avf = nota.getNotaFinal();
        Double media = null;
        Double mediaFinal = null;
        Situacao situacao = Situacao.PENDENTE;
        if (av1 != null && av2 != null) {
            media = (av1 + av2) / 2.0;
            if (media < 4.0 || media >= 7.0) {
                mediaFinal = media;
                situacao = media >= 7.0 ? Situacao.APROVADO : Situacao.REPROVADO;
            } else if (avf == null) {
                situacao = Situacao.EM_AVF;
            } else {
                mediaFinal = (media + avf) / 2.0;
                situacao = mediaFinal >= 5.0 ? Situacao.APROVADO : Situacao.REPROVADO;
            }
        }
        return new Resultado(nota.getTurmaId(), av1, av2, avf, media, mediaFinal, situacao);
    }
}
