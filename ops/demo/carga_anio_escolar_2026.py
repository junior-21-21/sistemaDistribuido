"""
Carga de datos de prueba: el año escolar 2026 de la I.E.P. Huamanga desde marzo hasta hoy.

Simula cómo funciona un colegio público de secundaria con los estudiantes, secciones, carga docente
y horarios que ya existen:
  * Calendario: inicio 16 de marzo; I bimestre (16/03-15/05) y II bimestre (25/05-24/07) cerrados;
    III bimestre en curso (10/08-16/10); vacaciones 18-22/05 y 27/07-07/08; feriados nacionales.
  * Familias: un apoderado por estudiante (los hermanos comparten), vínculo y consentimientos de datos.
  * Ingreso al colegio con DNI cada día lectivo (puntual, tardanza o ausencia) hasta ayer.
  * Asistencia por sesión de clase del III bimestre, coherente con el ingreso (si no entró: falta).
  * Notas por competencias (AD/A/B/C) del I y II bimestre completas y del III en avance, con pesos.
  * Convivencia (tardanzas reiteradas, faltas, méritos), avisos a las familias, fichas de salud,
    atenciones de tópico, derivaciones a psicología, tickets de mantenimiento y actividad del mural.
  * Estados de bimestre: I y II cerrados por Dirección (con auditoría), III y IV abiertos.

Uso (desde la raíz del proyecto, con Docker en marcha):
    python ops/demo/carga_anio_escolar_2026.py <carpeta_salida> --deshacer   (revierte la carga)
    python ops/demo/carga_anio_escolar_2026.py <carpeta_salida> [--aplicar]

Sin --aplicar solo genera los .sql. Todo lo cargado lleva la marca «carga-anual-2026» donde la
tabla tiene un campo de autor, y el script deshacer_carga_anio_2026.sql (generado junto a los .sql)
elimina exactamente lo insertado.
"""
import csv
import io
import math
import random
import subprocess
import sys
import uuid
from datetime import date, datetime, time, timedelta
from pathlib import Path

CONTENEDOR = 'pad-postgres'
MARCA = 'carga-anual-2026'
ANIO = 2026
HOY = date(2026, 10, 15)
random.seed(20260316)

if '--deshacer' in sys.argv:
    # Ejecuta el deshacer_carga_anio_2026.sql generado en la carpeta de salida, base por base
    texto = (Path(sys.argv[1]) / 'deshacer_carga_anio_2026.sql').read_text(encoding='utf-8')
    for bloque in texto.split('\n-- \\c ')[1:]:
        base, sentencias = bloque.split('\n', 1)
        r = subprocess.run(['docker', 'exec', '-i', CONTENEDOR, 'psql', '-U', 'postgres', '-d', base.strip(),
                            '-v', 'ON_ERROR_STOP=1', '-1', '-q'], input=sentencias.encode('utf-8'), capture_output=True)
        print(base.strip(), 'deshecho' if r.returncode == 0 else r.stderr.decode('utf-8', 'replace'))
    sys.exit(0)

# ------------------------------------------------------------------ calendario escolar
INICIO = date(2026, 3, 16)
BIMESTRES = {
    'I BIMESTRE': (date(2026, 3, 16), date(2026, 5, 15)),
    'II BIMESTRE': (date(2026, 5, 25), date(2026, 7, 24)),
    'III BIMESTRE': (date(2026, 8, 10), date(2026, 10, 16)),
    'IV BIMESTRE': (date(2026, 10, 26), date(2026, 12, 18)),
}
VACACIONES = [(date(2026, 5, 18), date(2026, 5, 22)), (date(2026, 7, 27), date(2026, 8, 7))]
FERIADOS = {date(2026, 4, 2), date(2026, 4, 3), date(2026, 5, 1), date(2026, 6, 29), date(2026, 7, 6),
            date(2026, 7, 23), date(2026, 10, 8)}
DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO']


def es_lectivo(d):
    return (d.weekday() < 5 and d not in FERIADOS
            and not any(a <= d <= b for a, b in VACACIONES))


DIAS_LECTIVOS = [INICIO + timedelta(n) for n in range((HOY - INICIO).days) if es_lectivo(INICIO + timedelta(n))]


def bimestre_de(d):
    return next((b for b, (a, z) in BIMESTRES.items() if a <= d <= z), None)


# ------------------------------------------------------------------ acceso a datos
def consultar(db, sql):
    salida = subprocess.run(['docker', 'exec', CONTENEDOR, 'psql', '-U', 'postgres', '-d', db, '-c',
                             f'COPY ({sql}) TO STDOUT WITH CSV'], capture_output=True, check=True)
    return list(csv.reader(io.StringIO(salida.stdout.decode('utf-8'))))


def nulo(v):
    if v is None:
        return r'\N'
    if isinstance(v, bool):
        return 't' if v else 'f'
    s = str(v)
    return s.replace('\\', '\\\\').replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')


def bloque_copy(tabla, columnas, filas):
    lineas = [f'COPY {tabla} ({", ".join(columnas)}) FROM STDIN;']
    lineas += ['\t'.join(nulo(v) for v in fila) for fila in filas]
    lineas.append('\\.')
    return '\n'.join(lineas) + '\n'


def ts(d, h, m=0, s=0):
    return datetime.combine(d, time(h, m, s)).strftime('%Y-%m-%d %H:%M:%S')


# ------------------------------------------------------------------ datos existentes
print('Leyendo datos del sistema…')
secciones = {int(r[0]): {'grado_id': int(r[1]), 'grado': r[2], 'nombre': r[3], 'turno': r[4]}
             for r in consultar('academico_cursos',
                                'select s.id, g.id, g.nombre, s.nombre, s.turno from secciones s '
                                'join grados g on g.id = s.grado_id where s.activa')}
