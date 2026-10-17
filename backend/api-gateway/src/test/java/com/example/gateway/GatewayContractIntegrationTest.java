package com.example.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres")
class GatewayContractIntegrationTest {
    static final String SECRET = "clave-de-pruebas-con-mas-de-32-caracteres";
    static final HttpServer downstream = iniciarServicio();

    @DynamicPropertySource
    static void rutas(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gateway.routes[0].id", () -> "contrato-cursos");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:" + downstream.getAddress().getPort());
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/**");
    }

    @Autowired WebTestClient client;

    @Test
    void conservaContratoHttpJsonYAplicaRol() {
        client.get().uri("/api/cursos/42").headers(headers -> headers.setBearerAuth(token("SECRETARIA"))).exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectHeader().valueEquals("X-Servicio", "cursos")
                .expectHeader().exists("X-Correlation-Id")
                .expectBody().json("{\"id\":42,\"nombre\":\"Matemática\"}");

        client.get().uri("/api/cursos/42")
                .header("X-Correlation-Id", "contrato-forbidden")
                .headers(headers -> headers.setBearerAuth(token("ENFERMERA")))
                .exchange().expectStatus().isForbidden()
                .expectHeader().contentType("application/json")
                .expectHeader().valueEquals("X-Correlation-Id", "contrato-forbidden")
                .expectBody().jsonPath("$.error").isEqualTo("FORBIDDEN");
        client.get().uri("/api/cursos/42")
                .header("X-Correlation-Id", "contrato-unauthorized")
                .exchange().expectStatus().isUnauthorized()
                .expectHeader().contentType("application/json")
                .expectHeader().valueEquals("X-Correlation-Id", "contrato-unauthorized")
                .expectBody().jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void separaGobiernoAdministracionYOperacion() {
        client.post().uri("/api/usuarios").headers(h -> h.setBearerAuth(token("DOCENTE")))
                .exchange().expectStatus().isForbidden();
        client.post().uri("/api/usuarios").headers(h -> h.setBearerAuth(token("DIRECTOR")))
                .exchange().expectStatus().isOk();
        client.get().uri("/api/estudiantes/page").headers(h -> h.setBearerAuth(token("MANTENIMIENTO")))
                .exchange().expectStatus().isForbidden();
        client.get().uri("/api/estudiantes/page").headers(h -> h.setBearerAuth(token("SECRETARIA")))
                .exchange().expectStatus().isOk();
        // Secretaría consulta grados y secciones para matricular, pero el catálogo lo cambia Dirección
        client.get().uri("/api/grados").headers(h -> h.setBearerAuth(token("SECRETARIA")))
                .exchange().expectStatus().isOk();
        client.post().uri("/api/grados").headers(h -> h.setBearerAuth(token("SECRETARIA")))
                .exchange().expectStatus().isForbidden();
        client.post().uri("/api/grados").headers(h -> h.setBearerAuth(token("DIRECTOR")))
                .exchange().expectStatus().isOk();

        client.get().uri("/api/auditoria?entidad=ESTUDIANTE")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange().expectStatus().isOk();
        client.get().uri("/api/auditoria?entidad=ESTUDIANTE")
                .headers(h -> h.setBearerAuth(token("DOCENTE"))).exchange().expectStatus().isForbidden();

        client.get().uri("/api/portal-apoderado/estudiantes")
                .headers(h -> h.setBearerAuth(token("APODERADO"))).exchange().expectStatus().isOk();
        client.get().uri("/api/portal-apoderado/estudiantes")
                .headers(h -> h.setBearerAuth(token("SECRETARIA"))).exchange().expectStatus().isForbidden();

        client.get().uri("/api/asistencias?seccionId=1")
                .headers(h -> h.setBearerAuth(token("AUXILIAR"))).exchange().expectStatus().isOk();
        client.get().uri("/api/asistencias?seccionId=1")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange().expectStatus().isOk();
        client.post().uri("/api/asistencias")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange().expectStatus().isForbidden();

        client.get().uri("/api/integridad/ejecuciones")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange().expectStatus().isOk();
        client.get().uri("/api/integridad/ejecuciones")
                .headers(h -> h.setBearerAuth(token("SECRETARIA"))).exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/notificaciones")
                .headers(h -> h.setBearerAuth(token("RESPONSABLE_CONVIVENCIA"))).exchange()
                .expectStatus().isOk();
        client.get().uri("/api/notificaciones")
                .headers(h -> h.setBearerAuth(token("DOCENTE"))).exchange().expectStatus().isForbidden();

        client.get().uri("/api/notas/page")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange()
                .expectStatus().isOk();
        client.get().uri("/api/notas/page")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange()
                .expectStatus().isOk();
        client.get().uri("/api/notas/page")
                .headers(h -> h.setBearerAuth(token("AUXILIAR"))).exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/asignaciones-academicas/mias?anioEscolar=2026")
                .headers(h -> h.setBearerAuth(token("DOCENTE"))).exchange().expectStatus().isOk();
        client.post().uri("/api/asignaciones-academicas")
                .headers(h -> h.setBearerAuth(token("DOCENTE"))).exchange().expectStatus().isForbidden();
        client.post().uri("/api/asignaciones-academicas")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange()
                .expectStatus().isOk();

        client.put().uri("/api/tickets/1/estado")
                .headers(h -> h.setBearerAuth(token("MANTENIMIENTO"))).exchange()
                .expectStatus().isOk();
        client.put().uri("/api/tickets/1/estado")
                .headers(h -> h.setBearerAuth(token("DOCENTE"))).exchange()
                .expectStatus().isForbidden();
        client.get().uri("/api/tickets")
                .headers(h -> h.setBearerAuth(token("AUXILIAR"))).exchange()
                .expectStatus().isForbidden();
        client.get().uri("/api/aulas")
                .headers(h -> h.setBearerAuth(token("MANTENIMIENTO"))).exchange()
                .expectStatus().isOk();

        client.post().uri("/api/bimestres/2026/I/cerrar")
                .headers(h -> h.setBearerAuth(token("DIRECTOR")))
                .exchange().expectStatus().isOk();
        client.get().uri("/api/bimestres?anio=2026")
                .headers(h -> h.setBearerAuth(token("DOCENTE")))
                .exchange().expectStatus().isOk();
        client.get().uri("/api/bimestres/2026/I/auditoria")
                .headers(h -> h.setBearerAuth(token("DOCENTE")))
                .exchange().expectStatus().isForbidden();
        client.post().uri("/api/bimestres/2026/I/reabrir")
                .headers(h -> h.setBearerAuth(token("DOCENTE")))
                .exchange().expectStatus().isForbidden();
        client.post().uri("/api/bimestres/2026/I/reabrir")
                .headers(h -> h.setBearerAuth(token("DIRECTOR")))
                .exchange().expectStatus().isOk();

        client.get().uri("/api/consentimientos?estudianteId=1")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange().expectStatus().isOk();
        client.post().uri("/api/consentimientos")
                .headers(h -> h.setBearerAuth(token("DIRECTOR"))).exchange().expectStatus().isForbidden();
        client.post().uri("/api/consentimientos")
                .headers(h -> h.setBearerAuth(token("SECRETARIA"))).exchange().expectStatus().isOk();
    }

    @Test
    void autorizaConRolesMultiplesAunqueElRolPrincipalNoSeaSuficiente() {
        client.post().uri("/api/usuarios")
                .headers(h -> h.setBearerAuth(token("DOCENTE", List.of("DOCENTE", "DIRECTOR"))))
                .exchange().expectStatus().isOk();
    }

    @AfterAll static void detener() { downstream.stop(0); }

    private static HttpServer iniciarServicio() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/cursos/42", exchange -> {
                byte[] body = "{\"id\":42,\"nombre\":\"Matemática\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("X-Servicio", "cursos");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.createContext("/api/", exchange -> {
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo iniciar el servicio HTTP de contrato", e);
        }
    }

    private static String token(String rol) {
        return token(rol, List.of());
    }

    private static String token(String rol, List<String> roles) {
        try {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject("integration-user").claim("rol", rol)
                    .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 60_000));
            if (!roles.isEmpty()) builder.claim("roles", roles);
            JWTClaimsSet claims = builder.build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
