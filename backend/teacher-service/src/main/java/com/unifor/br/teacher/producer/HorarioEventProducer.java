package com.unifor.br.teacher.producer;

import com.unifor.br.teacher.model.Horario;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class HorarioEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public HorarioEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void notificarHorarioAtualizado(Horario horario) {
        // Tópico definido nos requisitos do projeto
        kafkaTemplate.send("schedule.updated", horario);
    }
}