cursos = {int(r[0]): {'nombre': r[1], 'grado_id': int(r[2]) if r[2] else None}
          for r in consultar('academico_cursos', 'select id, nombre, grado_id from cursos')}
docentes = {int(r[0]): {'username': r[1] or f'docente{r[0]}', 'nombre': r[2], 'apellido': r[3]}
            for r in consultar('academico_cursos', 'select id, username, nombre, apellido from docentes')}
asignaciones = [(int(r[0]), int(r[1]), int(r[2])) for r in consultar(
    'academico_cursos', f'select seccion_id, curso_id, docente_id from asignaciones_academicas '
                        f'where activa and anio_escolar = {ANIO}')]
horarios = [{'id': int(r[0]), 'seccion': int(r[1]), 'dia': r[2], 'inicio': r[3], 'curso': int(r[4]),
             'docente': int(r[5]), 'turno': r[6]}
            for r in consultar('academico_cursos',
                               "select id, seccion_id, dia_semana, hora_inicio, curso_id, docente_id, turno "
                               "from horarios where hora_fin - hora_inicio = interval '45 minutes' "
                               "and curso_id is not null and docente_id is not null")]
aulas = [int(r[0]) for r in consultar('academico_cursos', 'select id from aulas where activa')]
estudiantes = [{'id': int(r[0]), 'nombre': r[1], 'apellido': r[2], 'dni': r[3], 'seccion': int(r[4])}
               for r in consultar('academico_estudiantes',
                                  "select id, nombre, apellido, dni, seccion_id from estudiantes "
                                  "where estado = 'ACTIVO' and seccion_id is not null order by id")]
estudiantes = [e for e in estudiantes if e['seccion'] in secciones]
con_apoderado = {int(r[0]) for r in consultar('academico_estudiantes', 'select estudiante_id from apoderado_estudiante')}
dnis_apoderados = {r[0] for r in consultar('academico_estudiantes', 'select dni from apoderados')}
con_ficha = {int(r[0]) for r in consultar('academico_estudiantes', 'select estudiante_id from fichas_medicas')}
notas_existentes = {(int(r[0]), int(r[1]), r[2], r[3].strip().lower()) for r in consultar(
    'academico_evaluaciones',
    "select estudiante_id, curso_id, case bimestre when 'BIMESTRE_1' then 'I BIMESTRE' "
    "when 'BIMESTRE_2' then 'II BIMESTRE' when 'BIMESTRE_3' then 'III BIMESTRE' "
    "when 'BIMESTRE_4' then 'IV BIMESTRE' else bimestre end, competencia from notas")}
pesos_existentes = {(int(r[0]), int(r[1]), r[2]) for r in consultar(
    'academico_evaluaciones', 'select seccion_id, curso_id, bimestre from ponderaciones_competencia')}
sesiones_existentes = {(int(r[0]), r[1]) for r in consultar('academico_evaluaciones',
                                                             'select horario_id, fecha from sesiones_asistencia')}
print(f'  {len(estudiantes)} estudiantes, {len(secciones)} secciones, {len(asignaciones)} asignaciones, '
      f'{len(horarios)} bloques de 45 min, {len(DIAS_LECTIVOS)} días lectivos hasta {HOY - timedelta(1)}')

por_seccion = {}
for e in estudiantes:
    por_seccion.setdefault(e['seccion'], []).append(e)

# Perfil de cada estudiante: habilidad académica, puntualidad y asistencia
for e in estudiantes:
    r = random.random()
    e['riesgo'] = r < 0.03                      # ~3 % con inasistencia crónica (alerta de deserción)
    e['ausencia'] = random.uniform(0.15, 0.30) if e['riesgo'] else random.choice([0.01, 0.02, 0.03, 0.04, 0.06])
    e['tardanza'] = random.choice([0.02, 0.04, 0.06, 0.09, 0.15]) if random.random() < 0.8 else 0.25
    e['habilidad'] = random.gauss(-0.6 if e['riesgo'] else 0.3, 0.8)
    e['areas'] = {}

# ------------------------------------------------------------------ competencias del CNEB
COMPETENCIAS = {
    'Matemática': ['Resuelve problemas de cantidad', 'Resuelve problemas de regularidad, equivalencia y cambio',
                   'Resuelve problemas de forma, movimiento y localización',
                   'Resuelve problemas de gestión de datos e incertidumbre'],
    'Comunicación': ['Se comunica oralmente en su lengua materna', 'Lee diversos tipos de textos',
                     'Escribe diversos tipos de textos'],
    'Inglés': ['Se comunica oralmente en inglés', 'Lee diversos tipos de textos en inglés',
               'Escribe diversos tipos de textos en inglés'],
    'Inglés como Lengua Extranjera': ['Se comunica oralmente en inglés', 'Lee diversos tipos de textos en inglés',
                                      'Escribe diversos tipos de textos en inglés'],
    'Ciencia y Tecnología': ['Indaga mediante métodos científicos',
                             'Explica el mundo físico basándose en conocimientos científicos',
                             'Diseña y construye soluciones tecnológicas'],
    'Ciencias Sociales': ['Construye interpretaciones históricas', 'Gestiona responsablemente el espacio y el ambiente',
                          'Gestiona responsablemente los recursos económicos'],
    'Desarrollo Personal, Ciudadanía y Cívica': ['Construye su identidad', 'Convive y participa democráticamente'],
    'Educación Física': ['Se desenvuelve de manera autónoma a través de su motricidad', 'Asume una vida saludable',
                         'Interactúa a través de sus habilidades sociomotrices'],
    'Arte y Cultura': ['Aprecia de manera crítica manifestaciones artístico-culturales',
                       'Crea proyectos desde los lenguajes artísticos'],
    'Educación Religiosa': ['Construye su identidad como persona humana amada por Dios',
                            'Asume la experiencia del encuentro personal y comunitario con Dios'],
    'Educación para el Trabajo': ['Gestiona proyectos de emprendimiento económico o social'],
    'Quechua': ['Se comunica oralmente en quechua', 'Lee diversos tipos de textos en quechua'],
    'Castellano como Segunda Lengua': ['Se comunica oralmente en castellano', 'Lee diversos tipos de textos en castellano'],
    'Plan Lector': ['Lee diversos tipos de textos'],
    'Razonamiento Matemático': ['Resuelve problemas de cantidad'],
    'Taller de Competencias Digitales': ['Se desenvuelve en entornos virtuales generados por las TIC'],
}


