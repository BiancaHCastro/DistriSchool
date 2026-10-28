package com.unifor.br.distrischool.nota_service.messaging;

import com.unifor.br.distrischool.nota_service.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MediaProducer {

    private final RabbitTemplate rabbitTemplate;

    public MediaProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void solicitarCalculo(Long alunoId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FILA_MEDIA,
                alunoId.toString()
        );
    }
}