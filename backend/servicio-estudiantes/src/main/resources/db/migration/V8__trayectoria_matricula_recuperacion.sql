-- Trayectoria del estudiante como en SIAGIE: una matrícula por año escolar con su tipo
-- (nuevo ingreso, continuidad o traslado), procedencia y situación final.
CREATE TABLE IF NOT EXISTS matriculas (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id),
    anio_escolar INTEGER NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    grado_id BIGINT,
    seccion_id BIGINT,
    ie_procedencia VARCHAR(150),
    codigo_modular_procedencia VARCHAR(12),
    situacion_final VARCHAR(20),
    ie_destino VARCHAR(150),
    codigo_modular_destino VARCHAR(12),
    fecha_traslado DATE,
    motivo_traslado VARCHAR(300),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registrado_por VARCHAR(100) NOT NULL,
    CONSTRAINT uk_matricula_estudiante_anio UNIQUE (estudiante_id, anio_escolar),
    CONSTRAINT ck_matricula_tipo CHECK (tipo IN ('NUEVO_INGRESO', 'CONTINUIDAD', 'TRASLADO_ENTRANTE')),
    CONSTRAINT ck_matricula_situacion CHECK (situacion_final IS NULL OR situacion_final IN
        ('PROMOVIDO', 'PERMANECE', 'RECUPERACION', 'EGRESADO', 'TRASLADADO', 'RETIRADO'))
);

-- Áreas que el estudiante debe subsanar (recuperación pedagógica) aunque haya pasado de grado.
CREATE TABLE IF NOT EXISTS areas_pendientes (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id),
    curso_nombre VARCHAR(150) NOT NULL,
    grado_origen VARCHAR(60) NOT NULL,
    anio_origen INTEGER NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    calificacion VARCHAR(2),
    observacion VARCHAR(300),
    fecha_resultado DATE,
    registrado_por VARCHAR(100) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_area_pendiente UNIQUE (estudiante_id, curso_nombre, anio_origen),
    CONSTRAINT ck_area_estado CHECK (estado IN ('PENDIENTE', 'SUBSANADA', 'NO_SUBSANADA')),
    CONSTRAINT ck_area_calificacion CHECK (calificacion IS NULL OR calificacion IN ('AD', 'A', 'B', 'C'))
);

CREATE INDEX IF NOT EXISTS ix_areas_pendientes_estado ON areas_pendientes (estado);

-- Los estudiantes existentes quedan con su matrícula del año en curso como continuidad.
INSERT INTO matriculas (estudiante_id, anio_escolar, tipo, grado_id, seccion_id, registrado_por)
SELECT e.id, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 'CONTINUIDAD', e.grado_id, e.seccion_id, 'migracion'
FROM estudiantes e
WHERE e.estado = 'ACTIVO'
ON CONFLICT (estudiante_id, anio_escolar) DO NOTHING;
