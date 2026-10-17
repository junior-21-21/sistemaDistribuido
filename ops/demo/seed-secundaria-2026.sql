\set ON_ERROR_STOP on

\if :{?estudiantes_por_seccion}
\else
\set estudiantes_por_seccion 20
\endif
\if :{?cargar_horarios}
\else
\set cargar_horarios true
\endif
\if :{?reiniciar_horarios}
\else
\set reiniciar_horarios false
\endif

SELECT (:'estudiantes_por_seccion'::integer BETWEEN 1 AND 35) AS cantidad_estudiantes_valida \gset
\if :cantidad_estudiantes_valida
\else
\echo 'estudiantes_por_seccion debe estar entre 1 y 35'
\quit 2
\endif

\connect academico_cursos
BEGIN;

INSERT INTO niveles(nombre,descripcion,activo)
VALUES ('SECUNDARIA','Educación secundaria de primero a quinto grado',true)
ON CONFLICT (nombre) DO UPDATE SET descripcion=EXCLUDED.descripcion,activo=true;

INSERT INTO grados(nombre,nivel_id,activo)
SELECT v.nombre,n.id,true
FROM (VALUES ('PRIMERO'),('SEGUNDO'),('TERCERO'),('CUARTO'),('QUINTO')) v(nombre)
JOIN niveles n ON n.nombre='SECUNDARIA'
WHERE NOT EXISTS (SELECT 1 FROM grados g WHERE g.nombre=v.nombre AND g.nivel_id=n.id);

INSERT INTO docentes(nombre,apellido,dni,email,especialidad,username,estado,horas_semanales_contratadas) VALUES
('Ana','Flores Huanca','70110001','ana.flores@iephuamanga.edu.pe','Matemática','docente1','ACTIVO',30),
('Luis','Palomino Ccahuana','70110002','luis.palomino@iephuamanga.edu.pe','Comunicación','docente2','ACTIVO',30),
('Karen','Chávez Rojas','70110006','karen.chavez@iephuamanga.edu.pe','Inglés','docente.ingles.m','ACTIVO',30),
('Elena','Soto Gutiérrez','70110003','elena.soto@iephuamanga.edu.pe','Ciencia y Tecnología','coord.academica','ACTIVO',30),
('Miguel','Ramos Paredes','70110004','miguel.ramos@iephuamanga.edu.pe','Ciencias Sociales','docente.sociales.m','ACTIVO',30),
('Patricia','León Vargas','70110007','patricia.leon@iephuamanga.edu.pe','DPCC','docente.dpcc.m','ACTIVO',30),
('Jorge','Quispe Arias','70110008','jorge.quispe@iephuamanga.edu.pe','Educación para el Trabajo','docente.ept.m','ACTIVO',30),
('Daniela','Huamán Soto','70110009','daniela.huaman@iephuamanga.edu.pe','Educación Física','docente.efisica.m','ACTIVO',30),
('Teresa','Vílchez Quispe','70110005','teresa.vilchez@iephuamanga.edu.pe','Arte y Cultura','docente.arte.m','ACTIVO',30),
('José','Cárdenas López','70110010','jose.cardenas@iephuamanga.edu.pe','Educación Religiosa','docente.religion.m','ACTIVO',30),
('Mónica','Ayala Torres','70110011','monica.ayala@iephuamanga.edu.pe','Tutoría','docente.tutoria.m','ACTIVO',30),
('Raúl','Sullca Huamán','70110012','raul.sullca@iephuamanga.edu.pe','Quechua','docente.quechua.m','ACTIVO',30),
('Carmen','Rojas Poma','70110013','carmen.rojas@iephuamanga.edu.pe','Taller TIC','docente.tic.m','ACTIVO',30),
('Víctor','Mendoza Flores','70110014','victor.mendoza@iephuamanga.edu.pe','Razonamiento Matemático','docente.razonamiento.m','ACTIVO',30),
('Silvia','Condori Luna','70110015','silvia.condori@iephuamanga.edu.pe','Plan Lector','docente.lector.m','ACTIVO',30),
('Ricardo','Gutiérrez Paredes','70110016','ricardo.gutierrez@iephuamanga.edu.pe','Matemática','docente.matematica.t','ACTIVO',30),
('Marisol','Puma Salazar','70110017','marisol.puma@iephuamanga.edu.pe','Comunicación','docente.comunicacion.t','ACTIVO',30),
('Henry','Castro Quispe','70110018','henry.castro@iephuamanga.edu.pe','Inglés','docente.ingles.t','ACTIVO',30),
('Natalia','Cáceres Huanca','70110019','natalia.caceres@iephuamanga.edu.pe','Ciencia y Tecnología','docente.cyt.t','ACTIVO',30),
('Edwin','Palacios Suni','70110020','edwin.palacios@iephuamanga.edu.pe','Ciencias Sociales','docente.sociales.t','ACTIVO',30),
('Roxana','Vega Ccahuana','70110021','roxana.vega@iephuamanga.edu.pe','DPCC','docente.dpcc.t','ACTIVO',30),
('Óscar','Torres Mamani','70110022','oscar.torres@iephuamanga.edu.pe','Educación para el Trabajo','docente.ept.t','ACTIVO',30),
('Fiorella','Soto Ramos','70110023','fiorella.soto@iephuamanga.edu.pe','Educación Física','docente.efisica.t','ACTIVO',30),
('Alonso','Pérez Sulca','70110024','alonso.perez@iephuamanga.edu.pe','Arte y Cultura','docente.arte.t','ACTIVO',30),
('Beatriz','Huanca León','70110025','beatriz.huanca@iephuamanga.edu.pe','Educación Religiosa','docente.religion.t','ACTIVO',30),
('Gustavo','Flores Ayala','70110026','gustavo.flores@iephuamanga.edu.pe','Tutoría','docente.tutoria.t','ACTIVO',30),
('Yolanda','Quispe Poma','70110027','yolanda.quispe@iephuamanga.edu.pe','Quechua','docente.quechua.t','ACTIVO',30),
('César','Luna Vargas','70110028','cesar.luna@iephuamanga.edu.pe','Taller TIC','docente.tic.t','ACTIVO',30),
('Liliana','Rojas Cárdenas','70110029','liliana.rojas@iephuamanga.edu.pe','Razonamiento Matemático','docente.razonamiento.t','ACTIVO',30),
('Samuel','Mendoza Arias','70110030','samuel.mendoza@iephuamanga.edu.pe','Plan Lector','docente.lector.t','ACTIVO',30)
ON CONFLICT (dni) DO UPDATE SET especialidad=EXCLUDED.especialidad,username=EXCLUDED.username,
estado='ACTIVO',horas_semanales_contratadas=30;

