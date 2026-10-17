\set ON_ERROR_STOP on

\connect academico_cursos
BEGIN;

INSERT INTO niveles(nombre, descripcion, activo) VALUES
('PRIMARIA', 'Educación primaria', true),
('SECUNDARIA', 'Educación secundaria', true)
ON CONFLICT (nombre) DO UPDATE SET descripcion = EXCLUDED.descripcion, activo = true;

INSERT INTO grados(nombre, nivel_id, activo)
SELECT data.nombre, n.id, true
FROM (VALUES ('PRIMERO'), ('SEGUNDO'), ('TERCERO'), ('CUARTO'), ('QUINTO')) data(nombre)
JOIN niveles n ON n.nombre = 'PRIMARIA'
WHERE NOT EXISTS (SELECT 1 FROM grados g WHERE g.nombre = data.nombre AND g.nivel_id = n.id);

INSERT INTO docentes(nombre, apellido, dni, email, especialidad, estado, horas_semanales_contratadas) VALUES
('Ana', 'Flores Huanca', '70110001', 'ana.flores@iephuamanga.edu.pe', 'Matemática', 'ACTIVO', 30),
('Luis', 'Palomino Ccahuana', '70110002', 'luis.palomino@iephuamanga.edu.pe', 'Comunicación', 'ACTIVO', 30),
('Elena', 'Soto Gutiérrez', '70110003', 'elena.soto@iephuamanga.edu.pe', 'Ciencia y Tecnología', 'ACTIVO', 24),
('Miguel', 'Ramos Paredes', '70110004', 'miguel.ramos@iephuamanga.edu.pe', 'Personal Social', 'ACTIVO', 24),
('Teresa', 'Vílchez Quispe', '70110005', 'teresa.vilchez@iephuamanga.edu.pe', 'Arte y Cultura', 'ACTIVO', 20)
ON CONFLICT (dni) DO UPDATE SET estado = 'ACTIVO', especialidad = EXCLUDED.especialidad,
horas_semanales_contratadas = EXCLUDED.horas_semanales_contratadas;

INSERT INTO aulas(nombre, pabellon, capacidad_maxima, turno_disponible, observaciones, activa)
SELECT 'Aula ' || n, CASE WHEN n <= 5 THEN 'Pabellón A' ELSE 'Pabellón B' END,
30, CASE WHEN n % 2 = 0 THEN 'TARDE' ELSE 'MANANA' END, 'Ambiente equipado para clases', true
FROM generate_series(101, 110) n
WHERE NOT EXISTS (SELECT 1 FROM aulas a WHERE a.nombre = 'Aula ' || n);

INSERT INTO secciones(nombre, grado_id, docente_tutor_id, capacidad_maxima, turno, anio_escolar, activa)
SELECT letra, g.id,
       CASE WHEN letra = 'A' THEN (SELECT min(id) FROM docentes) ELSE (SELECT max(id) FROM docentes) END,
       30, CASE WHEN letra = 'A' THEN 'MANANA' ELSE 'TARDE' END, 2026, true
FROM grados g JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA'
CROSS JOIN (VALUES ('A'), ('B')) letras(letra)
WHERE NOT EXISTS (SELECT 1 FROM secciones s WHERE s.nombre = letra AND s.grado_id = g.id AND s.anio_escolar = 2026);

INSERT INTO cursos(codigo, nombre, creditos, tipo, grado_id, estado)
SELECT materia.prefijo || grado.numero, materia.nombre, materia.horas, 'OBLIGATORIO', g.id, 'ACTIVO'
FROM grados g
JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA'
JOIN (VALUES ('PRIMERO','1'),('SEGUNDO','2'),('TERCERO','3'),('CUARTO','4'),('QUINTO','5')) grado(nombre,numero)
  ON grado.nombre = g.nombre
CROSS JOIN (VALUES ('MAT','Matemática',5),('COM','Comunicación',4)) materia(prefijo,nombre,horas)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, estado = 'ACTIVO';

