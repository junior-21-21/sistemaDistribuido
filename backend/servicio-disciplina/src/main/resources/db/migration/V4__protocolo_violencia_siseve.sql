-- Violencia escolar (pelea, acoso, maltrato): protocolo de atención y registro en el SíseVe
-- (DS 004-2018-MINEDU). Responsable de convivencia (TOE) o Dirección completa cada paso.
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS codigo_siseve VARCHAR(30);
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS fecha_siseve DATE;
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS citacion_apoderado TIMESTAMP;
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS medidas_proteccion VARCHAR(500);
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS derivacion_externa VARCHAR(20);
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS fecha_derivacion DATE;
ALTER TABLE reportes_disciplinarios ADD COLUMN IF NOT EXISTS protocolo_actualizado_por VARCHAR(100);
ALTER TABLE reportes_disciplinarios ADD CONSTRAINT ck_disc_derivacion_externa CHECK (
    derivacion_externa IS NULL OR derivacion_externa IN
    ('PSICOLOGIA', 'DEMUNA', 'CEM', 'POLICIA', 'FISCALIA', 'CENTRO_SALUD'));
CREATE INDEX IF NOT EXISTS idx_disc_tipo ON reportes_disciplinarios (tipo);