def nivel(z):
    if z > 1.35:
        return 'AD'
    if z > -0.15:
        return 'A'
    if z > -1.3:
        return 'B'
    return 'C'


# ------------------------------------------------------------------ nombres para familias
NOMBRES_M = ['María', 'Rosa', 'Carmen', 'Juana', 'Luz', 'Elena', 'Gladys', 'Marisol', 'Nancy', 'Yolanda', 'Ruth',
             'Flor', 'Maritza', 'Sonia', 'Hilda', 'Lidia', 'Norma', 'Delia', 'Silvia', 'Teresa']
NOMBRES_H = ['Juan', 'José', 'Carlos', 'Luis', 'Pedro', 'Jorge', 'Víctor', 'Hugo', 'Raúl', 'Alberto', 'Mario',
             'Walter', 'Edwin', 'Freddy', 'Julio', 'César', 'Teodoro', 'Félix', 'Roberto', 'Óscar']
APELLIDOS = ['Quispe', 'Huamán', 'Mamani', 'Flores', 'Ccahuana', 'Gutiérrez', 'Palomino', 'Rojas', 'Cárdenas',
             'Pariona', 'Huamaní', 'Tineo', 'Prado', 'Sulca', 'Bautista', 'Mendoza', 'Ayala', 'Conde', 'Yupanqui', 'Vílchez']


def celular():
    return '9' + ''.join(random.choice('0123456789') for _ in range(8))


# ================================================================== academico_estudiantes
sql_est = ['BEGIN;', "SET client_encoding = 'UTF8';"]
apoderados, vinculos, familias = [], [], {}
siguiente_dni = 45100000
for e in estudiantes:
    if e['id'] in con_apoderado:
        continue
    apellidos = e['apellido'].split()
    padre_ap, madre_ap = (apellidos + [random.choice(APELLIDOS)] * 2)[:2]
    # Hermanos: mismo par de apellidos en la misma familia (aprox. 8 %)
    clave = (padre_ap, madre_ap)
    if clave in familias and random.random() < 0.35:
        dni, parentesco = familias[clave]
    else:
        parentesco = random.choices(['MADRE', 'PADRE', 'ABUELO', 'TUTOR'], [60, 30, 6, 4])[0]
        while str(siguiente_dni) in dnis_apoderados:
            siguiente_dni += 1
        dni = str(siguiente_dni)
        siguiente_dni += 1
        if parentesco == 'MADRE':
            nombre, apellido = random.choice(NOMBRES_M), f'{madre_ap} {random.choice(APELLIDOS)}'
        elif parentesco == 'PADRE':
            nombre, apellido = random.choice(NOMBRES_H), f'{padre_ap} {random.choice(APELLIDOS)}'
        else:
            nombre, apellido = random.choice(NOMBRES_M + NOMBRES_H), f'{padre_ap} {random.choice(APELLIDOS)}'
        tel = celular()
        correo = f'{nombre.lower()}.{apellido.split()[0].lower()}{dni[-3:]}@correo.pe'.translate(
            str.maketrans('áéíóúñ', 'aeioun')) if random.random() < 0.55 else None
        apoderados.append((nombre, apellido, dni, tel, correo))
        familias[clave] = (dni, parentesco)
        e['apoderado'] = (nombre, apellido, tel)
    vinculos.append((e['id'], dni, parentesco, random.random() < 0.25))
    e['apoderado_dni'] = dni
    e['parentesco'] = parentesco
    e['telefono_apoderado'] = next((a[3] for a in apoderados if a[2] == dni), celular())

sql_est.append('CREATE TEMP TABLE st_apod (nombre text, apellido text, dni text, telefono text, email text);')
sql_est.append(bloque_copy('st_apod', ['nombre', 'apellido', 'dni', 'telefono', 'email'], apoderados))
sql_est.append('INSERT INTO apoderados (nombre, apellido, dni, telefono, email) SELECT * FROM st_apod;')
sql_est.append('CREATE TEMP TABLE st_vinc (estudiante_id bigint, dni text, parentesco text, permiso boolean);')
sql_est.append(bloque_copy('st_vinc', ['estudiante_id', 'dni', 'parentesco', 'permiso'], vinculos))
sql_est.append('INSERT INTO apoderado_estudiante (parentesco, permiso_retiro, apoderado_id, estudiante_id) '
               'SELECT v.parentesco, v.permiso, a.id, v.estudiante_id FROM st_vinc v JOIN apoderados a ON a.dni = v.dni;')
sql_est.append(f"""INSERT INTO consentimientos_datos (estudiante_id, apoderado_id, finalidad, accion, version_politica,
    evidencia, registrado_por, registrado_en)
SELECT v.estudiante_id, a.id, f.finalidad,
       CASE WHEN f.finalidad = 'SALUD' AND (v.estudiante_id % 23) = 0 THEN 'DENEGADO' ELSE 'OTORGADO' END,
       'v1-2026', 'Ficha de matrícula firmada por el apoderado', '{MARCA}',
       TIMESTAMP '2026-03-02 08:30' + ((v.estudiante_id % 10) || ' days')::interval + ((v.estudiante_id % 480) || ' minutes')::interval
FROM st_vinc v JOIN apoderados a ON a.dni = v.dni
CROSS JOIN (VALUES ('DATOS_ACADEMICOS'), ('SALUD'), ('COMUNICACIONES')) AS f(finalidad);""")

