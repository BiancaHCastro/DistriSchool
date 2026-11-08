package com.unifor.br.distrischool.attendance_service;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AttendanceServiceApplication {

        @Bean
        public Queue attendanceQueue() {
                return new Queue("attendance.recorded", true);
        }

        public static void main(String[] args) {
                SpringApplication.run(AttendanceServiceApplication.class, args);
        }

}
