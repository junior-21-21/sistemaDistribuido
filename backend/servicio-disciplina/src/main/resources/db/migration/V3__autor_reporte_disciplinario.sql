ALTER TABLE reportes_disciplinarios
    ALTER COLUMN docente_id DROP NOT NULL;

ALTER TABLE reportes_disciplinarios
    ADD COLUMN IF NOT EXISTS registrado_por VARCHAR(100);

UPDATE reportes_disciplinarios
SET registrado_por = 'sistema'
WHERE registrado_por IS NULL OR BTRIM(registrado_por) = '';

ALTER TABLE reportes_disciplinarios
    ALTER COLUMN registrado_por SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_disc_registrado_por
    ON reportes_disciplinarios (registrado_por);
