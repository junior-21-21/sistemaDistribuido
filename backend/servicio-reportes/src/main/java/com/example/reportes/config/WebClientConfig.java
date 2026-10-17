package com.example.reportes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServletBearerExchangeFilterFunction;

@Configuration
public class WebClientConfig {

    @Bean(name = "estudiantesClient")
    public WebClient estudiantesWebClient(@Value("${servicio.estudiantes.url}") String url) {
        return authenticatedClient(url);
    }

    @Bean(name = "cursosClient")
    public WebClient cursosWebClient(@Value("${servicio.cursos.url}") String url) {
        return authenticatedClient(url);
    }

    @Bean(name = "evaluacionesClient")
    public WebClient evaluacionesWebClient(@Value("${servicio.evaluaciones.url}") String url) {
        return authenticatedClient(url);
    }

    @Bean(name = "disciplinaClient")
    public WebClient disciplinaWebClient(@Value("${servicio.disciplina.url}") String url) {
        return authenticatedClient(url);
    }

    private WebClient authenticatedClient(String url) {
        return WebClient.builder()
                .baseUrl(url)
                .filter(new ServletBearerExchangeFilterFunction())
                .build();
    }
}
