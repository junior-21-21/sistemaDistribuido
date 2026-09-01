package com.example.reportes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "estudiantesClient")
    public WebClient estudiantesWebClient(@Value("${servicio.estudiantes.url}") String url) {
        return WebClient.builder().baseUrl(url).build();
    }

    @Bean(name = "evaluacionesClient")
    public WebClient evaluacionesWebClient(@Value("${servicio.evaluaciones.url}") String url) {
        return WebClient.builder().baseUrl(url).build();
    }

    @Bean(name = "disciplinaClient")
    public WebClient disciplinaWebClient(@Value("${servicio.disciplina.url}") String url) {
        return WebClient.builder().baseUrl(url).build();
    }
}
