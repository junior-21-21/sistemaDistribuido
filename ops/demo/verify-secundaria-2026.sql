\set ON_ERROR_STOP on

\if :{?estudiantes_por_seccion}
\else
\set estudiantes_por_seccion 20
\endif
\if :{?esperar_horarios}
\else
\set esperar_horarios true
\endif

\connect academico_cursos
WITH sec AS (
 SELECT s.id FROM secciones s JOIN grados g ON g.id=s.grado_id
 JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND s.anio_escolar=2026 AND s.activa),
cur AS (SELECT c.id FROM cursos c JOIN grados g ON g.id=c.grado_id JOIN niveles n ON n.id=g.nivel_id
        WHERE n.nombre='SECUNDARIA' AND c.estado='ACTIVO'),
asi AS (SELECT a.* FROM asignaciones_academicas a JOIN sec s ON s.id=a.seccion_id WHERE a.anio_escolar=2026 AND a.activa),
hor AS (SELECT h.* FROM horarios h JOIN sec s ON s.id=h.seccion_id)
SELECT (SELECT count(*) FROM sec) secciones,
       (SELECT count(*) FROM cur) cursos,
       (SELECT count(*) FROM asi) asignaciones,
       (SELECT count(DISTINCT docente_id) FROM asi) docentes,
       (SELECT count(*) FROM hor) horarios,
       (SELECT count(*) FROM secciones s JOIN sec x ON x.id=s.id WHERE s.docente_tutor_id IS NOT NULL) tutores
\gset

SELECT (:secciones::integer=10 AND :cursos::integer=75 AND :asignaciones::integer=150
        AND :docentes::integer=30 AND :tutores::integer=10) estructura_ok \gset
\if :estructura_ok
\else
\echo 'ERROR estructura: secciones=' :secciones ' cursos=' :cursos ' asignaciones=' :asignaciones ' docentes=' :docentes ' tutores=' :tutores
\quit 3
\endif

\if :esperar_horarios
SELECT (:horarios::integer=350) horarios_ok \gset
\else
SELECT (:horarios::integer=0) horarios_ok \gset
\endif
\if :horarios_ok
\else
\echo 'ERROR cantidad de horarios=' :horarios
\quit 4
\endif

WITH sec AS (SELECT s.id FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id
 WHERE n.nombre='SECUNDARIA' AND s.anio_escolar=2026), hor AS (SELECT h.* FROM horarios h JOIN sec s ON s.id=h.seccion_id)
SELECT
 (SELECT count(*) FROM hor h1 JOIN horarios h2 ON h1.id<h2.id AND h1.docente_id=h2.docente_id
  AND h1.dia_semana=h2.dia_semana AND h1.hora_inicio<h2.hora_fin AND h1.hora_fin>h2.hora_inicio) conflictos_docente,
 (SELECT count(*) FROM hor h1 JOIN horarios h2 ON h1.id<h2.id AND h1.aula_id=h2.aula_id
  AND h1.dia_semana=h2.dia_semana AND h1.hora_inicio<h2.hora_fin AND h1.hora_fin>h2.hora_inicio) conflictos_aula,
 (SELECT count(*) FROM hor h JOIN secciones s ON s.id=h.seccion_id JOIN aulas a ON a.id=h.aula_id
  WHERE h.turno<>s.turno OR NOT a.activa OR (a.turno_disponible<>'AMBOS' AND a.turno_disponible<>h.turno)
     OR a.capacidad_maxima<s.capacidad_maxima) incompatibles,
 (SELECT count(*) FROM (
   SELECT a.docente_id FROM asignaciones_academicas a JOIN sec s ON s.id=a.seccion_id JOIN docentes d ON d.id=a.docente_id
   WHERE a.anio_escolar=2026 AND a.activa GROUP BY a.docente_id,d.horas_semanales_contratadas
   HAVING sum(a.horas_semanales)>d.horas_semanales_contratadas) carga) cargas_excedidas
