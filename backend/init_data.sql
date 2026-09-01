-- ==========================================================
-- SCRIPT DE INICIALIZACIÓN DE DATOS (PostgreSQL)
-- ==========================================================
-- IMPORTANTE: Ejecutar los bloques de este script desde DBeaver o pgAdmin
-- conectándote a cada base de datos específica según lo indicado.
-- ==========================================================

-- ==========================================================
-- SEED DE DATOS BÁSICOS Y DOCENTES
-- -> EJECUTAR EN BASE DE DATOS: academico_cursos
-- ==========================================================

-- Insertar Cursos Básicos si no existen (Spring Boot crea la tabla 'cursos')
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'MAT-01', 'Matemática I', 4, 'OBLIGATORIO', 1, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'MAT-01');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'COM-01', 'Comunicación I', 4, 'OBLIGATORIO', 1, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'COM-01');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'CYT-01', 'Ciencia y Tecnología', 3, 'OBLIGATORIO', 1, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'CYT-01');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'MAT-02', 'Matemática II', 4, 'OBLIGATORIO', 2, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'MAT-02');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'COM-02', 'Comunicación II', 4, 'OBLIGATORIO', 2, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'COM-02');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'HIS-02', 'Historia', 3, 'OBLIGATORIO', 2, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'HIS-02');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'MAT-03', 'Matemática III', 4, 'OBLIGATORIO', 3, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'MAT-03');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'COM-03', 'Comunicación III', 4, 'OBLIGATORIO', 3, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'COM-03');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'MAT-04', 'Matemática IV', 4, 'OBLIGATORIO', 4, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'MAT-04');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'COM-04', 'Comunicación IV', 4, 'OBLIGATORIO', 4, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'COM-04');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'MAT-05', 'Matemática V', 4, 'OBLIGATORIO', 5, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'MAT-05');
INSERT INTO cursos (codigo, nombre, creditos, tipo, grado_id, estado)
SELECT 'COM-05', 'Comunicación V', 4, 'OBLIGATORIO', 5, 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE codigo = 'COM-05');


-- Crear tabla docentes e insertar
CREATE TABLE IF NOT EXISTS docentes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    apellido VARCHAR(255) NOT NULL,
    dni VARCHAR(8) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    especialidad VARCHAR(255),
    estado VARCHAR(255) NOT NULL DEFAULT 'ACTIVO'
);

INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Juan', 'Pérez', '12345678', 'juan.perez@iep.edu.pe', 'Matemática', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '12345678');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'María', 'Salas', '87654321', 'maria.salas@iep.edu.pe', 'Comunicación', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '87654321');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Carlos', 'López', '11223344', 'carlos.lopez@iep.edu.pe', 'Historia', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '11223344');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Ana', 'Gómez', '44332211', 'ana.gomez@iep.edu.pe', 'Inglés', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '44332211');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Luis', 'Torres', '55667788', 'luis.torres@iep.edu.pe', 'Educación Física', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '55667788');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Elena', 'Martínez', '88776655', 'elena.martinez@iep.edu.pe', 'Física', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '88776655');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Jorge', 'Sánchez', '99887766', 'jorge.sanchez@iep.edu.pe', 'Química', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '99887766');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Rosa', 'Díaz', '66778899', 'rosa.diaz@iep.edu.pe', 'Biología', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '66778899');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Miguel', 'Rojas', '22334455', 'miguel.rojas@iep.edu.pe', 'Arte', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '22334455');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Carmen', 'Vargas', '55443322', 'carmen.vargas@iep.edu.pe', 'Música', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '55443322');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Raúl', 'Fernández', '10203040', 'raul.fernandez@iep.edu.pe', 'Filosofía', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '10203040');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Patricia', 'Quispe', '04030201', 'patricia.quispe@iep.edu.pe', 'Psicología', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '04030201');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Fernando', 'Condori', '15253545', 'fernando.condori@iep.edu.pe', 'Computación', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '15253545');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Lucía', 'Mamani', '54535251', 'lucia.mamani@iep.edu.pe', 'Danza', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '54535251');
INSERT INTO docentes (nombre, apellido, dni, email, especialidad, estado)
SELECT 'Hugo', 'Cruz', '16263646', 'hugo.cruz@iep.edu.pe', 'Raz. Matemático', 'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM docentes WHERE dni = '16263646');