INSERT INTO aulas(nombre,pabellon,capacidad_maxima,turno_disponible,observaciones,activa)
SELECT 'Secundaria '||n,'Pabellón Secundaria',35,
       CASE WHEN n%2=1 THEN 'MANANA' ELSE 'TARDE' END,'Aula de secundaria',true
FROM generate_series(101,112) n
WHERE NOT EXISTS (SELECT 1 FROM aulas a WHERE a.nombre='Secundaria '||n);

INSERT INTO secciones(nombre,grado_id,capacidad_maxima,turno,anio_escolar,activa)
SELECT letra,g.id,35,CASE WHEN letra='A' THEN 'MANANA' ELSE 'TARDE' END,2026,true
FROM grados g JOIN niveles n ON n.id=g.nivel_id
CROSS JOIN (VALUES ('A'),('B')) l(letra)
WHERE n.nombre='SECUNDARIA'
AND NOT EXISTS (SELECT 1 FROM secciones s WHERE s.nombre=letra AND s.grado_id=g.id AND s.anio_escolar=2026);

WITH tutores(grado,seccion,username) AS (VALUES
 ('PRIMERO','A','docente1'),('SEGUNDO','A','docente2'),('TERCERO','A','docente.sociales.m'),
 ('CUARTO','A','docente.dpcc.m'),('QUINTO','A','docente.ept.m'),
 ('PRIMERO','B','docente.matematica.t'),('SEGUNDO','B','docente.comunicacion.t'),
 ('TERCERO','B','docente.sociales.t'),('CUARTO','B','docente.dpcc.t'),('QUINTO','B','docente.ept.t'))
UPDATE secciones s SET docente_tutor_id=d.id
FROM grados g,niveles n,tutores t,docentes d
WHERE s.grado_id=g.id AND g.nivel_id=n.id AND n.nombre='SECUNDARIA'
  AND s.anio_escolar=2026 AND g.nombre=t.grado AND s.nombre=t.seccion
  AND lower(d.username)=lower(t.username);

WITH materias(prefijo,nombre,horas) AS (VALUES
 ('MAT','Matemática',5),('COM','Comunicación',5),('ING','Inglés',2),
 ('CYT','Ciencia y Tecnología',4),('SOC','Ciencias Sociales',3),
 ('DPC','Desarrollo Personal, Ciudadanía y Cívica',2),('EPT','Educación para el Trabajo',2),
 ('EFI','Educación Física',3),('ART','Arte y Cultura',2),('REL','Educación Religiosa',2),
 ('TUT','Tutoría y Orientación Educativa',1),('QUE','Quechua',1),
 ('TIC','Taller de Competencias Digitales',1),('RAZ','Razonamiento Matemático',1),('LEC','Plan Lector',1)),
