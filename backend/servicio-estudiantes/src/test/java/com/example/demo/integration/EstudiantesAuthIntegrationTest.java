package com.example.demo.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.model.Apoderado;
import com.example.demo.model.ApoderadoEstudiante;
import com.example.demo.model.Estudiante;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ApoderadoEstudianteRepository;
import com.example.demo.repository.ApoderadoRepository;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.ConsentimientoDatosRepository;
import com.example.demo.service.AsignacionAcademicaClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
class EstudiantesAuthIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired EstudianteRepository estudiantes;
    @Autowired ApoderadoRepository apoderados;
    @Autowired ApoderadoEstudianteRepository vinculos;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ConsentimientoDatosRepository consentimientos;
    @MockitoBean AsignacionAcademicaClient asignaciones;

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM ingresos_colegio");
        jdbc.update("DELETE FROM areas_pendientes");
        jdbc.update("DELETE FROM matriculas");
        consentimientos.deleteAll();
        vinculos.deleteAll();
        usuarios.deleteAll();
        apoderados.deleteAll();
        estudiantes.deleteAll();
    }

    @Test
    void loginExponeRolesMultiplesYAutorizaConCualquieraDeEllos() throws Exception {
        Usuario administrador = new Usuario();
        administrador.setUsername("multi.admin");
        administrador.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        administrador.setNombre("María");
        administrador.setApellido("Administradora");
        administrador.setEmail("multi.admin@example.test");
        administrador.setRol("DIRECTOR");
        administrador.setRoles(new LinkedHashSet<>(List.of("DIRECTOR", "DOCENTE")));
        administrador.setEstado("ACTIVO");
        usuarios.save(administrador);

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", "multi.admin", "password", "ClaveFuerte123*"));
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder(
                        "DIRECTOR", "DOCENTE")))
                .andReturn().getResponse().getContentAsString();
        JsonNode login = objectMapper.readTree(response);

        mvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + login.get("token").asText()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointInternoDevuelveContactoConOpenInViewDesactivado() throws Exception {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre("Lucía");
        estudiante.setApellido("Quispe");
        estudiante.setDni("70000001");
        estudiante.setEmail("lucia@example.test");
        estudiante = estudiantes.save(estudiante);

        Apoderado apoderado = new Apoderado();
        apoderado.setNombre("Rosa");
        apoderado.setApellido("Quispe");
        apoderado.setDni("71000001");
        apoderado.setEmail("rosa@example.test");
        apoderado.setTelefono("999111222");
        apoderado = apoderados.save(apoderado);

        ApoderadoEstudiante vinculo = new ApoderadoEstudiante();
        vinculo.setEstudiante(estudiante);
        vinculo.setApoderado(apoderado);
        vinculo.setParentesco("MADRE");
        vinculos.save(vinculo);

        mvc.perform(get("/api/internal/contactos/estudiante/{id}", estudiante.getId())
                        .header("X-Internal-Api-Key", "clave-interna-de-pruebas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("rosa@example.test"))
                .andExpect(jsonPath("$[0].telefono").value("999111222"));

        mvc.perform(get("/api/internal/contactos/estudiante/{id}", estudiante.getId())
                        .header("X-Internal-Api-Key", "incorrecta"))
                .andExpect(status().isForbidden());
    }

    @Test
    void matriculaSinCorreoYVinculaAlApoderado() throws Exception {
        Usuario secretaria = new Usuario();
        secretaria.setUsername("secretaria.prueba");
        secretaria.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        secretaria.setNombre("Sara");
        secretaria.setApellido("Secretaria");
        secretaria.setEmail("secretaria.prueba@example.test");
        secretaria.setRol("SECRETARIA");
        secretaria.setRoles(new LinkedHashSet<>(List.of("SECRETARIA")));
        secretaria.setEstado("ACTIVO");
        usuarios.save(secretaria);

        Apoderado apoderado = new Apoderado();
        apoderado.setNombre("María");
        apoderado.setApellido("Flores");
        apoderado.setDni("72000001");
        apoderado.setTelefono("987654321");
        apoderado = apoderados.save(apoderado);

        String body = """
                {
                  "estudiante": {
                    "nombre": "Junior",
                    "apellido": "Quica Flores",
                    "dni": "73000001",
                    "email": "",
                    "telefono": "",
                    "gradoId": 7,
                    "seccionId": 15,
                    "estado": "ACTIVO"
                  },
                  "apoderadoId": %d,
                  "parentesco": "MADRE",
                  "permisoRetiro": true,
                  "consentimientos": [
                    {"finalidad":"DATOS_ACADEMICOS","accion":"OTORGADO","evidencia":"ACTA-MAT-2026-001"},
                    {"finalidad":"SALUD","accion":"DENEGADO","evidencia":"ACTA-MAT-2026-001"},
                    {"finalidad":"COMUNICACIONES","accion":"OTORGADO","evidencia":"ACTA-MAT-2026-001"}
                  ]
                }
                """.formatted(apoderado.getId());

        mvc.perform(post("/api/estudiantes/matricula")
                        .header("Authorization", "Bearer " + autenticar(
                                "secretaria.prueba", "ClaveFuerte123*"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("73000001"));

        Estudiante guardado = estudiantes.findByDni("73000001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(guardado.getEmail());
        org.junit.jupiter.api.Assertions.assertNull(guardado.getTelefono());
        ApoderadoEstudiante vinculo = vinculos.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("MADRE", vinculo.getParentesco());
        org.junit.jupiter.api.Assertions.assertTrue(vinculo.getPermisoRetiro());
        org.junit.jupiter.api.Assertions.assertEquals(3, consentimientos.count());
        org.junit.jupiter.api.Assertions.assertTrue(consentimientos.findByEstudianteIdOrderByRegistradoEnDesc(
                guardado.getId()).stream().anyMatch(item -> "DENEGADO".equals(item.getAccion())));

        String bodyInvalido = body.replace("73000001", "73000002")
                .replace("\"SALUD\",\"accion\":\"DENEGADO\"", "\"SALUD\",\"accion\":\"DESCONOCIDO\"");
        mvc.perform(post("/api/estudiantes/matricula")
                        .header("Authorization", "Bearer " + autenticar(
                                "secretaria.prueba", "ClaveFuerte123*"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyInvalido))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertTrue(estudiantes.findByDni("73000002").isEmpty(),
                "La matrícula completa debe revertirse cuando un consentimiento es inválido");
        org.junit.jupiter.api.Assertions.assertEquals(3, consentimientos.count());
    }

    @Test
    void coordinacionConvivenciaPuedeConsultarDerivaciones() throws Exception {
        Usuario coordinador = new Usuario();
        coordinador.setUsername("coord.convivencia");
        coordinador.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        coordinador.setNombre("Carlos");
        coordinador.setApellido("Coordinador");
        coordinador.setEmail("convivencia@example.test");
        coordinador.setRol("RESPONSABLE_CONVIVENCIA");
        coordinador.setRoles(new LinkedHashSet<>(List.of("RESPONSABLE_CONVIVENCIA")));
        coordinador.setEstado("ACTIVO");
        usuarios.save(coordinador);

        mvc.perform(get("/api/derivaciones")
                        .header("Authorization", "Bearer " + autenticar(
                                "coord.convivencia", "ClaveFuerte123*")))
                .andExpect(status().isOk());

        Usuario apoderado = new Usuario();
        apoderado.setUsername("apoderado");
        apoderado.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        apoderado.setNombre("Rosa");
        apoderado.setApellido("Apoderada");
        apoderado.setEmail("apoderado@example.test");
        apoderado.setRol("APODERADO");
        apoderado.setRoles(new LinkedHashSet<>(List.of("APODERADO")));
        apoderado.setEstado("ACTIVO");
        usuarios.save(apoderado);

        mvc.perform(get("/api/derivaciones")
                        .header("Authorization", "Bearer " + autenticar(
                                "apoderado", "ClaveFuerte123*")))
                .andExpect(status().isForbidden());
    }

    @Test
    void mantenimientoNoPuedeConsultarEstudiantesYSecretariaSi() throws Exception {
        Usuario administrador = new Usuario();
        administrador.setUsername("admin.tecnico");
        administrador.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        administrador.setNombre("Ada");
        administrador.setApellido("Sistema");
        administrador.setEmail("admin.tecnico@example.test");
        administrador.setRol("MANTENIMIENTO");
        administrador.setRoles(new LinkedHashSet<>(List.of("MANTENIMIENTO")));
        administrador.setEstado("ACTIVO");
        usuarios.save(administrador);

        Usuario secretaria = new Usuario();
        secretaria.setUsername("secretaria.lectura");
        secretaria.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        secretaria.setNombre("Sara");
        secretaria.setApellido("Matrícula");
        secretaria.setEmail("secretaria.lectura@example.test");
        secretaria.setRol("SECRETARIA");
        secretaria.setRoles(new LinkedHashSet<>(List.of("SECRETARIA")));
        secretaria.setEstado("ACTIVO");
        usuarios.save(secretaria);

        mvc.perform(get("/api/estudiantes")
                        .header("Authorization", "Bearer " + autenticar(
                                "admin.tecnico", "ClaveFuerte123*")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/estudiantes")
                        .header("Authorization", "Bearer " + autenticar(
                                "secretaria.lectura", "ClaveFuerte123*")))
                .andExpect(status().isOk());
    }

    @Test
    void docenteSoloConsultaEstudiantesDeSusSecciones() throws Exception {
        Usuario docente = usuario("docente.alcance", "docente.alcance@example.test", "DOCENTE");
        usuarios.save(docente);
        Estudiante permitido = estudiante("74000001", "Ana", 10L);
        Estudiante ajeno = estudiante("74000002", "Bruno", 20L);
        permitido = estudiantes.save(permitido);
        ajeno = estudiantes.save(ajeno);
        when(asignaciones.seccionesPropias(anyString(), anyInt())).thenReturn(java.util.Set.of(10L));
        String token = autenticar("docente.alcance", "ClaveFuerte123*");

        mvc.perform(get("/api/estudiantes/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].dni").value("74000001"));
        mvc.perform(get("/api/estudiantes/{id}", ajeno.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/estudiantes/{id}", permitido.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void consentimientoEsInmutableVersionadoYExigeVinculo() throws Exception {
        Usuario secretaria = usuario("secretaria.consentimiento", "consent@example.test", "SECRETARIA");
        usuarios.save(secretaria);
        Estudiante estudiante = estudiantes.save(estudiante("75000001", "Lucía", 10L));
        Apoderado apoderado = new Apoderado();
        apoderado.setNombre("Rosa"); apoderado.setApellido("Quispe"); apoderado.setDni("76000001");
        apoderado = apoderados.save(apoderado);
        ApoderadoEstudiante vinculo = new ApoderadoEstudiante();
        vinculo.setEstudiante(estudiante); vinculo.setApoderado(apoderado); vinculo.setParentesco("MADRE");
        vinculos.save(vinculo);
        String token = autenticar("secretaria.consentimiento", "ClaveFuerte123*");
        String body = objectMapper.writeValueAsString(Map.of(
                "estudianteId", estudiante.getId(), "apoderadoId", apoderado.getId(),
                "finalidad", "SALUD", "accion", "OTORGADO", "evidencia", "ACTA-2026-001"));

        mvc.perform(post("/api/consentimientos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionPolitica").value("2026-01"));
        mvc.perform(get("/api/consentimientos").param("estudianteId", estudiante.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    private Usuario usuario(String username, String email, String rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username); usuario.setPassword(passwordEncoder.encode("ClaveFuerte123*"));
        usuario.setNombre("Usuario"); usuario.setApellido("Prueba"); usuario.setEmail(email);
        usuario.setRol(rol); usuario.setRoles(new LinkedHashSet<>(List.of(rol))); usuario.setEstado("ACTIVO");
        return usuario;
    }

    private Estudiante estudiante(String dni, String nombre, Long seccionId) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(nombre); estudiante.setApellido("Prueba"); estudiante.setDni(dni);
        estudiante.setSeccionId(seccionId); estudiante.setEstado("ACTIVO");
        return estudiante;
    }

    private String autenticar(String username, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", password));
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