INSERT INTO periodos_escolares(nombre, anio_escolar, fecha_inicio, fecha_fin, estado, creado_por, fecha_creacion)
SELECT 'Año Escolar 2026', 2026, DATE '2026-03-16', DATE '2026-12-18', 'ACTIVO', 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM periodos_escolares WHERE anio_escolar = 2026);

INSERT INTO asignaciones_academicas(curso_id, docente_id, seccion_id, horas_semanales, anio_escolar, activa)
SELECT c.id,
       CASE WHEN c.codigo LIKE 'MAT%' THEN (SELECT id FROM docentes WHERE dni='70110001')
            ELSE (SELECT id FROM docentes WHERE dni='70110002') END,
       s.id, CASE WHEN c.codigo LIKE 'MAT%' THEN 5 ELSE 4 END, 2026, true
FROM secciones s
JOIN grados g ON g.id = s.grado_id
JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA'
JOIN cursos c ON c.grado_id = g.id
WHERE s.anio_escolar = 2026 AND (c.codigo LIKE 'MAT%' OR c.codigo LIKE 'COM%')
ON CONFLICT (curso_id, docente_id, seccion_id, anio_escolar)
DO UPDATE SET horas_semanales = EXCLUDED.horas_semanales, activa = true;

INSERT INTO horarios(curso_id, docente_id, seccion_id, aula_id, dia_semana, hora_inicio, hora_fin, turno)
SELECT a.curso_id, a.docente_id, a.seccion_id,
       (SELECT au.id FROM aulas au WHERE au.activa ORDER BY au.id LIMIT 1 OFFSET ((a.seccion_id - 1) % 10)::integer),
       CASE WHEN c.codigo LIKE 'MAT%' THEN 'LUNES' ELSE 'MARTES' END,
       CASE WHEN s.turno = 'MANANA' THEN TIME '08:00' ELSE TIME '14:00' END,
       CASE WHEN s.turno = 'MANANA' THEN TIME '09:30' ELSE TIME '15:30' END,
       s.turno
FROM asignaciones_academicas a
JOIN cursos c ON c.id = a.curso_id
JOIN secciones s ON s.id = a.seccion_id
JOIN grados g ON g.id=s.grado_id
JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA'
WHERE a.anio_escolar = 2026
  AND NOT EXISTS (SELECT 1 FROM horarios h WHERE h.curso_id=a.curso_id AND h.seccion_id=a.seccion_id
                  AND h.dia_semana=CASE WHEN c.codigo LIKE 'MAT%' THEN 'LUNES' ELSE 'MARTES' END);

UPDATE tickets_mantenimiento SET aula_id = (SELECT min(id) FROM aulas) WHERE aula_id NOT IN (SELECT id FROM aulas);
UPDATE tickets_mantenimiento SET docente_id = (SELECT min(id) FROM docentes) WHERE docente_id NOT IN (SELECT id FROM docentes);
COMMIT;