# Fichas de salud
fichas = []
for e in estudiantes:
    if e['id'] in con_ficha:
        continue
    sangre = random.choices(['O+', 'A+', 'B+', 'AB+', 'O-'], [74, 15, 7, 2, 2])[0]
    alergia = random.choice(['Penicilina', 'Polen', 'Mariscos', 'Lactosa', 'Polvo', 'Picadura de abeja']) \
        if random.random() < 0.14 else 'Ninguna'
    cronica = random.choice(['Asma', 'Rinitis alérgica', 'Gastritis', 'Anemia leve']) if random.random() < 0.08 else 'Ninguna'
    seguro = random.choices(['SIS', 'ESSALUD', 'PRIVADO', 'NINGUNO'], [70, 25, 3, 2])[0]
    nombre_ap, apellido_ap, tel = e.get('apoderado', (None, None, e.get('telefono_apoderado')))
    contacto = f'{nombre_ap} {apellido_ap}' if nombre_ap else 'Apoderado registrado'
    fichas.append((alergia, cronica, contacto, (e.get('parentesco') or 'MADRE').capitalize(), tel or celular(),
                   'Ninguna', e['id'], ts(date(2026, 3, 20), 10), ts(date(2026, 3, 20), 10),
                   'Salbutamol inhalador' if cronica == 'Asma' else 'Ninguno',
                   ''.join(random.choice('0123456789') for _ in range(9)) if seguro in ('SIS', 'ESSALUD') else None,
                   'Declaración de salud de la matrícula', sangre, seguro, 'enfermera1'))
sql_est.append(bloque_copy('fichas_medicas', [
    'alergias', 'condiciones_cronicas', 'contacto_emergencia_nombre', 'contacto_emergencia_parentesco',
    'contacto_emergencia_telefono', 'discapacidades', 'estudiante_id', 'fecha_actualizacion', 'fecha_creacion',
    'medicamentos_habituales', 'nro_seguro', 'observaciones_generales', 'tipo_sangre', 'tipo_seguro',
    'ultima_actualizacion_por'], fichas))

# Atenciones de tópico
MOTIVOS_TOPICO = [
    ('Dolor de cabeza', 'Cefalea tensional leve', 'Reposo 20 minutos e hidratación'),
    ('Golpe en Educación Física', 'Contusión leve en rodilla', 'Compresa fría y vendaje; se informó al apoderado'),
    ('Dolor de estómago', 'Dispepsia leve', 'Reposo y agua tibia; retorna a clase'),
    ('Mareo', 'Ayuno prolongado', 'Refrigerio y reposo; se recomienda desayunar'),
    ('Corte en la mano', 'Herida superficial', 'Limpieza y curación con apósito'),
    ('Fiebre', 'Cuadro febril 38,2 °C', 'Paños tibios; el apoderado lo recogió'),
    ('Dificultad respiratoria', 'Crisis asmática leve', 'Uso de su inhalador; control de saturación'),
    ('Dolor de muela', 'Dolor dental', 'Se orientó a centro de salud; aviso al apoderado'),
]
atenciones = []
for _ in range(240):
    e = random.choice(estudiantes)
    d = random.choice(DIAS_LECTIVOS)
    m, dx, tx = random.choice(MOTIVOS_TOPICO)
    turno = secciones[e['seccion']]['turno']
    hora = random.randint(8, 12) if turno == 'MANANA' else random.randint(14, 18)
    atenciones.append((e['id'], ts(d, hora, random.randint(0, 59)), m, dx, tx))
sql_est.append('CREATE TEMP TABLE st_aten (estudiante_id bigint, fecha timestamp, motivo text, diagnostico text, tratamiento text);')
sql_est.append(bloque_copy('st_aten', ['estudiante_id', 'fecha', 'motivo', 'diagnostico', 'tratamiento'], atenciones))
sql_est.append("INSERT INTO atenciones_medicas (diagnostico, fecha_atencion, motivo, registrado_por, tratamiento, ficha_medica_id) "
               "SELECT a.diagnostico, a.fecha, a.motivo, 'enfermera1', a.tratamiento, f.id FROM st_aten a "
               "JOIN fichas_medicas f ON f.estudiante_id = a.estudiante_id;")

# Derivaciones a psicología
docentes_ids = [d for d in docentes]
derivaciones = []
DERIV = [('RENDIMIENTO', 'Bajo rendimiento sostenido y desmotivación en varias áreas'),
         ('COMPORTAMIENTO', 'Conductas disruptivas reiteradas en el aula'),
         ('FAMILIAR', 'Comenta problemas familiares que afectan su ánimo'),
         ('BULLYING', 'Compañeros reportan burlas y exclusión en el recreo'),
         ('OTRO', 'Ansiedad antes de las evaluaciones')]
for i in range(48):
    e = random.choice([x for x in estudiantes if x['riesgo']] * 3 + estudiantes)
    motivo, desc = random.choice(DERIV)
    d = random.choice(DIAS_LECTIVOS[:-5])
    estado = random.choices(['CONCLUIDO', 'EN_EVALUACION', 'RECIBIDO'], [55, 30, 15])[0]
    atencion = ts(d + timedelta(days=random.randint(1, 6)), 11) if estado != 'RECIBIDO' else None
    notas = {'CONCLUIDO': 'Se realizaron sesiones de acompañamiento y orientación a la familia.',
             'EN_EVALUACION': 'En evaluación: primera entrevista realizada.'}.get(estado)
    derivaciones.append((desc, random.choice(docentes_ids), estado, e['id'], atencion, ts(d, 10, 15), motivo, notas,
                         'URGENTE' if motivo == 'BULLYING' or random.random() < 0.15 else 'NORMAL'))
