package com.example.demo.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AsignacionAcademicaClient;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Reglas del colegio: roles base, encargos para docentes/auxiliares y módulos opcionales. */
@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres",
        "app.internal-api-key=clave-interna-de-pruebas",
        "app.seed-demo-data=false",
        "app.bootstrap-admin.password=",
        "app.bootstrap-system-admin.password=",
        "notifications.enabled=false"
})
@AutoConfigureMockMvc
class RolesEncargosIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AsignacionAcademicaClient asignaciones;

    private String director;

    @BeforeEach
    void preparar() throws Exception {
        usuarios.deleteAll();
        jdbc.update("UPDATE modulos_institucion SET habilitado = false");
        Usuario usuario = new Usuario();
        usuario.setUsername("directora"); usuario.setPassword(encoder.encode("ClaveFuerte123*"));
        usuario.setNombre("Rosa"); usuario.setApellido("Quispe"); usuario.setEmail("directora@colegio.pe");
        usuario.setRol("DIRECTOR"); usuario.setRoles(new LinkedHashSet<>(List.of("DIRECTOR")));
        usuario.setEstado("ACTIVO");
        usuarios.save(usuario);
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"directora\",\"password\":\"ClaveFuerte123*\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        director = "Bearer " + json.readTree(respuesta).get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions crear(String username, String... roles) throws Exception {
        String cuerpo = json.writeValueAsString(Map.of(
                "username", username, "clave", "ClaveTemporal1*", "nombre", "Ana", "apellido", "Prueba",
                "email", username + "@colegio.pe", "roles", List.of(roles)));
        return mvc.perform(post("/api/usuarios").header("Authorization", director)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void losEncargosSoloSeDanADocentesOAuxiliares() throws Exception {
        crear("solo.encargo", "RESPONSABLE_CONVIVENCIA").andExpect(status().isBadRequest());
        crear("secretaria.conv", "SECRETARIA", "RESPONSABLE_CONVIVENCIA").andExpect(status().isBadRequest());
        crear("auxiliar.conv", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("AUXILIAR"));
        crear("docente.topico", "DOCENTE", "PRIMEROS_AUXILIOS").andExpect(status().isCreated());
        crear("rol.antiguo", "COORDINADOR_ACADEMICO").andExpect(status().isBadRequest());
    }

    @Test
    void losRolesOpcionalesExigenQueDireccionHabiliteSuModulo() throws Exception {
        crear("psicologa", "PSICOLOGO").andExpect(status().isBadRequest());

        mvc.perform(put("/api/institucion/modulos/PSICOLOGIA").header("Authorization", director)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"habilitado\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.PSICOLOGIA").value(true));
        crear("psicologa", "PSICOLOGO").andExpect(status().isCreated());

        // No se puede deshabilitar mientras haya personal activo con ese rol
        mvc.perform(put("/api/institucion/modulos/PSICOLOGIA").header("Authorization", director)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"habilitado\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/institucion/modulos").header("Authorization", director))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ENFERMERIA").value(false));
    }

    @Test
    void direccionAsignaYQuitaEncargosSinQuitarseASiMisma() throws Exception {
        String creado = crear("auxiliar.dos", "AUXILIAR").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creado).get("id").asLong();
        mvc.perform(put("/api/usuarios/{id}/roles", id).header("Authorization", director)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"AUXILIAR\",\"RESPONSABLE_CONVIVENCIA\",\"PRIMEROS_AUXILIOS\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(3));

        long propio = usuarios.findByUsername("directora").orElseThrow().getId();
        mvc.perform(put("/api/usuarios/{id}/roles", propio).header("Authorization", director)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"DOCENTE\"]}"))
                .andExpect(status().isBadRequest());
    }
}
