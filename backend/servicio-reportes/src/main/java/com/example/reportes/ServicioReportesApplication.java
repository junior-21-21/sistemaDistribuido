package com.example.reportes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServicioReportesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicioReportesApplication.class, args);
    }
}