SELECT s.id AS sec_1a FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='PRIMERO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_1b FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='PRIMERO' AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_2a FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='SEGUNDO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_2b FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='SEGUNDO' AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_3a FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='TERCERO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_3b FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='TERCERO' AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_4a FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='CUARTO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_4b FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='CUARTO' AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_5a FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='QUINTO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id AS sec_5b FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='PRIMARIA' WHERE g.nombre='QUINTO' AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT id AS mat1 FROM cursos WHERE codigo='MAT1' \gset
SELECT id AS mat2 FROM cursos WHERE codigo='MAT2' \gset
SELECT id AS mat3 FROM cursos WHERE codigo='MAT3' \gset
SELECT id AS mat4 FROM cursos WHERE codigo='MAT4' \gset
SELECT id AS mat5 FROM cursos WHERE codigo='MAT5' \gset
SELECT id AS com1 FROM cursos WHERE codigo='COM1' \gset
SELECT id AS com2 FROM cursos WHERE codigo='COM2' \gset
SELECT id AS com3 FROM cursos WHERE codigo='COM3' \gset
SELECT id AS com4 FROM cursos WHERE codigo='COM4' \gset
SELECT id AS com5 FROM cursos WHERE codigo='COM5' \gset
SELECT id AS doc_mat FROM docentes WHERE dni='70110001' \gset
SELECT id AS doc_com FROM docentes WHERE dni='70110002' \gset
SELECT h.id AS hor_1a FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT1' AND h.seccion_id=:sec_1a \gset
SELECT h.id AS hor_1b FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT1' AND h.seccion_id=:sec_1b \gset
SELECT h.id AS hor_2a FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT2' AND h.seccion_id=:sec_2a \gset
SELECT h.id AS hor_2b FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT2' AND h.seccion_id=:sec_2b \gset
SELECT h.id AS hor_3a FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT3' AND h.seccion_id=:sec_3a \gset
SELECT h.id AS hor_3b FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT3' AND h.seccion_id=:sec_3b \gset
SELECT h.id AS hor_4a FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT4' AND h.seccion_id=:sec_4a \gset
SELECT h.id AS hor_4b FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT4' AND h.seccion_id=:sec_4b \gset
SELECT h.id AS hor_5a FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT5' AND h.seccion_id=:sec_5a \gset
SELECT h.id AS hor_5b FROM horarios h JOIN cursos c ON c.id=h.curso_id WHERE c.codigo='MAT5' AND h.seccion_id=:sec_5b \gset

\connect academico_estudiantes
BEGIN;
UPDATE estudiantes SET seccion_id = CASE
 WHEN grado_id=1 AND seccion_id IN (1,2) THEN CASE WHEN seccion_id=1 THEN :sec_1a ELSE :sec_1b END
 WHEN grado_id=2 AND seccion_id IN (1,2) THEN CASE WHEN seccion_id=1 THEN :sec_2a ELSE :sec_2b END
 WHEN grado_id=3 AND seccion_id IN (1,2) THEN CASE WHEN seccion_id=1 THEN :sec_3a ELSE :sec_3b END
 WHEN grado_id=4 AND seccion_id IN (1,2) THEN CASE WHEN seccion_id=1 THEN :sec_4a ELSE :sec_4b END
 WHEN grado_id=5 AND seccion_id IN (1,2) THEN CASE WHEN seccion_id=1 THEN :sec_5a ELSE :sec_5b END
 ELSE seccion_id END;
COMMIT;

\connect academico_evaluaciones
BEGIN;
WITH alumnos(estudiante_id,seccion_id,horario_id,curso_mat,curso_com) AS (VALUES
 (1,:sec_1a,:hor_1a,:mat1,:com1),(2,:sec_1a,:hor_1a,:mat1,:com1),(3,:sec_1b,:hor_1b,:mat1,:com1),
 (4,:sec_2a,:hor_2a,:mat2,:com2),(5,:sec_2a,:hor_2a,:mat2,:com2),(6,:sec_2b,:hor_2b,:mat2,:com2),
 (7,:sec_3a,:hor_3a,:mat3,:com3),(8,:sec_3a,:hor_3a,:mat3,:com3),(9,:sec_3b,:hor_3b,:mat3,:com3),
 (10,:sec_4a,:hor_4a,:mat4,:com4),(11,:sec_4a,:hor_4a,:mat4,:com4),(12,:sec_4b,:hor_4b,:mat4,:com4),
 (13,:sec_5a,:hor_5a,:mat5,:com5),(14,:sec_5a,:hor_5a,:mat5,:com5),(15,:sec_5b,:hor_5b,:mat5,:com5),
 (16,:sec_1a,:hor_1a,:mat1,:com1),(17,:sec_2a,:hor_2a,:mat2,:com2),(18,:sec_3a,:hor_3a,:mat3,:com3),
 (19,:sec_4b,:hor_4b,:mat4,:com4),(20,:sec_5a,:hor_5a,:mat5,:com5)
), materias AS (
 SELECT a.*, m.curso_id, m.docente_id, m.competencia
 FROM alumnos a CROSS JOIN LATERAL (VALUES
   (a.curso_mat, :doc_mat, 'Resuelve problemas de cantidad'),
   (a.curso_com, :doc_com, 'Lee diversos tipos de textos')) m(curso_id,docente_id,competencia)
)
INSERT INTO notas(estudiante_id,curso_id,docente_id,seccion_id,bimestre,competencia,valor_numerico,
escala_literal,observaciones,periodo_anio,fecha_registro)
SELECT estudiante_id,curso_id,docente_id,seccion_id,'BIMESTRE_1',competencia,
       12 + ((estudiante_id + curso_id) % 8),
       CASE WHEN 12 + ((estudiante_id + curso_id) % 8) >= 18 THEN 'AD'
            WHEN 12 + ((estudiante_id + curso_id) % 8) >= 14 THEN 'A' ELSE 'B' END,
       'Avance registrado durante el primer bimestre','2026',CURRENT_TIMESTAMP