grados_num(nombre,numero) AS (VALUES
 ('PRIMERO','1'),('SEGUNDO','2'),('TERCERO','3'),('CUARTO','4'),('QUINTO','5'))
INSERT INTO cursos(codigo,nombre,creditos,tipo,grado_id,estado)
SELECT m.prefijo||'-S'||gn.numero,m.nombre,m.horas,
       CASE WHEN m.prefijo IN ('TIC','RAZ','LEC') THEN 'TALLER' ELSE 'OBLIGATORIO' END,g.id,'ACTIVO'
FROM grados g JOIN niveles n ON n.id=g.nivel_id AND n.nombre='SECUNDARIA'
JOIN grados_num gn ON gn.nombre=g.nombre CROSS JOIN materias m
ON CONFLICT (codigo) DO UPDATE SET nombre=EXCLUDED.nombre,creditos=EXCLUDED.creditos,
tipo=EXCLUDED.tipo,grado_id=EXCLUDED.grado_id,estado='ACTIVO';

\copy (SELECT h.id FROM horarios h JOIN secciones s ON s.id=h.seccion_id JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='PRIMARIA' AND s.anio_escolar=2026 ORDER BY h.id) TO '/tmp/primaria_horarios_retirados.csv' WITH CSV
DELETE FROM horarios h
USING secciones s,grados g,niveles n
WHERE h.seccion_id=s.id AND s.grado_id=g.id AND g.nivel_id=n.id
  AND n.nombre='PRIMARIA' AND s.anio_escolar=2026;
UPDATE asignaciones_academicas a SET activa=false
FROM secciones s,grados g,niveles n
WHERE a.seccion_id=s.id AND s.grado_id=g.id AND g.nivel_id=n.id
  AND n.nombre='PRIMARIA' AND a.anio_escolar=2026;

DELETE FROM asignaciones_academicas a
USING secciones s,grados g,niveles n
WHERE a.seccion_id=s.id AND s.grado_id=g.id AND g.nivel_id=n.id
  AND n.nombre='SECUNDARIA' AND a.anio_escolar=2026;

WITH mapa(prefijo,turno,username) AS (VALUES
 ('MAT','MANANA','docente1'),('COM','MANANA','docente2'),('ING','MANANA','docente.ingles.m'),
 ('CYT','MANANA','coord.academica'),('SOC','MANANA','docente.sociales.m'),('DPC','MANANA','docente.dpcc.m'),
 ('EPT','MANANA','docente.ept.m'),('EFI','MANANA','docente.efisica.m'),('ART','MANANA','docente.arte.m'),
 ('REL','MANANA','docente.religion.m'),('TUT','MANANA','docente.tutoria.m'),('QUE','MANANA','docente.quechua.m'),
 ('TIC','MANANA','docente.tic.m'),('RAZ','MANANA','docente.razonamiento.m'),('LEC','MANANA','docente.lector.m'),
 ('MAT','TARDE','docente.matematica.t'),('COM','TARDE','docente.comunicacion.t'),('ING','TARDE','docente.ingles.t'),
 ('CYT','TARDE','docente.cyt.t'),('SOC','TARDE','docente.sociales.t'),('DPC','TARDE','docente.dpcc.t'),
 ('EPT','TARDE','docente.ept.t'),('EFI','TARDE','docente.efisica.t'),('ART','TARDE','docente.arte.t'),
 ('REL','TARDE','docente.religion.t'),('TUT','TARDE','docente.tutoria.t'),('QUE','TARDE','docente.quechua.t'),
 ('TIC','TARDE','docente.tic.t'),('RAZ','TARDE','docente.razonamiento.t'),('LEC','TARDE','docente.lector.t'))
INSERT INTO asignaciones_academicas(curso_id,docente_id,seccion_id,horas_semanales,anio_escolar,activa)
SELECT c.id,d.id,s.id,c.creditos,2026,true
FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='SECUNDARIA'
JOIN cursos c ON c.grado_id=g.id JOIN mapa m ON c.codigo LIKE m.prefijo||'-%' AND m.turno=s.turno
JOIN docentes d ON lower(d.username)=lower(m.username)
WHERE s.anio_escolar=2026 AND s.activa=true
ON CONFLICT (curso_id,docente_id,seccion_id,anio_escolar)
DO UPDATE SET horas_semanales=EXCLUDED.horas_semanales,activa=true;

