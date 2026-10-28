package com.unifor.br.distrischool.nota_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILA_MEDIA = "fila.calculo.media";

    @Bean
    public Queue filaMedia() {
        return new Queue(FILA_MEDIA, true);
    }
}