\gset
SELECT (:conflictos_docente::integer=0 AND :conflictos_aula::integer=0
        AND :incompatibles::integer=0 AND :cargas_excedidas::integer=0) reglas_ok \gset
\if :reglas_ok
\else
\echo 'ERROR reglas: docente=' :conflictos_docente ' aula=' :conflictos_aula ' incompatibles=' :incompatibles ' carga=' :cargas_excedidas
\quit 5
\endif

\connect academico_estudiantes
WITH demo AS (
 SELECT * FROM estudiantes WHERE estado='ACTIVO'
 AND email ~ '^sec[0-9]+@estudiante\.iephuamanga\.edu\.pe$'
 AND substring(email from '^sec([0-9]+)@')::integer <= 10 * :'estudiantes_por_seccion'::integer),
por_seccion AS (SELECT seccion_id,count(*) cantidad FROM demo GROUP BY seccion_id)
SELECT (SELECT count(*) FROM demo) estudiantes,
       (SELECT count(*) FROM por_seccion) secciones_con_estudiantes,
       (SELECT min(cantidad) FROM por_seccion) minimo_seccion,
       (SELECT max(cantidad) FROM por_seccion) maximo_seccion,
       (SELECT count(*) FROM apoderado_estudiante ae JOIN demo d ON d.id=ae.estudiante_id) vinculos,
       (SELECT count(*) FROM consentimientos_datos c JOIN demo d ON d.id=c.estudiante_id
        WHERE c.registrado_por='seed-demo-secundaria') consentimientos
\gset
SELECT (:estudiantes::integer=10*:'estudiantes_por_seccion'::integer
        AND :secciones_con_estudiantes::integer=10
        AND :minimo_seccion::integer=:'estudiantes_por_seccion'::integer
        AND :maximo_seccion::integer=:'estudiantes_por_seccion'::integer
        AND :vinculos::integer=:estudiantes::integer
        AND :consentimientos::integer=3*:estudiantes::integer) estudiantes_ok \gset
\if :estudiantes_ok
\else
\echo 'ERROR padrón: estudiantes=' :estudiantes ' secciones=' :secciones_con_estudiantes ' min=' :minimo_seccion ' max=' :maximo_seccion ' vínculos=' :vinculos ' consentimientos=' :consentimientos
\quit 6
\endif

\connect academico_evaluaciones
SELECT (SELECT count(*) FROM notas WHERE observaciones='Registro generado para la demostración de secundaria') notas,
       (SELECT count(*) FROM asistencias WHERE registrado_por='seed-demo-secundaria'
        AND fecha BETWEEN DATE '2026-09-21' AND DATE '2026-09-25') asistencias
\gset
SELECT (:notas::integer=2*:estudiantes::integer) notas_ok \gset
\if :notas_ok
\else
\echo 'ERROR notas secundarias=' :notas
\quit 7
\endif
\if :esperar_horarios
SELECT (:asistencias::integer>=5*:estudiantes::integer) asistencias_ok \gset
\else
SELECT (:asistencias::integer=0) asistencias_ok \gset
\endif
\if :asistencias_ok
\else
\echo 'ERROR asistencias secundarias=' :asistencias
\quit 8
\endif

\connect academico_disciplina
SELECT count(*) reportes FROM reportes_disciplinarios
WHERE observaciones='Caso sintético para la demostración académica' \gset
SELECT (:reportes::integer=(:estudiantes::integer/10)) reportes_ok \gset
\if :reportes_ok
\else
\echo 'ERROR reportes disciplinarios secundarios=' :reportes
\quit 9
\endif

\echo 'DEMO SECUNDARIA OK'
\echo 'Estudiantes=' :estudiantes ' (10 secciones x ' :estudiantes_por_seccion ')'
\echo 'Docentes=' :docentes ' Cursos=' :cursos ' Asignaciones=' :asignaciones ' Horarios=' :horarios
\echo 'Notas=' :notas ' Asistencias=' :asistencias ' Reportes=' :reportes ' Consentimientos=' :consentimientos
