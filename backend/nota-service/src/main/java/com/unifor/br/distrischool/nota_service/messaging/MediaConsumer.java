package com.unifor.br.distrischool.nota_service.messaging;

import com.unifor.br.distrischool.nota_service.config.RabbitMQConfig;
import com.unifor.br.distrischool.nota_service.service.NotaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
public class MediaConsumer {

    private final NotaService notaService;

    public MediaConsumer(NotaService notaService) {
        this.notaService = notaService;
    }

    @RabbitListener(queues = RabbitMQConfig.FILA_MEDIA)
    public void processarCalculo(String mensagem) {

        Long alunoId = Long.parseLong(mensagem);

        Double media = notaService.calcularMedia(alunoId);

        System.out.println(
                "Média calculada para o aluno " +
                        alunoId + ": " + media
        );
    }
}