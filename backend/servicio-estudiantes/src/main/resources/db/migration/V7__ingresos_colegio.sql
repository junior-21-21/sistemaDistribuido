-- Registro de ingreso al colegio (portería) escaneando el DNI del estudiante.
CREATE TABLE IF NOT EXISTS ingresos_colegio (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id),
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    turno VARCHAR(10) NOT NULL,
    estado VARCHAR(12) NOT NULL,
    metodo VARCHAR(12) NOT NULL,
    registrado_por VARCHAR(100) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ingreso_estudiante_fecha UNIQUE (estudiante_id, fecha),
    CONSTRAINT ck_ingreso_estado CHECK (estado IN ('PUNTUAL', 'TARDANZA')),
    CONSTRAINT ck_ingreso_metodo CHECK (metodo IN ('ESCANER', 'CAMARA', 'MANUAL'))
);

CREATE INDEX IF NOT EXISTS ix_ingresos_fecha ON ingresos_colegio (fecha);
