package com.unifor.br.distrischool.alunoservice;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AlunoServiceApplication {

    @Bean
    public Queue studentQueue() {
        return new Queue("student.created", true);
    }

    public static void main(String[] args) {
        SpringApplication.run(AlunoServiceApplication.class, args);
    }

}
