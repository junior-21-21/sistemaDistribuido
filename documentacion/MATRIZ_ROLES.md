# Matriz de roles del colegio

La plataforma sigue la organización real de una institución educativa pública de secundaria:
**seis puestos base**, **encargos** que Dirección asigna a personal que ya trabaja en el colegio
y **módulos opcionales** que se activan solo si el colegio tiene ese personal. Así se evita que
dos roles hagan lo mismo y cada persona ve un menú corto con lo suyo.

## Puestos base

| Rol | Quién es | Qué hace en el sistema |
|---|---|---|
| `DIRECTOR` | Director/a | Responsable legal: aprueba y firma la nómina de matrícula, aprueba los permisos formales, gestiona cuentas, encargos y módulos, ve la auditoría y la integridad, y datos de la institución |
| `SUBDIRECTOR` | Subdirector/a (pedagógica o de nivel) | Lo pedagógico: carga docente y horarios, cursos y secciones, designa tutores, consolida notas y bimestres, supervisa convivencia; aprueba justificaciones y papeletas. No gestiona cuentas, auditoría ni la nómina |
| `DOCENTE` | Profesores (y tutores de aula) | Solo sus secciones y el aula que tutora: asistencia, notas, incidencias, derivaciones, registro de justificaciones y papeletas que trae el apoderado, reporte de fallas |
| `AUXILIAR` | Auxiliares de educación | Ingreso con DNI, asistencia del día, incidencias, papeletas de salida y justificaciones (las aprueba), citaciones a la familia. No se le asignan tareas administrativas |
| `SECRETARIA` | Secretaría / mesa de partes | Matrícula, traslados, legajo, consentimientos, nóminas (las prepara e imprime), permisos formales de varios días (los registra; los aprueba Dirección) |
| `APODERADO` | Familia | Notas, asistencia, documentos y avisos de sus hijos; solicita justificaciones, papeletas y permisos desde el portal |

## Encargos (función adicional de un docente o auxiliar)

| Encargo | A quién se asigna | Qué habilita |
|---|---|---|
| `RESPONSABLE_CONVIVENCIA` | Coordinación de Tutoría y Orientación Educativa (TOE) | Libro de incidencias, protocolo de violencia escolar (registro SíseVe, citación, medidas de protección, derivación a DEMUNA, CEM, Policía, Fiscalía o centro de salud), aprobación de justificaciones y papeletas, avisos a familias |
| `PRIMEROS_AUXILIOS` | Auxiliar o docente | Tópico y fichas de salud cuando el colegio no tiene enfermera |

El **tutor de aula** no es un rol aparte: Dirección o Subdirección elige un docente activo como tutor
de cada sección en «Secciones». Ese docente ve la asistencia y las incidencias de su aula aunque no
dicte un curso en ella.

Base normativa: RM 432-2020-MINEDU (matrícula y nómina), DS 004-2018-MINEDU (convivencia sin violencia,
SíseVe y protocolos), RM 010-2026 y normas de organización de II.EE. públicas; el auxiliar lleva el
registro de asistencia, el cuaderno de incidencias y la ficha de seguimiento (Oficio Múltiple
00087-2022-MINEDU).

## Módulos opcionales (Dirección los habilita en «Personal y encargos»)

| Módulo | Rol | Si el colegio no lo tiene |
|---|---|---|
| Psicología | `PSICOLOGO` | La pantalla de Psicología se oculta; la coordinación de TOE hace el seguimiento |
| Enfermería | `ENFERMERA` | El tópico lo atiende quien tenga el encargo de primeros auxilios |
| Mantenimiento | `MANTENIMIENTO` | Los tickets de los docentes los ve y resuelve Dirección |
| Portería | `PORTERO` | El auxiliar registra los ingresos y verifica las salidas |

Un rol opcional solo se puede asignar si su módulo está habilitado, y un módulo no se puede
deshabilitar mientras haya cuentas activas con ese rol. Portería solo ve el control de ingreso
y las papeletas de salida aprobadas del día; no ve datos de estudiantes ni aprueba nada.

## Flujos principales

| Situación | Quién inicia | Quién aprueba o registra | Qué hace el sistema |
|---|---|---|---|
| Matrícula | Secretaría | Dirección aprueba la nómina por sección | Marca la nómina como aprobada; si luego cambia el número de estudiantes, queda «desactualizada» y pide nueva aprobación. Nómina en PDF o impresa para firma |
| Justificación de falta o tardanza | Apoderado (portal, auxiliar o tutor) | Auxiliar, TOE, Subdirección o Dirección | Al aprobar, la asistencia de esas fechas pasa a «Justificado» y se avisa a la familia |
| Papeleta de salida | Apoderado o personal | Auxiliar, TOE, Subdirección o Dirección | Aparece en «Salidas de hoy» de Portería con quien recoge y su DNI; Portería registra la hora real de salida |
| Permiso de varios días | Secretaría (mesa de partes) | Solo Dirección | Justifica la asistencia del periodo y avisa a la familia |
| Malestar o accidente | Tópico | — | Se registra la atención, si se avisó al apoderado y el destino (vuelve al aula, lo recoge el apoderado o traslado a centro de salud con o sin SIS). La familia recibe el aviso |
| Violencia escolar | Docente, auxiliar o TOE (tipo «Violencia escolar») | TOE, Subdirección o Dirección completan el protocolo | Código SíseVe, citación al apoderado, medidas de protección y derivación externa, con avance 0/4 a 4/4 en la papeleta |

## Equivalencia con el modelo anterior (migración V9)

| Rol anterior | Pasa a |
|---|---|
| `COORDINADOR_ACADEMICO` | `SUBDIRECTOR` desde la migración V11 (antes `DIRECTOR`); si además enseñaba, conserva `DOCENTE` |
| `COORDINADOR_CONVIVENCIA` | `AUXILIAR` + encargo `RESPONSABLE_CONVIVENCIA` |
| `ADMIN_SISTEMA` | `DIRECTOR` (un colegio público no tiene administrador de sistemas; las cuentas las gestiona Dirección) |

## Reglas de seguridad

- Toda cuenta tiene al menos un puesto; un encargo nunca va solo.
- Los encargos solo se asignan a docentes o auxiliares.
- Una cuenta de apoderado no combina con roles del personal.
- Dirección no puede quitarse a sí misma el rol de Dirección.
- El docente nunca obtiene alcance global por conocer un ID: sus lecturas se intersectan con su
  carga curso–docente–sección del año escolar.
- Los permisos se validan en el API Gateway y se repiten en cada servicio.
- La base de datos solo acepta los roles del catálogo (`ck_usuario_rol`).

## Alta inicial

Las cuentas iniciales son opt-in. Configura `BOOTSTRAP_ADMIN_PASSWORD` (y, si se usa,
`BOOTSTRAP_SYSTEM_ADMIN_PASSWORD`) para crear cuentas de Dirección. Las contraseñas deben tener
al menos diez caracteres, mayúscula, minúscula, número y símbolo; cada cuenta queda obligada a
cambiarla en el primer ingreso.

## Pruebas con contraseñas propias

Los scripts `ops/tests/system-smoke.ps1`, `gateway-contract.ps1` y `resilience-smoke.ps1` leen
las contraseñas de variables de entorno `PAD_CLAVE_<USUARIO>` (por ejemplo `PAD_CLAVE_DOCENTE1`,
`PAD_CLAVE_COORD_CONVIVENCIA`, `PAD_CLAVE_MANTENIMIENTO1`). Si no existen, usan las de demostración.