sql_est.append(bloque_copy('derivaciones_psicologicas', [
    'descripcion', 'docente_derivador_id', 'estado', 'estudiante_id', 'fecha_atencion', 'fecha_derivacion',
    'motivo', 'notas_psicologo', 'prioridad'], derivaciones))

# Ingreso al colegio con DNI
ingresos, ausencias, tardanzas_dia = [], set(), {}
for d in DIAS_LECTIVOS:
    for e in estudiantes:
        if random.random() < e['ausencia']:
            ausencias.add((e['id'], d))
            continue
        turno = secciones[e['seccion']]['turno']
        tarde = random.random() < e['tardanza']
        if turno == 'MANANA':
            minuto = random.randint(46, 95) if tarde else random.randint(0, 39)
            hora = time(7, 5) if not tarde else time(7, 0)
            llegada = (datetime.combine(d, time(7, 5)) + timedelta(minutes=minuto)) if not tarde \
                else datetime.combine(d, time(7, 0)) + timedelta(minutes=minuto)
        else:
            llegada = (datetime.combine(d, time(12, 50)) + timedelta(minutes=random.randint(0, 54))) if not tarde \
                else datetime.combine(d, time(13, 46)) + timedelta(minutes=random.randint(0, 35))
        llegada += timedelta(seconds=random.randint(0, 59))
        if tarde:
            tardanzas_dia[(e['id'], d)] = llegada
        metodo = random.choices(['ESCANER', 'CAMARA', 'MANUAL'], [86, 10, 4])[0]
        ingresos.append((e['id'], d.isoformat(), llegada.strftime('%H:%M:%S'), turno, 'TARDANZA' if tarde else 'PUNTUAL',
                         metodo, random.choice(['auxiliar1', 'coord.convivencia']), llegada.strftime('%Y-%m-%d %H:%M:%S')))
sql_est.append('CREATE TEMP TABLE st_ing (estudiante_id bigint, fecha date, hora time, turno text, estado text, '
               'metodo text, registrado_por text, creado_en timestamp);')
sql_est.append(bloque_copy('st_ing', ['estudiante_id', 'fecha', 'hora', 'turno', 'estado', 'metodo', 'registrado_por',
                                      'creado_en'], ingresos))
sql_est.append('INSERT INTO ingresos_colegio (estudiante_id, fecha, hora, turno, estado, metodo, registrado_por, creado_en) '
               'SELECT * FROM st_ing ON CONFLICT (estudiante_id, fecha) DO NOTHING;')
sql_est.append('COMMIT;')

# ================================================================== academico_evaluaciones
sql_eva = ['BEGIN;', "SET client_encoding = 'UTF8';",
           "UPDATE notas SET bimestre = CASE bimestre WHEN 'BIMESTRE_1' THEN 'I BIMESTRE' WHEN 'BIMESTRE_2' THEN 'II BIMESTRE' "
           "WHEN 'BIMESTRE_3' THEN 'III BIMESTRE' WHEN 'BIMESTRE_4' THEN 'IV BIMESTRE' END WHERE bimestre LIKE 'BIMESTRE\\_%';",
           "DELETE FROM bimestres_estado WHERE anio = 2026 AND bimestre LIKE 'BIMESTRE\\_%';",
           # Semillas antiguas registraron notas y asistencia con una sección distinta a la del estudiante
           'CREATE TEMP TABLE st_sec (estudiante_id bigint, seccion_id bigint);',
           bloque_copy('st_sec', ['estudiante_id', 'seccion_id'], [(e['id'], e['seccion']) for e in estudiantes]),
           'DELETE FROM notas n USING st_sec s WHERE s.estudiante_id = n.estudiante_id AND s.seccion_id <> n.seccion_id;',
           'DELETE FROM asistencias a USING st_sec s WHERE s.estudiante_id = a.estudiante_id AND s.seccion_id <> a.seccion_id;']
for bim, estado, cierre in [('I BIMESTRE', 'CERRADO', date(2026, 5, 18)), ('II BIMESTRE', 'CERRADO', date(2026, 7, 27)),
                            ('III BIMESTRE', 'ABIERTO', None), ('IV BIMESTRE', 'ABIERTO', None)]:
    actualizado = ts(cierre or date(2026, 3, 16), 9)
    sql_eva.append(f"INSERT INTO bimestres_estado (anio, bimestre, estado, actualizado_por, actualizado_en, version) "
                   f"VALUES ({ANIO}, '{bim}', '{estado}', '{'director' if cierre else MARCA}', '{actualizado}', 0) "
                   f"ON CONFLICT (anio, bimestre) DO UPDATE SET estado = EXCLUDED.estado, "
                   f"actualizado_por = EXCLUDED.actualizado_por, actualizado_en = EXCLUDED.actualizado_en;")
    if cierre:
        sql_eva.append(f"INSERT INTO bimestres_auditoria (anio, bimestre, accion, motivo, actor, ocurrido_en) VALUES "
                       f"({ANIO}, '{bim}', 'CIERRE', 'Cierre del {bim.lower()} con actas revisadas y firmadas por Dirección', "
                       f"'director', '{actualizado}');")

