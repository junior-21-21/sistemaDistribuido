-- Organización real de un colegio público: 5 roles base, encargos y módulos opcionales.
--   * Coordinación académica y Administración del sistema pasan a Dirección.
--   * Coordinación de convivencia pasa a ser un auxiliar con el encargo RESPONSABLE_CONVIVENCIA.
--   * Psicología, Enfermería y Mantenimiento son módulos que Dirección habilita si tiene ese personal.

CREATE TABLE modulos_institucion (
    codigo VARCHAR(30) PRIMARY KEY,
    habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    actualizado_por VARCHAR(100) NOT NULL DEFAULT 'sistema',
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_modulo_codigo CHECK (codigo IN ('PSICOLOGIA', 'ENFERMERIA', 'MANTENIMIENTO'))
);

-- Un módulo queda habilitado si el colegio ya tiene personal activo con ese rol.
INSERT INTO modulos_institucion (codigo, habilitado)
SELECT m.codigo, EXISTS (
    SELECT 1 FROM usuarios u JOIN usuario_roles r ON r.usuario_id = u.id
    WHERE r.rol = m.rol AND u.estado = 'ACTIVO')
FROM (VALUES ('PSICOLOGIA', 'PSICOLOGO'), ('ENFERMERIA', 'ENFERMERA'), ('MANTENIMIENTO', 'MANTENIMIENTO')) AS m(codigo, rol);

-- Coordinación de convivencia → auxiliar con encargo de responsable de convivencia
-- (si la persona ya era docente, conserva ese puesto).
INSERT INTO usuario_roles (usuario_id, rol)
SELECT usuario_id, 'AUXILIAR' FROM usuario_roles r
WHERE r.rol = 'COORDINADOR_CONVIVENCIA'
  AND NOT EXISTS (SELECT 1 FROM usuario_roles d WHERE d.usuario_id = r.usuario_id AND d.rol = 'DOCENTE')
ON CONFLICT DO NOTHING;
INSERT INTO usuario_roles (usuario_id, rol)
SELECT usuario_id, 'RESPONSABLE_CONVIVENCIA' FROM usuario_roles WHERE rol = 'COORDINADOR_CONVIVENCIA'
ON CONFLICT DO NOTHING;

-- Coordinación académica y Administración del sistema → Dirección
INSERT INTO usuario_roles (usuario_id, rol)
SELECT usuario_id, 'DIRECTOR' FROM usuario_roles WHERE rol IN ('COORDINADOR_ACADEMICO', 'ADMIN_SISTEMA')
ON CONFLICT DO NOTHING;

DELETE FROM usuario_roles WHERE rol IN ('COORDINADOR_ACADEMICO', 'ADMIN_SISTEMA', 'COORDINADOR_CONVIVENCIA');

-- Rol principal (columna heredada) alineado con el nuevo modelo
UPDATE usuarios SET rol = 'DIRECTOR' WHERE rol IN ('COORDINADOR_ACADEMICO', 'ADMIN_SISTEMA');
UPDATE usuarios u SET rol = CASE
        WHEN EXISTS (SELECT 1 FROM usuario_roles d WHERE d.usuario_id = u.id AND d.rol = 'DOCENTE') THEN 'DOCENTE'
        ELSE 'AUXILIAR' END
WHERE rol = 'COORDINADOR_CONVIVENCIA';

-- Ningún rol fuera del catálogo nuevo
ALTER TABLE usuario_roles ADD CONSTRAINT ck_usuario_rol CHECK (rol IN (
    'DIRECTOR', 'DOCENTE', 'AUXILIAR', 'SECRETARIA', 'APODERADO',
    'PSICOLOGO', 'ENFERMERA', 'MANTENIMIENTO',
    'RESPONSABLE_CONVIVENCIA', 'PRIMEROS_AUXILIOS'));
