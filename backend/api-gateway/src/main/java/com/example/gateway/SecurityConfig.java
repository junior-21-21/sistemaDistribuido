package com.example.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, exception) -> jsonError(
                                exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Autenticación requerida"))
                        .accessDeniedHandler((exchange, exception) -> jsonError(
                                exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Permisos insuficientes")))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .pathMatchers("/api/portal-apoderado/**").hasRole("APODERADO")
                        .pathMatchers("/api/auditoria/**").hasRole("DIRECTOR")
                        .pathMatchers("/api/usuarios/**").hasRole("DIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/institucion/**").authenticated()
                        .pathMatchers("/api/institucion/**").hasRole("DIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/apoderados/**").hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")
                        .pathMatchers("/api/apoderados/**").hasRole("SECRETARIA")
                        .pathMatchers("/api/estudiantes/*/documentos/**").hasAnyRole("SECRETARIA", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers("/api/notificaciones/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers("/api/integridad/**").hasRole("DIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/asignaciones-academicas/**", "/api/horarios/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers("/api/asignaciones-academicas/**", "/api/horarios/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.POST, "/api/ingresos")
                            .hasAnyRole("AUXILIAR", "RESPONSABLE_CONVIVENCIA", "PORTERO")
                        .pathMatchers(HttpMethod.GET, "/api/ingresos/**")
                            .hasAnyRole("AUXILIAR", "RESPONSABLE_CONVIVENCIA", "SECRETARIA", "DIRECTOR", "SUBDIRECTOR", "DOCENTE", "PORTERO")
                        .pathMatchers("/api/ingresos/**").denyAll()
                        // Justificaciones, papeletas de salida y permisos: el servicio valida quién aprueba qué.
                        .pathMatchers("/api/permisos/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR",
                                    "RESPONSABLE_CONVIVENCIA", "PORTERO")
                        // Nómina de matrícula: Secretaría la prepara, Dirección la aprueba.
                        .pathMatchers(HttpMethod.GET, "/api/nominas/**").hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")
                        .pathMatchers("/api/nominas/**").hasRole("DIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/recuperacion/**")
                            .hasAnyRole("SECRETARIA", "DIRECTOR", "SUBDIRECTOR", "DOCENTE")
                        .pathMatchers("/api/recuperacion/**").hasAnyRole("SECRETARIA", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers("/api/fichas-medicas/**", "/api/atenciones-medicas/**")
                            .hasAnyRole("ENFERMERA", "PRIMEROS_AUXILIOS")
                        .pathMatchers(HttpMethod.GET, "/api/estudiantes/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "PSICOLOGO", "ENFERMERA", "PRIMEROS_AUXILIOS", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        // Cambio de sección dentro del grado: también Subdirección (organiza las secciones).
                        .pathMatchers(HttpMethod.POST, "/api/estudiantes/*/cambio-seccion")
                            .hasAnyRole("SECRETARIA", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers("/api/estudiantes/**").hasAnyRole("SECRETARIA", "DIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/consentimientos/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")
                        .pathMatchers("/api/consentimientos/**").hasRole("SECRETARIA")
                        .pathMatchers(HttpMethod.GET, "/api/notas/estudiante/**")
                            .hasAnyRole("DOCENTE", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/aulas/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA", "MANTENIMIENTO")
                        // Portería solo necesita el nombre de cada sección para mostrar ingresos y salidas.
                        .pathMatchers(HttpMethod.GET, "/api/secciones")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA", "PORTERO")
                        .pathMatchers(HttpMethod.GET, "/api/cursos/**", "/api/docentes/**", "/api/secciones/**",
                                "/api/grados/**", "/api/niveles/**", "/api/periodos/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers(HttpMethod.GET, "/api/notas/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE")
                        .pathMatchers("/api/notas/**").hasRole("DOCENTE")
                        .pathMatchers(HttpMethod.GET, "/api/bimestres/*/*/auditoria")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/bimestres/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "DOCENTE")
                        .pathMatchers(HttpMethod.POST, "/api/bimestres/*/*/cerrar")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.POST, "/api/bimestres/*/*/reabrir")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.GET, "/api/asistencias/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers("/api/asistencias/**")
                            .hasAnyRole("DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers(HttpMethod.GET, "/api/reportes-disciplinarios/**", "/api/disciplina/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers(HttpMethod.PATCH, "/api/reportes-disciplinarios/*/protocolo")
                            .hasAnyRole("RESPONSABLE_CONVIVENCIA", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers("/api/reportes-disciplinarios/**", "/api/disciplina/**")
                            .hasAnyRole("DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers(HttpMethod.PUT, "/api/tickets/*/estado").hasAnyRole("MANTENIMIENTO", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.POST, "/api/tickets").hasRole("DOCENTE")
                        .pathMatchers(HttpMethod.GET, "/api/tickets/**")
                            .hasAnyRole("DOCENTE", "MANTENIMIENTO", "DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers("/api/derivaciones/**")
                            .hasAnyRole("DOCENTE", "PSICOLOGO", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers(HttpMethod.GET, "/api/estadisticas/dashboard")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers(HttpMethod.POST, "/api/actividades/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "RESPONSABLE_CONVIVENCIA")
                        .pathMatchers("/api/cursos/**", "/api/horarios/**", "/api/docentes/**", "/api/aulas/**",
                                "/api/niveles/**", "/api/grados/**", "/api/secciones/**", "/api/periodos/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR")
                        .pathMatchers("/api/reportes/**", "/api/alertas/**", "/api/estadisticas/**", "/api/actividades/**")
                            .hasAnyRole("DIRECTOR", "SUBDIRECTOR", "RESPONSABLE_CONVIVENCIA")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint((exchange, exception) -> jsonError(
                                exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Token inválido o ausente"))
                        .accessDeniedHandler((exchange, exception) -> jsonError(
                                exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Permisos insuficientes"))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = paddedKey(secret);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromRole);
        return converter;
    }

    private Flux<GrantedAuthority> authoritiesFromRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            String legacyRole = jwt.getClaimAsString("rol");
            roles = legacyRole == null || legacyRole.isBlank() ? List.of() : List.of(legacyRole);
        }
        Collection<GrantedAuthority> authorities = roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                        "ROLE_" + role.toUpperCase(Locale.ROOT)))
                .toList();
        return Flux.fromIterable(authorities);
    }

    private byte[] paddedKey(String secret) {
        byte[] original = secret.getBytes(StandardCharsets.UTF_8);
        if (original.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
        return original;
    }

    private Mono<Void> jsonError(ServerWebExchange exchange, HttpStatus status,
                                 String error, String message) {
        byte[] body = ("{\"status\":" + status.value() + ",\"error\":\"" + error
                + "\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body)));
    }
}