\if :reiniciar_horarios
\copy (SELECT h.id FROM horarios h JOIN secciones s ON s.id=h.seccion_id JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND s.anio_escolar=2026 ORDER BY h.id) TO '/tmp/secundaria_horarios_previos.csv' WITH CSV
DELETE FROM horarios h
USING secciones s,grados g,niveles n
WHERE h.seccion_id=s.id AND s.grado_id=g.id AND g.nivel_id=n.id
  AND n.nombre='SECUNDARIA' AND s.anio_escolar=2026;
\endif

\if :cargar_horarios
WITH sec AS (
 SELECT s.*,row_number() OVER (PARTITION BY s.turno ORDER BY g.id,s.nombre)-1 desplazamiento
 FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id
 WHERE n.nombre='SECUNDARIA' AND s.anio_escolar=2026 AND s.activa=true
), sec_aula AS (
 SELECT sec.*,au.id aula_id FROM sec JOIN LATERAL (
  SELECT id FROM aulas WHERE activa=true AND turno_disponible=sec.turno AND nombre LIKE 'Secundaria %'
  ORDER BY id OFFSET sec.desplazamiento::integer LIMIT 1) au ON true
), secuencia(pos,prefijo) AS (VALUES
 (0,'MAT'),(1,'COM'),(2,'CYT'),(3,'SOC'),(4,'EFI'),(5,'ING'),(6,'MAT'),
 (7,'COM'),(8,'CYT'),(9,'SOC'),(10,'EFI'),(11,'DPC'),(12,'MAT'),(13,'COM'),
 (14,'CYT'),(15,'EPT'),(16,'ART'),(17,'REL'),(18,'MAT'),(19,'COM'),(20,'CYT'),
 (21,'SOC'),(22,'EFI'),(23,'ING'),(24,'MAT'),(25,'COM'),(26,'DPC'),(27,'EPT'),
 (28,'ART'),(29,'REL'),(30,'TUT'),(31,'QUE'),(32,'TIC'),(33,'RAZ'),(34,'LEC')),
dias(indice,nombre) AS (VALUES (0,'LUNES'),(1,'MARTES'),(2,'MIERCOLES'),(3,'JUEVES'),(4,'VIERNES')),
bloques(turno,indice,inicio,fin) AS (VALUES
 ('MANANA',0,TIME '07:30',TIME '08:15'),('MANANA',1,TIME '08:15',TIME '09:00'),
 ('MANANA',2,TIME '09:00',TIME '09:45'),('MANANA',3,TIME '10:00',TIME '10:45'),
 ('MANANA',4,TIME '10:45',TIME '11:30'),('MANANA',5,TIME '11:30',TIME '12:15'),
 ('MANANA',6,TIME '12:30',TIME '13:15'),('TARDE',0,TIME '13:30',TIME '14:15'),
 ('TARDE',1,TIME '14:15',TIME '15:00'),('TARDE',2,TIME '15:00',TIME '15:45'),
 ('TARDE',3,TIME '16:00',TIME '16:45'),('TARDE',4,TIME '16:45',TIME '17:30'),
 ('TARDE',5,TIME '17:30',TIME '18:15'),('TARDE',6,TIME '18:30',TIME '19:15')),
slots AS (SELECT generate_series(0,34) pos),
mapa(prefijo,turno,username) AS (VALUES
 ('MAT','MANANA','docente1'),('COM','MANANA','docente2'),('ING','MANANA','docente.ingles.m'),
 ('CYT','MANANA','coord.academica'),('SOC','MANANA','docente.sociales.m'),('DPC','MANANA','docente.dpcc.m'),
 ('EPT','MANANA','docente.ept.m'),('EFI','MANANA','docente.efisica.m'),('ART','MANANA','docente.arte.m'),
 ('REL','MANANA','docente.religion.m'),('TUT','MANANA','docente.tutoria.m'),('QUE','MANANA','docente.quechua.m'),
 ('TIC','MANANA','docente.tic.m'),('RAZ','MANANA','docente.razonamiento.m'),('LEC','MANANA','docente.lector.m'),
 ('MAT','TARDE','docente.matematica.t'),('COM','TARDE','docente.comunicacion.t'),('ING','TARDE','docente.ingles.t'),
 ('CYT','TARDE','docente.cyt.t'),('SOC','TARDE','docente.sociales.t'),('DPC','TARDE','docente.dpcc.t'),
 ('EPT','TARDE','docente.ept.t'),('EFI','TARDE','docente.efisica.t'),('ART','TARDE','docente.arte.t'),
 ('REL','TARDE','docente.religion.t'),('TUT','TARDE','docente.tutoria.t'),('QUE','TARDE','docente.quechua.t'),
 ('TIC','TARDE','docente.tic.t'),('RAZ','TARDE','docente.razonamiento.t'),('LEC','TARDE','docente.lector.t'))
