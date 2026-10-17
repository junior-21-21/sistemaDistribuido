-- Organización de un colegio público según MINEDU (RM 432-2020, RM 010-2026, DS 004-2018):
--   * SUBDIRECTOR: puesto base para lo pedagógico de su nivel.
--   * PORTERO: personal de servicio y guardianía (módulo opcional PORTERIA).
--   * Justificaciones, papeletas de salida y permisos de los estudiantes.
--   * Nómina de matrícula por sección aprobada por Dirección.
--   * Tópico: aviso al apoderado y traslado a un establecimiento de salud.

ALTER TABLE usuario_roles DROP CONSTRAINT IF EXISTS ck_usuario_rol;
ALTER TABLE usuario_roles ADD CONSTRAINT ck_usuario_rol CHECK (rol IN (
    'DIRECTOR', 'SUBDIRECTOR', 'DOCENTE', 'AUXILIAR', 'SECRETARIA', 'APODERADO',
    'PSICOLOGO', 'ENFERMERA', 'MANTENIMIENTO', 'PORTERO',
    'RESPONSABLE_CONVIVENCIA', 'PRIMEROS_AUXILIOS'));

ALTER TABLE modulos_institucion DROP CONSTRAINT IF EXISTS ck_modulo_codigo;
ALTER TABLE modulos_institucion ADD CONSTRAINT ck_modulo_codigo
    CHECK (codigo IN ('PSICOLOGIA', 'ENFERMERIA', 'MANTENIMIENTO', 'PORTERIA'));
INSERT INTO modulos_institucion (codigo, habilitado) VALUES ('PORTERIA', FALSE)
ON CONFLICT (codigo) DO NOTHING;

-- El apoderado acude al auxiliar o al tutor; lo formal (varios días) pasa por Secretaría y lo aprueba Dirección.
CREATE TABLE IF NOT EXISTS solicitudes_permiso (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id),
    tipo VARCHAR(25) NOT NULL,
    fecha_desde DATE NOT NULL,
    fecha_hasta DATE NOT NULL,
    hora_salida TIME,
    motivo VARCHAR(400) NOT NULL,
    persona_recoge VARCHAR(150),
    dni_persona_recoge VARCHAR(8),
    origen VARCHAR(12) NOT NULL,
    solicitado_por VARCHAR(100) NOT NULL,
    estado VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE',
    revisado_por VARCHAR(100),
    fecha_revision TIMESTAMP,
    observacion_revision VARCHAR(300),
    asistencias_justificadas INTEGER NOT NULL DEFAULT 0,
    salida_registrada_por VARCHAR(100),
    fecha_salida TIMESTAMP,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_permiso_tipo CHECK (tipo IN ('JUSTIFICACION', 'PAPELETA_SALIDA', 'PERMISO')),
    CONSTRAINT ck_permiso_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_permiso_origen CHECK (origen IN ('APODERADO', 'PERSONAL')),
    CONSTRAINT ck_permiso_fechas CHECK (fecha_hasta >= fecha_desde)
);
CREATE INDEX IF NOT EXISTS ix_permisos_estado ON solicitudes_permiso (estado, creado_en);
CREATE INDEX IF NOT EXISTS ix_permisos_estudiante ON solicitudes_permiso (estudiante_id, fecha_desde);

-- El director aprueba y firma la nómina al concluir la matrícula (RM 432-2020-MINEDU).
CREATE TABLE IF NOT EXISTS nominas_matricula (
    id BIGSERIAL PRIMARY KEY,
    seccion_id BIGINT NOT NULL,
    anio_escolar INTEGER NOT NULL,
    total_estudiantes INTEGER NOT NULL,
    aprobada_por VARCHAR(100) NOT NULL,
    fecha_aprobacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion VARCHAR(300),
    CONSTRAINT uk_nomina_seccion_anio UNIQUE (seccion_id, anio_escolar)
);

-- Tópico: se llama al apoderado y, si es grave, se traslada al establecimiento de salud (SIS).
ALTER TABLE atenciones_medicas ADD COLUMN IF NOT EXISTS apoderado_avisado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE atenciones_medicas ADD COLUMN IF NOT EXISTS destino VARCHAR(25) NOT NULL DEFAULT 'RETORNA_AULA';
ALTER TABLE atenciones_medicas ADD COLUMN IF NOT EXISTS establecimiento_salud VARCHAR(150);
ALTER TABLE atenciones_medicas ADD COLUMN IF NOT EXISTS cubierto_sis BOOLEAN;
ALTER TABLE atenciones_medicas ADD CONSTRAINT ck_atencion_destino
    CHECK (destino IN ('RETORNA_AULA', 'RETIRADO_APODERADO', 'TRASLADO_SALUD'));
