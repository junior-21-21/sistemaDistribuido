-- Cambio de sección dentro del mismo grado y año escolar (por convivencia, turno o vacantes).
-- La asistencia y las notas ya registradas se quedan en la sección de origen como historial.
CREATE TABLE IF NOT EXISTS cambios_seccion (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id),
    anio_escolar INTEGER NOT NULL,
    seccion_origen_id BIGINT,
    seccion_destino_id BIGINT NOT NULL,
    motivo VARCHAR(300) NOT NULL,
    registrado_por VARCHAR(100) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_cambios_seccion_estudiante ON cambios_seccion (estudiante_id, fecha);