-- ==========================================================
-- SEED DE NOTAS
-- -> EJECUTAR EN BASE DE DATOS: academico_evaluaciones
-- ==========================================================
CREATE TABLE IF NOT EXISTS notas (
    id SERIAL PRIMARY KEY,
    escala_literal VARCHAR(2),
    valor_numerico FLOAT,
    competencia VARCHAR(100) NOT NULL,
    bimestre VARCHAR(50) NOT NULL,
    periodo_anio VARCHAR(20) NOT NULL,
    estudiante_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    seccion_id BIGINT NOT NULL,
    fecha_registro TIMESTAMP NOT NULL,
    fecha_modificacion TIMESTAMP,
    observaciones VARCHAR(500)
);

INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'AD', 'Resuelve problemas de cantidad', 'I BIMESTRE', '2026', 1, 1, 1, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 1);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'A', 'Resuelve problemas de regularidad', 'I BIMESTRE', '2026', 1, 1, 1, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 2);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'A', 'Se comunica oralmente', 'I BIMESTRE', '2026', 1, 2, 2, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 3);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'B', 'Lee diversos tipos de textos', 'I BIMESTRE', '2026', 1, 2, 2, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 4);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'C', 'Indaga mediante métodos científicos', 'I BIMESTRE', '2026', 2, 3, 3, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 5);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'AD', 'Explica el mundo físico', 'I BIMESTRE', '2026', 2, 3, 3, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 6);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'A', 'Construye interpretaciones históricas', 'I BIMESTRE', '2026', 3, 6, 4, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 7);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'B', 'Gestiona responsablemente el espacio', 'I BIMESTRE', '2026', 3, 6, 4, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 8);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'AD', 'Resuelve problemas de forma', 'I BIMESTRE', '2026', 4, 1, 1, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 9);
INSERT INTO notas (escala_literal, competencia, bimestre, periodo_anio, estudiante_id, curso_id, docente_id, seccion_id, fecha_registro)
SELECT 'A', 'Escribe diversos tipos de textos', 'I BIMESTRE', '2026', 5, 2, 2, 1, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM notas WHERE id = 10);


-- ==========================================================
-- SEED DE DISCIPLINA
-- -> EJECUTAR EN BASE DE DATOS: academico_disciplina
-- ==========================================================
CREATE TABLE IF NOT EXISTS reportes_disciplinarios (
    id SERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    seccion_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    descripcion VARCHAR(600) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'EMITIDO',
    apoderado_notificado BOOLEAN DEFAULT FALSE,
    fecha_reporte TIMESTAMP NOT NULL,
    fecha_notificacion TIMESTAMP,
    observaciones VARCHAR(300)
);

INSERT INTO reportes_disciplinarios (estudiante_id, docente_id, seccion_id, tipo, descripcion, estado, apoderado_notificado, fecha_reporte)
SELECT 1, 1, 1, 'TARDANZA', 'El estudiante llegó 15 minutos tarde a la primera hora.', 'EMITIDO', FALSE, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM reportes_disciplinarios WHERE id = 1);
INSERT INTO reportes_disciplinarios (estudiante_id, docente_id, seccion_id, tipo, descripcion, estado, apoderado_notificado, fecha_reporte)
SELECT 2, 2, 1, 'FALTA_LEVE', 'Uso del celular en clase de matemáticas.', 'NOTIFICADO', TRUE, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM reportes_disciplinarios WHERE id = 2);
INSERT INTO reportes_disciplinarios (estudiante_id, docente_id, seccion_id, tipo, descripcion, estado, apoderado_notificado, fecha_reporte)
SELECT 3, 3, 1, 'MERITO', 'Participación destacada en la feria de ciencias.', 'ARCHIVADO', TRUE, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM reportes_disciplinarios WHERE id = 3);
INSERT INTO reportes_disciplinarios (estudiante_id, docente_id, seccion_id, tipo, descripcion, estado, apoderado_notificado, fecha_reporte)
SELECT 4, 4, 1, 'FALTA_GRAVE', 'Pelea en el recreo con un compañero de otro salón.', 'EMITIDO', FALSE, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM reportes_disciplinarios WHERE id = 4);
INSERT INTO reportes_disciplinarios (estudiante_id, docente_id, seccion_id, tipo, descripcion, estado, apoderado_notificado, fecha_reporte)
SELECT 5, 5, 1, 'LLAMADA_ATENCION', 'No trajo los materiales solicitados para arte.', 'NOTIFICADO', TRUE, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM reportes_disciplinarios WHERE id = 5);
