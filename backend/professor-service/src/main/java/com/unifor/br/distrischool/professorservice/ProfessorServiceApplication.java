package com.unifor.br.distrischool.professorservice;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProfessorServiceApplication {

    @Bean
    public Queue teacherQueue() {
        return new Queue("teacher.assigned", true);
    }

    public static void main(String[] args) {
        SpringApplication.run(ProfessorServiceApplication.class, args);
    }

}
