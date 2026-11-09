package com.unifor.br.distrischool.turma_service.producer;

import com.unifor.br.distrischool.turma_service.model.Horario;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class HorarioEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public HorarioEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void notificarHorarioAtualizado(Horario horario) {
        // Envia o evento de horário atualizado para o RabbitMQ
        rabbitTemplate.convertAndSend("schedule.updated", horario);
    }
}
