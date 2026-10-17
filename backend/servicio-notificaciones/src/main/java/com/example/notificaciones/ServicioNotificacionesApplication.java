package com.example.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServicioNotificacionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicioNotificacionesApplication.class, args);
    }
}
