package com.example.notificaciones.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.example.notificaciones.service.ProveedorNotificacion;

@Component("proveedorNotificaciones")
public class ProveedorHealthIndicator implements HealthIndicator {
    private final ProveedorNotificacion proveedor;

    public ProveedorHealthIndicator(ProveedorNotificacion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("modo", proveedor.modo())
                .withDetail("canalesConfigurados", proveedor.canalesConfigurados())
                .build();
    }
}
