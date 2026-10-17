package com.example.notificaciones.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.notificaciones.repository.NotificacionRepository;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres",
        "app.internal-api-key=clave-interna-de-pruebas",
        "notifications.mode=SIMULATED",
        "notifications.retry-scan-ms=600000",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
class NotificacionesIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired NotificacionRepository repository;
    @MockBean RabbitTemplate rabbitTemplate;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void registraEventoDeFormaIdempotenteYPermiteConsultarlo() throws Exception {
        String body = """
                {"eventoId":"ASISTENCIA-20-FALTA","tipo":"INASISTENCIA","estudianteId":20,
                 "asunto":"Alerta de asistencia","mensaje":"Se registró una inasistencia",
                 "canales":["EMAIL","SMS"]}
                """;

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/internal/notificaciones")
                            .header("X-Internal-Api-Key", "clave-interna-de-pruebas")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        org.junit.jupiter.api.Assertions.assertEquals(2, repository.count());
        mvc.perform(get("/api/notificaciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void protegeConsultaPorRolYValidaClaveInterna() throws Exception {
        mvc.perform(get("/api/notificaciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/notificaciones"))
                .andExpect(status().isUnauthorized());

        String body = """
                {"eventoId":"DISCIPLINA-1","tipo":"DISCIPLINA","estudianteId":1,
                 "asunto":"Aviso","mensaje":"Mensaje","canales":["EMAIL"]}
                """;
        mvc.perform(post("/api/internal/notificaciones")
                        .header("X-Internal-Api-Key", "incorrecta")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorPuedeCertificarCanalYDocenteNo() throws Exception {
        String body = "{\"canal\":\"EMAIL\",\"destinatario\":\"certificacion@example.test\"}";
        mvc.perform(post("/api/notificaciones/proveedores/certificar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("SIMULATED"))
                .andExpect(jsonPath("$.resultado").value("SIMULATED"));
        mvc.perform(post("/api/notificaciones/proveedores/certificar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}