FROM materias m
WHERE NOT EXISTS (SELECT 1 FROM notas n WHERE n.estudiante_id=m.estudiante_id AND n.curso_id=m.curso_id
                  AND n.bimestre='BIMESTRE_1' AND n.competencia=m.competencia);

WITH alumnos(estudiante_id,seccion_id,horario_id,curso_id) AS (VALUES
 (1,:sec_1a,:hor_1a,:mat1),(2,:sec_1a,:hor_1a,:mat1),(3,:sec_1b,:hor_1b,:mat1),
 (4,:sec_2a,:hor_2a,:mat2),(5,:sec_2a,:hor_2a,:mat2),(6,:sec_2b,:hor_2b,:mat2),
 (7,:sec_3a,:hor_3a,:mat3),(8,:sec_3a,:hor_3a,:mat3),(9,:sec_3b,:hor_3b,:mat3),
 (10,:sec_4a,:hor_4a,:mat4),(11,:sec_4a,:hor_4a,:mat4),(12,:sec_4b,:hor_4b,:mat4),
 (13,:sec_5a,:hor_5a,:mat5),(14,:sec_5a,:hor_5a,:mat5),(15,:sec_5b,:hor_5b,:mat5),
 (16,:sec_1a,:hor_1a,:mat1),(17,:sec_2a,:hor_2a,:mat2),(18,:sec_3a,:hor_3a,:mat3),
 (19,:sec_4b,:hor_4b,:mat4),(20,:sec_5a,:hor_5a,:mat5)
), registros AS (
 SELECT a.*, CURRENT_DATE - dias AS fecha,
   CASE WHEN (estudiante_id + dias) % 17 = 0 THEN 'FALTA'
        WHEN (estudiante_id + dias) % 11 = 0 THEN 'TARDE' ELSE 'PRESENTE' END estado
 FROM alumnos a CROSS JOIN generate_series(1,5) dias
)
INSERT INTO asistencias(estudiante_id,horario_id,seccion_id,curso_id,docente_id,fecha,estado,turno,
justificacion,registrado_por,fecha_registro)
SELECT estudiante_id,horario_id,seccion_id,curso_id,:doc_mat,fecha,estado,'MANANA',
       CASE WHEN estado='FALTA' THEN 'Pendiente de justificación' ELSE NULL END,
       'docente1',CURRENT_TIMESTAMP
FROM registros r
ON CONFLICT (estudiante_id,horario_id,fecha) DO NOTHING;

INSERT INTO sesiones_asistencia(horario_id,seccion_id,curso_id,docente_id,fecha,estado,abierta_por,cerrada_por,fecha_apertura,fecha_cierre)
SELECT horario_id,min(seccion_id),min(curso_id),:doc_mat,fecha,'CERRADA','docente1','docente1',
       fecha + TIME '08:00',fecha + TIME '09:35'