# Notas por competencias y pesos
notas, pesos = [], []
fechas_notas = {'I BIMESTRE': date(2026, 5, 13), 'II BIMESTRE': date(2026, 7, 22), 'III BIMESTRE': date(2026, 10, 9)}
avance_iii = {}
for seccion_id, curso_id, docente_id in asignaciones:
    curso = cursos.get(curso_id, {}).get('nombre', '')
    competencias = COMPETENCIAS.get(curso)
    if not competencias or seccion_id not in por_seccion:
        continue
    # Avance del III bimestre: algunos docentes ya registraron todo, otros van a la mitad
    avance_iii[(seccion_id, curso_id)] = random.choice([1.0, 1.0, 0.67, 0.5, 0.34])
    for bim, fecha in fechas_notas.items():
        lista = competencias if bim != 'III BIMESTRE' else competencias[:max(1, math.ceil(len(competencias) * avance_iii[(seccion_id, curso_id)]))]
        if (seccion_id, curso_id, bim) not in pesos_existentes:
            base = round(100 / len(lista), 2)
            for i, c in enumerate(lista):
                peso = round(100 - base * (len(lista) - 1), 2) if i == len(lista) - 1 else base
                pesos.append((seccion_id, curso_id, bim, str(ANIO), c, peso, i, docentes[docente_id]['username'], ts(fecha, 18)))
        for e in por_seccion[seccion_id]:
            area = e['areas'].setdefault(curso_id, random.gauss(0, 0.55))
            mejora = {'I BIMESTRE': 0, 'II BIMESTRE': 0.15, 'III BIMESTRE': 0.25}[bim]
            for c in lista:
                if (e['id'], curso_id, bim, c.lower()) in notas_existentes:
                    continue
                z = e['habilidad'] + area + mejora + random.gauss(0, 0.6)
                registro = ts(fecha - timedelta(days=random.randint(0, 3)), random.randint(16, 21), random.randint(0, 59))
                notas.append((bim, c, curso_id, docente_id, nivel(z), e['id'], registro, None, str(ANIO), seccion_id))
sql_eva.append(bloque_copy('ponderaciones_competencia', ['seccion_id', 'curso_id', 'bimestre', 'periodo_anio', 'competencia',
                                                        'peso', 'orden', 'actualizado_por', 'actualizado_en'], pesos))
sql_eva.append(bloque_copy('notas', ['bimestre', 'competencia', 'curso_id', 'docente_id', 'escala_literal', 'estudiante_id',
                                     'fecha_registro', 'observaciones', 'periodo_anio', 'seccion_id'], notas))

# Asistencia por sesión de clase (III bimestre), coherente con el ingreso al colegio
sesiones, asistencias = [], []
dias_iii = [d for d in DIAS_LECTIVOS if d >= BIMESTRES['III BIMESTRE'][0]]
horarios_por = {}
for h in horarios:
    horarios_por.setdefault((h['seccion'], h['dia']), []).append(h)
for lista in horarios_por.values():
    lista.sort(key=lambda h: h['inicio'])
for d in dias_iii:
    for (seccion_id, dia), bloques in horarios_por.items():
        if dia != DIAS[d.weekday()] or seccion_id not in por_seccion:
            continue
        for orden, h in enumerate(bloques):
            if (h['id'], d.isoformat()) in sesiones_existentes:
                continue
            usuario = docentes.get(h['docente'], {}).get('username', 'docente')
            hi = datetime.combine(d, time.fromisoformat(h['inicio']))
            sesiones.append((h['id'], seccion_id, h['curso'], h['docente'], d.isoformat(), 'CERRADA', usuario, usuario,
                             (hi + timedelta(minutes=2)).strftime('%Y-%m-%d %H:%M:%S'),
                             (hi + timedelta(minutes=12)).strftime('%Y-%m-%d %H:%M:%S')))
            for e in por_seccion[seccion_id]:
                if (e['id'], d) in ausencias:
                    estado = 'JUSTIFICADO' if (e['id'] + d.toordinal()) % 4 == 0 else 'FALTA'
                elif orden == 0 and (e['id'], d) in tardanzas_dia:
                    estado = 'TARDE'
                else:
                    estado = 'PRESENTE'
                asistencias.append((h['curso'], h['docente'], estado, e['id'], d.isoformat(),
                                    (hi + timedelta(minutes=5)).strftime('%Y-%m-%d %H:%M:%S'), h['id'],
                                    'Certificado médico' if estado == 'JUSTIFICADO' else None, usuario, seccion_id,
                                    secciones[seccion_id]['turno']))
sql_eva.append(bloque_copy('sesiones_asistencia', ['horario_id', 'seccion_id', 'curso_id', 'docente_id', 'fecha', 'estado',
                                                  'abierta_por', 'cerrada_por', 'fecha_apertura', 'fecha_cierre'], sesiones))
sql_eva.append('CREATE TEMP TABLE st_asis (curso_id bigint, docente_id bigint, estado text, estudiante_id bigint, fecha date, '
               'fecha_registro timestamp, horario_id bigint, justificacion text, registrado_por text, seccion_id bigint, turno text);')
sql_eva.append(bloque_copy('st_asis', ['curso_id', 'docente_id', 'estado', 'estudiante_id', 'fecha', 'fecha_registro',
                                      'horario_id', 'justificacion', 'registrado_por', 'seccion_id', 'turno'], asistencias))
sql_eva.append('INSERT INTO asistencias (curso_id, docente_id, estado, estudiante_id, fecha, fecha_registro, horario_id, '
               'justificacion, registrado_por, seccion_id, turno) SELECT * FROM st_asis '
               'ON CONFLICT (estudiante_id, horario_id, fecha) DO NOTHING;')
sql_eva.append('COMMIT;')

# ================================================================== academico_disciplina
reportes = []
docente_de_seccion = {}
for s, c, d in asignaciones:
    docente_de_seccion.setdefault(s, []).append(d)
# Tardanzas reiteradas: 4 o más en un mes → reporte de convivencia
por_mes = {}
for (eid, d) in tardanzas_dia:
    por_mes.setdefault((eid, d.year, d.month), []).append(d)
