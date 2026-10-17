CREATE TABLE ejecuciones_reconciliacion (
    id UUID PRIMARY KEY,
    ejecutada_en TIMESTAMP NOT NULL,
    incidencias_detectadas INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL,
    detalle VARCHAR(500)
);

CREATE TABLE incidencias_integridad (
    fingerprint VARCHAR(64) PRIMARY KEY,
    origen VARCHAR(40) NOT NULL,
    entidad VARCHAR(60) NOT NULL,
    registro_id BIGINT NOT NULL,
    campo VARCHAR(60) NOT NULL,
    valor_referenciado BIGINT NOT NULL,
    problema VARCHAR(300) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    primera_deteccion TIMESTAMP NOT NULL,
    ultima_deteccion TIMESTAMP NOT NULL,
    ultima_ejecucion UUID NOT NULL REFERENCES ejecuciones_reconciliacion(id),
    resuelta_en TIMESTAMP
);

CREATE INDEX idx_integridad_estado ON incidencias_integridad(estado, ultima_deteccion DESC);
CREATE INDEX idx_integridad_origen ON incidencias_integridad(origen, entidad);