FROM asistencias GROUP BY horario_id,fecha
ON CONFLICT (horario_id,fecha) DO NOTHING;
COMMIT;

\connect academico_disciplina
BEGIN;
INSERT INTO reportes_disciplinarios(estudiante_id,docente_id,seccion_id,tipo,descripcion,observaciones,
estado,apoderado_notificado,fecha_reporte,fecha_notificacion,registrado_por)
SELECT data.estudiante_id, data.docente_id, data.seccion_id, data.tipo, data.descripcion,
       data.observaciones, data.estado, data.notificado, CURRENT_TIMESTAMP - data.dias * INTERVAL '1 day',
       CASE WHEN data.notificado THEN CURRENT_TIMESTAMP - data.dias * INTERVAL '1 day' ELSE NULL END,
       CASE WHEN data.docente_id = :doc_mat THEN 'docente1' ELSE 'docente2' END
FROM (VALUES
 (3,:doc_mat,:sec_1b,'TARDANZA','Ingreso después del inicio de clases','Se dialogó con el estudiante','NOTIFICADO',true,2),
 (6,:doc_com,:sec_2b,'FALTA_LEVE','Interrumpió reiteradamente la sesión','Compromiso verbal','EMITIDO',false,4),
 (9,:doc_mat,:sec_3b,'MERITO','Apoyó a un compañero durante una actividad','Reconocimiento en aula','ARCHIVADO',true,6),
 (12,:doc_com,:sec_4b,'LLAMADA_ATENCION','No presentó el material solicitado','Se brindó una nueva fecha','NOTIFICADO',true,8),
 (15,:doc_mat,:sec_5b,'FALTA_GRAVE','Conflicto físico durante el recreo','Derivado a convivencia','EMITIDO',false,10),
 (2,:doc_com,:sec_1a,'MERITO','Participación destacada en lectura','Felicitación','ARCHIVADO',true,12)
) data(estudiante_id,docente_id,seccion_id,tipo,descripcion,observaciones,estado,notificado,dias)
WHERE NOT EXISTS (SELECT 1 FROM reportes_disciplinarios r WHERE r.estudiante_id=data.estudiante_id
                  AND r.tipo=data.tipo AND r.descripcion=data.descripcion);
COMMIT;

\connect academico_reportes
BEGIN;
INSERT INTO actividades(tipo,texto,origen,fecha)
SELECT * FROM (VALUES
 ('MATRICULA','Se registraron 20 matrículas activas','servicio-estudiantes',CURRENT_TIMESTAMP-INTERVAL '12 days'),
 ('HORARIO','Se publicó la programación académica 2026','servicio-cursos',CURRENT_TIMESTAMP-INTERVAL '10 days'),
 ('ASISTENCIA','Se cerraron las sesiones de asistencia de la semana','servicio-evaluaciones',CURRENT_TIMESTAMP-INTERVAL '2 days'),
 ('EVALUACION','Se registraron avances del primer bimestre','servicio-evaluaciones',CURRENT_TIMESTAMP-INTERVAL '1 day'),
 ('CONVIVENCIA','Se realizó seguimiento a incidencias de aula','servicio-disciplina',CURRENT_TIMESTAMP-INTERVAL '8 hours'),
 ('SALUD','Tópico actualizó fichas médicas prioritarias','servicio-estudiantes',CURRENT_TIMESTAMP-INTERVAL '4 hours'),
 ('SISTEMA','Se verificó la integridad de referencias distribuidas','servicio-reportes',CURRENT_TIMESTAMP-INTERVAL '1 hour')
) data(tipo,texto,origen,fecha)
WHERE NOT EXISTS (SELECT 1 FROM actividades a WHERE a.tipo=data.tipo AND a.texto=data.texto);
COMMIT;