INSERT INTO horarios(curso_id,docente_id,seccion_id,aula_id,dia_semana,hora_inicio,hora_fin,turno)
SELECT c.id,doc.id,s.id,s.aula_id,di.nombre,b.inicio,b.fin,s.turno
FROM sec_aula s CROSS JOIN slots sl JOIN secuencia seq ON seq.pos=((sl.pos+s.desplazamiento)%35)
JOIN dias di ON di.indice=(sl.pos/7) JOIN bloques b ON b.turno=s.turno AND b.indice=(sl.pos%7)
JOIN cursos c ON c.grado_id=s.grado_id AND c.codigo LIKE seq.prefijo||'-%'
JOIN mapa m ON m.prefijo=seq.prefijo AND m.turno=s.turno JOIN docentes doc ON lower(doc.username)=lower(m.username)
WHERE NOT EXISTS (SELECT 1 FROM horarios h WHERE h.seccion_id=s.id AND h.dia_semana=di.nombre AND h.hora_inicio=b.inicio);
\endif

COMMIT;

SELECT s.id sec_1a,g.id grado_1 FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND g.nombre='PRIMERO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id sec_1b FROM secciones s WHERE s.grado_id=:grado_1 AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id sec_2a,g.id grado_2 FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND g.nombre='SEGUNDO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id sec_2b FROM secciones s WHERE s.grado_id=:grado_2 AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id sec_3a,g.id grado_3 FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND g.nombre='TERCERO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id sec_3b FROM secciones s WHERE s.grado_id=:grado_3 AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id sec_4a,g.id grado_4 FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND g.nombre='CUARTO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id sec_4b FROM secciones s WHERE s.grado_id=:grado_4 AND s.nombre='B' AND s.anio_escolar=2026 \gset
SELECT s.id sec_5a,g.id grado_5 FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE n.nombre='SECUNDARIA' AND g.nombre='QUINTO' AND s.nombre='A' AND s.anio_escolar=2026 \gset
SELECT s.id sec_5b FROM secciones s WHERE s.grado_id=:grado_5 AND s.nombre='B' AND s.anio_escolar=2026 \gset

\copy (SELECT s.id,g.id,cm.id,am.docente_id,dm.username,cc.id,ac.docente_id,dc.username,hm.id,s.turno FROM secciones s JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id AND n.nombre='SECUNDARIA' JOIN cursos cm ON cm.grado_id=g.id AND cm.codigo LIKE 'MAT-%' JOIN asignaciones_academicas am ON am.curso_id=cm.id AND am.seccion_id=s.id AND am.anio_escolar=2026 AND am.activa JOIN docentes dm ON dm.id=am.docente_id JOIN cursos cc ON cc.grado_id=g.id AND cc.codigo LIKE 'COM-%' JOIN asignaciones_academicas ac ON ac.curso_id=cc.id AND ac.seccion_id=s.id AND ac.anio_escolar=2026 AND ac.activa JOIN docentes dc ON dc.id=ac.docente_id LEFT JOIN LATERAL (SELECT h.id FROM horarios h WHERE h.seccion_id=s.id AND h.curso_id=cm.id ORDER BY h.id LIMIT 1) hm ON true WHERE s.anio_escolar=2026 AND s.activa ORDER BY g.id,s.nombre) TO '/tmp/secundaria_academica.csv' WITH CSV

\connect academico_estudiantes
BEGIN;
WITH destinos(ord,grado_id,seccion_id) AS (VALUES
 (1,:grado_1,:sec_1a),(2,:grado_1,:sec_1b),(3,:grado_2,:sec_2a),(4,:grado_2,:sec_2b),
 (5,:grado_3,:sec_3a),(6,:grado_3,:sec_3b),(7,:grado_4,:sec_4a),(8,:grado_4,:sec_4b),
 (9,:grado_5,:sec_5a),(10,:grado_5,:sec_5b)),
personas AS (
 SELECT d.*,((d.ord-1)*:'estudiantes_por_seccion'::integer+n) idx
 FROM destinos d CROSS JOIN generate_series(1,:'estudiantes_por_seccion'::integer) n),
