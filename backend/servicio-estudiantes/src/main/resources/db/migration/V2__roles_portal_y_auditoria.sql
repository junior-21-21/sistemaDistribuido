ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS apoderado_id BIGINT;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS fk_usuario_apoderado;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_apoderado
    FOREIGN KEY (apoderado_id) REFERENCES apoderados(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_apoderado
    ON usuarios(apoderado_id) WHERE apoderado_id IS NOT NULL;

ALTER TABLE revinfo ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE revinfo ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_apoderado_estudiante
    ON apoderado_estudiante(apoderado_id, estudiante_id);
CREATE INDEX IF NOT EXISTS idx_apoderado_estudiante_estudiante
    ON apoderado_estudiante(estudiante_id);
