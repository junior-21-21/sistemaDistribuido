-- Distingue dos momentos de la norma de evaluación (RVM 094-2020-MINEDU, mod. RVM 048-2024-MINEDU):
--   RECUPERACION: competencias con C que se recuperan en el acompañamiento al terminar el año (enero-febrero).
--   SUBSANACION : el área que el estudiante promovido lleva con C y puede subsanar hasta diciembre del año siguiente.
ALTER TABLE areas_pendientes ADD COLUMN tipo VARCHAR(15) NOT NULL DEFAULT 'SUBSANACION';
ALTER TABLE areas_pendientes ADD CONSTRAINT ck_area_tipo CHECK (tipo IN ('RECUPERACION', 'SUBSANACION'));

ALTER TABLE areas_pendientes DROP CONSTRAINT uk_area_pendiente;
ALTER TABLE areas_pendientes ADD CONSTRAINT uk_area_pendiente UNIQUE (estudiante_id, curso_nombre, anio_origen, tipo);

CREATE INDEX IF NOT EXISTS ix_areas_pendientes_tipo_estado ON areas_pendientes (tipo, estado);