datos AS (
 SELECT idx,grado_id,seccion_id,
 (ARRAY['Adriana','Bruno','Carla','Diego','Elena','Fabio','Gabriela','Hugo','Isabel','Joaquín','Kiara','Leonardo','Mariana','Nicolás','Olivia','Paolo','Renata','Santiago','Valeria','Ximena'])[((idx-1)%20)+1] nombre,
 (ARRAY['Quispe','Huamán','Flores','Condori','Palomino','Ccahuana','Sullca','Mamani','Ayala','Puma'])[((idx-1)%10)+1]
 ||' '||(ARRAY['Rojas','Vargas','Castro','Soto','Torres','Mendoza','Luna','Paredes','Cárdenas','León'])[((idx+2)%10)+1] apellido FROM personas)
INSERT INTO estudiantes(nombre,apellido,dni,email,telefono,grado_id,seccion_id,estado)
SELECT nombre,apellido,'76'||lpad(idx::text,6,'0'),lower('sec'||idx||'@estudiante.iephuamanga.edu.pe'),
       '9663'||lpad(idx::text,5,'0'),grado_id,seccion_id,'ACTIVO' FROM datos
ON CONFLICT (dni) DO UPDATE SET grado_id=EXCLUDED.grado_id,seccion_id=EXCLUDED.seccion_id,estado='ACTIVO';

UPDATE estudiantes
SET estado='INACTIVO'
WHERE email ~ '^sec[0-9]+@estudiante\.iephuamanga\.edu\.pe$'
  AND substring(email from '^sec([0-9]+)@')::integer > 10 * :'estudiantes_por_seccion'::integer;

WITH nums AS (SELECT generate_series(1,10 * :'estudiantes_por_seccion'::integer) idx)
INSERT INTO apoderados(nombre,apellido,dni,email,telefono)
SELECT (ARRAY['María','José','Rosa','Juan','Carmen','Luis','Patricia','Jorge','Ana','Miguel'])[((idx-1)%10)+1],
       (ARRAY['Quispe','Huamán','Flores','Condori','Palomino','Ccahuana','Sullca','Mamani','Ayala','Puma'])[((idx-1)%10)+1]||' Familia',
       '56'||lpad(idx::text,6,'0'),'familia'||idx||'@correo.pe','9654'||lpad(idx::text,5,'0') FROM nums
ON CONFLICT (dni) DO UPDATE SET telefono=EXCLUDED.telefono;

INSERT INTO apoderado_estudiante(apoderado_id,estudiante_id,parentesco,permiso_retiro)
SELECT a.id,e.id,CASE WHEN mod(substring(a.dni,3)::integer,2)=0 THEN 'MADRE' ELSE 'PADRE' END,true
FROM apoderados a JOIN estudiantes e ON substring(a.dni,3)=substring(e.dni,3)
WHERE a.dni LIKE '56%' AND e.dni LIKE '76%'
ON CONFLICT (apoderado_id,estudiante_id) DO UPDATE SET permiso_retiro=true;

WITH alumnos AS (
 SELECT e.id estudiante_id,a.id apoderado_id,
        substring(e.email from '^sec([0-9]+)@')::integer idx
 FROM estudiantes e
 JOIN apoderados a ON substring(a.dni,3)=substring(e.dni,3)
 WHERE e.email ~ '^sec[0-9]+@estudiante\.iephuamanga\.edu\.pe$'
   AND substring(e.email from '^sec([0-9]+)@')::integer <= 10 * :'estudiantes_por_seccion'::integer
), finalidades(finalidad) AS (VALUES ('DATOS_ACADEMICOS'),('SALUD'),('COMUNICACIONES'))
INSERT INTO consentimientos_datos(estudiante_id,apoderado_id,finalidad,accion,version_politica,
                                  evidencia,registrado_por,registrado_en)
SELECT a.estudiante_id,a.apoderado_id,f.finalidad,
       CASE WHEN f.finalidad='COMUNICACIONES' AND mod(a.idx,10)=0 THEN 'DENEGADO' ELSE 'OTORGADO' END,
       '2026-01','Ficha de matrícula de demostración','seed-demo-secundaria',CURRENT_TIMESTAMP
FROM alumnos a CROSS JOIN finalidades f
WHERE NOT EXISTS (
 SELECT 1 FROM consentimientos_datos c
 WHERE c.estudiante_id=a.estudiante_id AND c.finalidad=f.finalidad
   AND c.registrado_por='seed-demo-secundaria');
COMMIT;

\copy (SELECT id,grado_id,seccion_id,substring(email from '^sec([0-9]+)@')::integer FROM estudiantes WHERE estado='ACTIVO' AND email ~ '^sec[0-9]+@estudiante\.iephuamanga\.edu\.pe$' ORDER BY id) TO '/tmp/secundaria_estudiantes.csv' WITH CSV