estudiante_por_id = {e['id']: e for e in estudiantes}
MESES = ['', 'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'setiembre', 'octubre']
for (eid, _, mes), fechas in por_mes.items():
    if len(fechas) >= 4:
        f = max(fechas)
        reportes.append((True, f'Acumula {len(fechas)} tardanzas en {MESES[mes]}', None, 'NOTIFICADO', eid,
                         ts(f, 18), ts(f, 9), 'Se citó al apoderado para compromiso de puntualidad',
                         estudiante_por_id[eid]['seccion'], 'TARDANZA', 'coord.convivencia'))
INCIDENCIAS = [
    ('FALTA_LEVE', 'Uso del celular durante la clase', 60), ('FALTA_LEVE', 'No presentó la tarea por tercera vez', 35),
    ('FALTA_LEVE', 'Incumple con el uniforme escolar', 20), ('LLAMADA_ATENCION', 'Conversa constantemente e interrumpe la clase', 55),
    ('LLAMADA_ATENCION', 'Sale del aula sin permiso', 25), ('FALTA_GRAVE', 'Agresión verbal a un compañero', 10),
    ('FALTA_GRAVE', 'Daño intencional al mobiliario del aula', 5), ('MERITO', 'Ayudó a un compañero con dificultades de aprendizaje', 60),
    ('MERITO', 'Destacada participación en la feria de ciencias', 40), ('MERITO', 'Representó al colegio en el concurso de oratoria', 15),
    ('MERITO', 'Devolvió un objeto perdido a Dirección', 20)]
for tipo, desc, cantidad in INCIDENCIAS:
    for _ in range(cantidad):
        e = random.choice(estudiantes)
        d = random.choice(DIAS_LECTIVOS)
        docente_id = random.choice(docente_de_seccion.get(e['seccion'], docentes_ids))
        estado = random.choices(['NOTIFICADO', 'ARCHIVADO', 'EMITIDO'], [70, 20, 10])[0] if d < HOY - timedelta(10) else 'EMITIDO'
        notificado = estado != 'EMITIDO'
        reportes.append((notificado, desc, docente_id, estado, e['id'], ts(d, 19) if notificado else None,
                         ts(d, random.randint(8, 17), random.randint(0, 59)), None, e['seccion'], tipo,
                         docentes.get(docente_id, {}).get('username', 'docente1')))
sql_dis = ['BEGIN;', "SET client_encoding = 'UTF8';", bloque_copy('reportes_disciplinarios', [
    'apoderado_notificado', 'descripcion', 'docente_id', 'estado', 'estudiante_id', 'fecha_notificacion', 'fecha_reporte',
    'observaciones', 'seccion_id', 'tipo', 'registrado_por'], reportes), 'COMMIT;']

# ================================================================== academico_notificaciones
avisos = []


def aviso(tipo, eid, cuando, asunto, mensaje):
    e = estudiante_por_id[eid]
    canal = random.choices(['WHATSAPP', 'SMS', 'EMAIL'], [70, 20, 10])[0]
    fallida = random.random() < 0.03
    enviado = cuando + timedelta(seconds=random.randint(3, 40))
    avisos.append((str(uuid.uuid4()), tipo, canal, eid, asunto, mensaje, e.get('telefono_apoderado') or celular(),
                   'FALLIDA' if fallida else 'ENVIADA', 3 if fallida else 1, 3, None,
                   'Número no disponible' if fallida else None, str(uuid.uuid4()), 'SIMULATED',
                   cuando.strftime('%Y-%m-%d %H:%M:%S'), enviado.strftime('%Y-%m-%d %H:%M:%S'),
                   None if fallida else enviado.strftime('%Y-%m-%d %H:%M:%S')))


for (eid, d), llegada in tardanzas_dia.items():
    e = estudiante_por_id[eid]
    aviso('INGRESO', eid, llegada, 'Llegada con tardanza',
          f"{e['nombre']} ingresó al colegio a las {llegada.strftime('%H:%M')} (tardanza).")
for (eid, d) in ausencias:
    if d >= BIMESTRES['III BIMESTRE'][0]:
        e = estudiante_por_id[eid]
        aviso('INASISTENCIA', eid, datetime.combine(d, time(9, 30)), 'Inasistencia de hoy',
              f"{e['nombre']} no asistió al colegio el {d.strftime('%d/%m/%Y')}.")
for r in reportes:
    if r[0] and r[9] != 'MERITO':
        aviso('DISCIPLINA', r[4], datetime.fromisoformat(r[5]), 'Reporte de convivencia', r[1])
sql_not = ['BEGIN;', "SET client_encoding = 'UTF8';", bloque_copy('notificaciones', [
    'evento_id', 'tipo', 'canal', 'estudiante_id', 'asunto', 'mensaje', 'destinatario', 'estado', 'intentos', 'max_intentos',
    'proximo_intento', 'ultimo_error', 'correlation_id', 'detalle_proveedor', 'creada_en', 'actualizada_en', 'enviada_en'],
    avisos), 'COMMIT;']

# ================================================================== academico_cursos: tickets
TICKETS = [('MOBILIARIO', 'Carpeta con la pata rota'), ('MOBILIARIO', 'Pizarra con la superficie dañada'),
           ('ELECTRICO', 'Fluorescente que parpadea'), ('ELECTRICO', 'Tomacorriente sin energía'),
           ('PLOMERIA', 'Caño del lavadero gotea'), ('TECNOLOGIA', 'Proyector no enciende'),
           ('TECNOLOGIA', 'Parlante del aula sin sonido'), ('OTROS', 'Vidrio de ventana rajado')]
tickets = []
for _ in range(72):
    cat, desc = random.choice(TICKETS)
    d = random.choice(DIAS_LECTIVOS)
    antiguo = d < HOY - timedelta(20)
    estado = random.choices(['RESUELTO', 'EN_PROCESO', 'PENDIENTE'], [80, 12, 8] if antiguo else [20, 40, 40])[0]
    resolucion = ts(d + timedelta(days=random.randint(1, 8)), 15) if estado == 'RESUELTO' else None
    tickets.append((random.choice(aulas), cat, desc, random.choice(docentes_ids), estado, ts(d, random.randint(8, 17)),
                    resolucion, 'Reparado por el personal de mantenimiento' if estado == 'RESUELTO' else None,
                    random.choices(['BAJA', 'MEDIA', 'ALTA', 'EMERGENCIA'], [30, 45, 20, 5])[0]))
sql_cur = ['BEGIN;', "SET client_encoding = 'UTF8';", bloque_copy('tickets_mantenimiento', [
    'aula_id', 'categoria', 'descripcion', 'docente_id', 'estado', 'fecha_reporte', 'fecha_resolucion', 'notas_resolucion',
    'urgencia'], tickets), 'COMMIT;']

# ================================================================== academico_reportes: mural
mural = []
for bim, (a, z) in BIMESTRES.items():
    if a > HOY:
        continue
    mural.append((ts(a, 7, 45), 'servicio-evaluaciones', f'Inició el {bim.lower()} {ANIO}', 'EVALUACION'))
    if z < HOY:
        mural.append((ts(z + timedelta(days=3), 9), 'servicio-evaluaciones', f'Dirección cerró el {bim.lower()} con actas firmadas', 'EVALUACION'))
EVENTOS = [(date(2026, 3, 2), 'servicio-estudiantes', 'Inició la matrícula 2026: gratuita y solo con DNI', 'MATRICULA'),
           (date(2026, 3, 13), 'servicio-cursos', 'Se publicó la carga docente y los horarios 2026', 'HORARIO'),
           (date(2026, 3, 16), 'servicio-estudiantes', 'Primer día de clases: ingreso con DNI en portería', 'ASISTENCIA'),
           (date(2026, 4, 24), 'servicio-disciplina', 'Campaña «Buen trato en el recreo»', 'CONVIVENCIA'),
           (date(2026, 6, 5), 'servicio-evaluaciones', 'Feria de ciencias de secundaria', 'EVALUACION'),
           (date(2026, 7, 6), 'servicio-cursos', 'Día del Maestro: actividad central', 'HORARIO'),
           (date(2026, 9, 18), 'servicio-disciplina', 'Simulacro nacional de sismo', 'CONVIVENCIA'),
           (date(2026, 9, 23), 'servicio-evaluaciones', 'Juegos florales escolares', 'EVALUACION')]
mural += [(ts(d, 8), o, t, tipo) for d, o, t, tipo in EVENTOS]
for d in DIAS_LECTIVOS[::5]:
    presentes = sum(1 for (eid, f) in ausencias if f == d)
    mural.append((ts(d, 9, 5), 'servicio-estudiantes',
                  f'Ingreso del {d.strftime("%d/%m")}: asistencia de {100 - round(presentes * 100 / max(1, len(estudiantes)))} %',
                  'ASISTENCIA'))
sql_rep = ['BEGIN;', "SET client_encoding = 'UTF8';", bloque_copy('actividades', ['fecha', 'origen', 'texto', 'tipo'], mural), 'COMMIT;']

# ================================================================== escribir y aplicar
salida = Path(sys.argv[1] if len(sys.argv) > 1 else 'carga-2026')
salida.mkdir(parents=True, exist_ok=True)
archivos = {'academico_estudiantes': sql_est, 'academico_evaluaciones': sql_eva, 'academico_disciplina': sql_dis,
            'academico_notificaciones': sql_not, 'academico_cursos': sql_cur, 'academico_reportes': sql_rep}

# Marcas para poder deshacer: ids máximos antes de la carga
TABLAS = {'academico_estudiantes': ['apoderados', 'apoderado_estudiante', 'consentimientos_datos', 'fichas_medicas',
                                    'atenciones_medicas', 'derivaciones_psicologicas', 'ingresos_colegio'],
          'academico_evaluaciones': ['notas', 'ponderaciones_competencia', 'sesiones_asistencia', 'asistencias', 'bimestres_auditoria'],
          'academico_disciplina': ['reportes_disciplinarios'], 'academico_notificaciones': ['notificaciones'],
          'academico_cursos': ['tickets_mantenimiento'], 'academico_reportes': ['actividades']}
deshacer = ['-- Elimina lo insertado por carga_anio_escolar_2026.py (ejecutar cada bloque en su base).']
for db, tablas in TABLAS.items():
    deshacer.append(f'\n-- \\c {db}')
    for t in reversed(tablas):
        maximo = consultar(db, f'select coalesce(max(id), 0) from {t}')[0][0]
        deshacer.append(f'DELETE FROM {t} WHERE id > {maximo};')
(salida / 'deshacer_carga_anio_2026.sql').write_text('\n'.join(deshacer) + '\n', encoding='utf-8')

totales = {'apoderados': len(apoderados), 'vínculos familiares': len(vinculos), 'consentimientos': len(vinculos) * 3,
           'fichas de salud': len(fichas), 'atenciones de tópico': len(atenciones), 'derivaciones': len(derivaciones),
           'ingresos con DNI': len(ingresos), 'notas': len(notas), 'pesos de columnas': len(pesos),
           'sesiones de clase': len(sesiones), 'registros de asistencia': len(asistencias),
           'reportes de convivencia': len(reportes), 'avisos a familias': len(avisos), 'tickets': len(tickets),
           'actividades del mural': len(mural)}
for db, sql in archivos.items():
    (salida / f'{db}.sql').write_text('\n'.join(sql), encoding='utf-8')
print('Generado:')
for k, v in totales.items():
    print(f'  {k:28} {v:>8,}')
print(f'  {"TOTAL":28} {sum(totales.values()):>8,}')

if '--aplicar' in sys.argv:
    for db in archivos:
        print(f'Aplicando {db}…')
        with open(salida / f'{db}.sql', 'rb') as f:
            r = subprocess.run(['docker', 'exec', '-i', CONTENEDOR, 'psql', '-U', 'postgres', '-d', db,
                                '-v', 'ON_ERROR_STOP=1', '-q'], stdin=f, capture_output=True)
        if r.returncode != 0:
            print(r.stderr.decode('utf-8', 'replace')[-2000:])
            sys.exit(f'Falló la carga en {db}')
    print('Carga aplicada.')
