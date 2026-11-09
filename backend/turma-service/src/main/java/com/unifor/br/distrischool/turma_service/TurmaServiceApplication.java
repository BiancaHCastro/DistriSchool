package com.unifor.br.distrischool.turma_service;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TurmaServiceApplication {

    @Bean
    public Queue scheduleQueue() {
        return new Queue("schedule.updated", true);
    }

    public static void main(String[] args) {
        SpringApplication.run(TurmaServiceApplication.class, args);
    }

}