\connect academico_evaluaciones
BEGIN;
CREATE TEMP TABLE demo_sec_academica(
 seccion_id BIGINT,grado_id BIGINT,curso_mat_id BIGINT,docente_mat_id BIGINT,docente_mat_username VARCHAR(100),
 curso_com_id BIGINT,docente_com_id BIGINT,docente_com_username VARCHAR(100),horario_mat_id BIGINT,turno VARCHAR(20));
CREATE TEMP TABLE demo_sec_estudiantes(estudiante_id BIGINT,grado_id BIGINT,seccion_id BIGINT,indice INTEGER);
\copy demo_sec_academica FROM '/tmp/secundaria_academica.csv' WITH CSV
\copy demo_sec_estudiantes FROM '/tmp/secundaria_estudiantes.csv' WITH CSV
CREATE TEMP TABLE demo_horarios_primaria_retirados(horario_id BIGINT);
\copy demo_horarios_primaria_retirados FROM '/tmp/primaria_horarios_retirados.csv' WITH CSV
DELETE FROM asistencias WHERE horario_id IN (SELECT horario_id FROM demo_horarios_primaria_retirados);
DELETE FROM sesiones_asistencia WHERE horario_id IN (SELECT horario_id FROM demo_horarios_primaria_retirados);
\if :reiniciar_horarios
CREATE TEMP TABLE demo_horarios_previos(horario_id BIGINT);
\copy demo_horarios_previos FROM '/tmp/secundaria_horarios_previos.csv' WITH CSV
DELETE FROM asistencias WHERE horario_id IN (SELECT horario_id FROM demo_horarios_previos);
DELETE FROM sesiones_asistencia WHERE horario_id IN (SELECT horario_id FROM demo_horarios_previos);
DELETE FROM asistencias WHERE registrado_por='seed-demo-secundaria';
DELETE FROM sesiones_asistencia WHERE abierta_por='seed-demo-secundaria';
\endif

WITH materias AS (
 SELECT e.estudiante_id,e.indice,a.seccion_id,m.curso_id,m.docente_id,m.competencia
 FROM demo_sec_estudiantes e JOIN demo_sec_academica a ON a.seccion_id=e.seccion_id
 CROSS JOIN LATERAL (VALUES
   (a.curso_mat_id,a.docente_mat_id,'Resuelve problemas de cantidad'),
   (a.curso_com_id,a.docente_com_id,'Lee diversos tipos de textos')) m(curso_id,docente_id,competencia))
INSERT INTO notas(estudiante_id,curso_id,docente_id,seccion_id,bimestre,competencia,valor_numerico,
                  escala_literal,observaciones,periodo_anio,fecha_registro)
SELECT estudiante_id,curso_id,docente_id,seccion_id,'BIMESTRE_1',competencia,
       11 + mod(indice+curso_id::integer,10),
       CASE WHEN 11 + mod(indice+curso_id::integer,10) >= 18 THEN 'AD'
            WHEN 11 + mod(indice+curso_id::integer,10) >= 14 THEN 'A' ELSE 'B' END,
       'Registro generado para la demostración de secundaria','2026',CURRENT_TIMESTAMP
FROM materias m
WHERE NOT EXISTS (SELECT 1 FROM notas n WHERE n.estudiante_id=m.estudiante_id
  AND n.curso_id=m.curso_id AND n.bimestre='BIMESTRE_1' AND n.competencia=m.competencia);

WITH fechas(fecha,orden) AS (VALUES
 (DATE '2026-09-21',1),(DATE '2026-09-22',2),(DATE '2026-09-23',3),
 (DATE '2026-09-24',4),(DATE '2026-09-25',5)), registros AS (
 SELECT e.estudiante_id,e.indice,a.*,f.fecha,f.orden,
        CASE WHEN mod(e.indice+f.orden,19)=0 THEN 'FALTA'
             WHEN mod(e.indice+f.orden,13)=0 THEN 'TARDE' ELSE 'PRESENTE' END estado
 FROM demo_sec_estudiantes e JOIN demo_sec_academica a ON a.seccion_id=e.seccion_id
 CROSS JOIN fechas f WHERE a.horario_mat_id IS NOT NULL)
INSERT INTO asistencias(estudiante_id,horario_id,seccion_id,curso_id,docente_id,fecha,estado,turno,
                        justificacion,registrado_por,fecha_registro)
SELECT estudiante_id,horario_mat_id,seccion_id,curso_mat_id,docente_mat_id,fecha,estado,turno,
       CASE WHEN estado='FALTA' THEN 'Pendiente de justificación' ELSE NULL END,
       'seed-demo-secundaria',CURRENT_TIMESTAMP
