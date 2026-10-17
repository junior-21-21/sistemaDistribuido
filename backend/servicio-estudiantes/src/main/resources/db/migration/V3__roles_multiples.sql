CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, rol)
);

INSERT INTO usuario_roles(usuario_id, rol)
SELECT id, rol FROM usuarios
ON CONFLICT (usuario_id, rol) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_usuario_roles_rol ON usuario_roles(rol);
