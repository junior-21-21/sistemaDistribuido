package com.example.gateway.tiemporeal;

import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/** Publica un {@link EventoCambio} cuando una escritura enrutada termina con 2xx. */
@Component
public class CambiosGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<HttpMethod> ESCRITURAS = Set.of(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);
    private static final Set<String> IGNORADOS = Set.of("auth", "portal-apoderado", "tiempo-real");

    private final TiempoRealService tiempoReal;

    public CambiosGlobalFilter(TiempoRealService tiempoReal) {
        this.tiempoReal = tiempoReal;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String modulo = modulo(exchange.getRequest().getPath().value());
        if (!ESCRITURAS.contains(method) || modulo == null) {
            return chain.filter(exchange);
        }
        return chain.filter(exchange).then(Mono.defer(() -> {
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            if (status == null || !status.is2xxSuccessful()) {
                return Mono.empty();
            }
            return ReactiveSecurityContextHolder.getContext()
                    .map(context -> context.getAuthentication())
                    .filter(JwtAuthenticationToken.class::isInstance)
                    .map(auth -> ((JwtAuthenticationToken) auth).getToken())
                    .doOnNext(jwt -> tiempoReal.publicar(modulo, method.name(), nombre(jwt), jwt.getSubject()))
                    .switchIfEmpty(Mono.fromRunnable(() -> tiempoReal.publicar(modulo, method.name(), "Sistema", null)))
                    .then();
        }));
    }

    static String modulo(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return null;
        }
        if (path.startsWith("/api/portal-apoderado/permisos")) {
            return "permisos";
        }
        String[] partes = path.substring(5).split("/");
        if (partes.length == 0 || partes[0].isBlank() || IGNORADOS.contains(partes[0])) {
            return null;
        }
        if (path.contains("/documentos")) {
            return "documentos";
        }
        return partes[0];
    }

    private static String nombre(Jwt jwt) {
        String nombre = jwt.getClaimAsString("nombre");
        return nombre == null || nombre.isBlank() ? jwt.getSubject() : nombre.trim();
    }

    @Override
    public int getOrder() {
        // Antes del enrutamiento para envolver toda la llamada al microservicio.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