FROM registros r ON CONFLICT (estudiante_id,horario_id,fecha) DO NOTHING;

INSERT INTO sesiones_asistencia(horario_id,seccion_id,curso_id,docente_id,fecha,estado,abierta_por,
                                cerrada_por,fecha_apertura,fecha_cierre)
SELECT a.horario_mat_id,a.seccion_id,a.curso_mat_id,a.docente_mat_id,f.fecha,'CERRADA',
       'seed-demo-secundaria','seed-demo-secundaria',
       f.fecha + CASE WHEN a.turno='MANANA' THEN TIME '07:25' ELSE TIME '13:25' END,
       f.fecha + CASE WHEN a.turno='MANANA' THEN TIME '08:20' ELSE TIME '14:20' END
FROM demo_sec_academica a CROSS JOIN (VALUES
 (DATE '2026-09-21'),(DATE '2026-09-22'),(DATE '2026-09-23'),
 (DATE '2026-09-24'),(DATE '2026-09-25')) f(fecha)
WHERE a.horario_mat_id IS NOT NULL ON CONFLICT (horario_id,fecha) DO NOTHING;

INSERT INTO bimestres_estado(anio,bimestre,estado,actualizado_por,actualizado_en,version)
SELECT 2026,b,'ABIERTO','seed-demo-secundaria',CURRENT_TIMESTAMP,0
FROM (VALUES ('BIMESTRE_1'),('BIMESTRE_2'),('BIMESTRE_3'),('BIMESTRE_4')) periodos(b)
ON CONFLICT (anio,bimestre) DO NOTHING;
COMMIT;

\connect academico_disciplina
BEGIN;
CREATE TEMP TABLE demo_sec_academica(
 seccion_id BIGINT,grado_id BIGINT,curso_mat_id BIGINT,docente_mat_id BIGINT,docente_mat_username VARCHAR(100),
 curso_com_id BIGINT,docente_com_id BIGINT,docente_com_username VARCHAR(100),horario_mat_id BIGINT,turno VARCHAR(20));
CREATE TEMP TABLE demo_sec_estudiantes(estudiante_id BIGINT,grado_id BIGINT,seccion_id BIGINT,indice INTEGER);
\copy demo_sec_academica FROM '/tmp/secundaria_academica.csv' WITH CSV
\copy demo_sec_estudiantes FROM '/tmp/secundaria_estudiantes.csv' WITH CSV

WITH seleccionados AS (
 SELECT e.*,a.docente_mat_id,a.docente_mat_username,row_number() OVER (ORDER BY e.indice) fila
 FROM demo_sec_estudiantes e JOIN demo_sec_academica a ON a.seccion_id=e.seccion_id)
INSERT INTO reportes_disciplinarios(estudiante_id,docente_id,seccion_id,tipo,descripcion,observaciones,
                                    estado,apoderado_notificado,fecha_reporte,fecha_notificacion,registrado_por)
SELECT estudiante_id,docente_mat_id,seccion_id,
       CASE WHEN mod(fila,4)=0 THEN 'MERITO' WHEN mod(fila,3)=0 THEN 'TARDANZA' ELSE 'FALTA_LEVE' END,
       CASE WHEN mod(fila,4)=0 THEN 'Participación destacada en una actividad colaborativa'
            WHEN mod(fila,3)=0 THEN 'Ingreso después del inicio de la sesión'
            ELSE 'Interrupción reiterada durante la clase' END,
       'Caso sintético para la demostración académica',
       CASE WHEN mod(fila,2)=0 THEN 'NOTIFICADO' ELSE 'EMITIDO' END,mod(fila,2)=0,
       TIMESTAMP '2026-09-26 10:00:00' - fila * INTERVAL '1 hour',
       CASE WHEN mod(fila,2)=0 THEN TIMESTAMP '2026-09-26 10:05:00' - fila * INTERVAL '1 hour' ELSE NULL END,
       docente_mat_username
FROM seleccionados s WHERE mod(fila,10)=0
  AND NOT EXISTS (SELECT 1 FROM reportes_disciplinarios r WHERE r.estudiante_id=s.estudiante_id
    AND r.descripcion IN ('Participación destacada en una actividad colaborativa',
                          'Ingreso después del inicio de la sesión','Interrupción reiterada durante la clase'));
COMMIT;

\connect academico_reportes
INSERT INTO actividades(tipo,texto,origen,fecha)
SELECT 'MATRICULA','Se cargó el padrón de secundaria para la demostración','seed-demo-secundaria',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM actividades WHERE origen='seed-demo-secundaria' AND tipo='MATRICULA');
