package com.example.gateway.tiemporeal;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * Canal Server-Sent Events: GET /api/tiempo-real/stream con el JWT en Authorization.
 * Emite "conectado" al abrir, "cambio" por cada escritura visible para el usuario y un
 * comentario de latido cada 25 s para que proxies y navegadores mantengan la conexión.
 */
@RestController
@RequestMapping("/api/tiempo-real")
public class TiempoRealController {

    private final TiempoRealService tiempoReal;

    public TiempoRealController(TiempoRealService tiempoReal) {
        this.tiempoReal = tiempoReal;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@AuthenticationPrincipal Jwt jwt,
                                                org.springframework.security.core.Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT))
                .toList();

        ServerSentEvent<Object> saludo = ServerSentEvent.builder()
                .event("conectado")
                .data((Object) Map.of("usuario", jwt.getSubject(), "fecha", Instant.now().toString()))
                .build();

        Flux<ServerSentEvent<Object>> cambios = tiempoReal.eventosPara(roles)
                .map(evento -> ServerSentEvent.builder()
                        .id(evento.id())
                        .event("cambio")
                        .data((Object) evento)
                        .build());

        Flux<ServerSentEvent<Object>> latidos = tiempoReal.latidos()
                .map(tick -> ServerSentEvent.builder().comment("latido").build());

        return Flux.concat(Flux.just(saludo), Flux.merge(cambios, latidos))
                .doOnSubscribe(subscription -> tiempoReal.conectado())
                .doFinally(signal -> tiempoReal.desconectado());
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of("conexiones", tiempoReal.conexionesActivas(), "fecha", Instant.now().toString());
    }
}
