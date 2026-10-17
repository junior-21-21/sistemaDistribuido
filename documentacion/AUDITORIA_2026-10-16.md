# Auditoría y adaptación a la organización MINEDU — 16 de octubre de 2026

Parte de lo ya resuelto en `AUDITORIA_2026-10-12.md` y `REDISENO_Y_TIEMPO_REAL.md`; aquí solo
está lo nuevo. Objetivo: que los roles y los flujos del sistema sigan la organización real de una
institución educativa pública urbana de Huamanga y que todo corra sin errores en Docker.

## Resultado verificado

| Verificación | Resultado |
|---|---|
| `docker compose down -v` y `up --wait` desde cero | 11/11 contenedores `healthy` en 1 min 45 s |
| Errores en logs tras el arranque | 0 en los 11 contenedores |
| Migraciones nuevas sobre una base existente (V11 estudiantes, V4 disciplina) | aplicadas sin errores |
| Pruebas backend (Maven + Testcontainers) | 80 aprobadas, 0 fallos |
| Pruebas unitarias frontend | 49 aprobadas, 0 fallos (3 nuevas sobre roles por ruta) |
| Build de producción Angular | aprobado, sin advertencias |
| `ops/tests/system-smoke.ps1 -UseDemoCredentials` | 71 verificaciones aprobadas (antes 50) |
| `ops/tests/gateway-contract.ps1` | 15 verificaciones aprobadas |
| Flujos reales de punta a punta (permisos, nóminas, tópico, SíseVe, tutor, portería) | 10/10 correctos, con avisos a la familia enviados |

El usuario `director` usa la contraseña de `BOOTSTRAP_ADMIN_PASSWORD` del `.env`; para la prueba
de humo se pasa con `PAD_CLAVE_DIRECTOR` (ver `MATRIZ_ROLES.md`).

## Errores encontrados y corregidos

1. **Datos demo rotos**: `seed-demo.sql` fallaba al insertar incidencias (`registrado_por` es
   obligatorio desde la migración de disciplina). Ahora registra al docente que corresponde.
2. **Prueba de humo desactualizada**: usaba la sección 1 fija para el docente; tras recargar los
   datos demo esa sección ya no era suya (403). Ahora toma la sección de su carga real.
3. **Portal del apoderado: las pestañas no cambiaban.** Usaban `data-bs-toggle` de Bootstrap,
   pero el JavaScript de Bootstrap no se carga. Ahora las controla Angular.
4. **Portal del apoderado**: la etiqueta «Secundaria» estaba fija aunque el hijo fuera de primaria,
   y las tardanzas (`TARDE`) no se coloreaban porque se comparaba con `TARDANZA`.
5. **Convivencia**: el año escolar 2026 estaba escrito a mano al cargar la carga docente; ahora
   usa el año actual.
6. **Ingreso al colegio**: el botón de registro se mostraba a Secretaría, pero el backend se lo
   niega (403). Ahora lo ven quienes realmente registran: auxiliar, TOE y portería.
7. **Encabezados**: el antetítulo (por ejemplo «CONVIVENCIA Y BIENESTAR») se montaba sobre el
   título en varias pantallas. Hay un estilo global que lo pone encima.
8. **Secciones**: subtítulo «Secundaria» aunque también hay primaria, y cabecera «ANO» en vez de «AÑO».
9. **`gateway-contract.ps1` fallaba en PowerShell 7**: solo entendía la excepción de Windows
   PowerShell 5.1. Ahora funciona en ambos.

## Roles actualizados según la organización del colegio

| Antes | Ahora |
|---|---|
| Subdirección era `DIRECTOR` (con acceso a cuentas y auditoría) | Nuevo rol base `SUBDIRECTOR`: lo pedagógico (carga, horarios, secciones, tutores, bimestres, convivencia), sin cuentas, auditoría, integridad ni aprobación de nóminas |
| No existía el personal de guardianía | Módulo opcional **Portería** con rol `PORTERO`: solo ingreso con DNI y papeletas de salida del día |
| «Responsable de convivencia» | Se presenta como **Coordinación de TOE** (mismo código `RESPONSABLE_CONVIVENCIA`) |
| El tutor no tenía efecto en el sistema | Dirección o Subdirección designa al **tutor de cada sección**; el tutor ve la asistencia y las incidencias de su aula |

La cuenta demo `coord.academica` pasa de `DIRECTOR + DOCENTE` a `SUBDIRECTOR + DOCENTE`, y se
agrega `portero1` (`DemoPorteria2026*`).

## Funciones nuevas

- **Permisos y justificaciones** (`/permisos`, `/api/permisos`): justificación de inasistencias
  (hasta 30 días atrás), papeletas de salida y permisos de varios días (hasta 60). El auxiliar, TOE,
  Subdirección o Dirección aprueban lo diario; los permisos formales los registra Secretaría y los
  aprueba solo Dirección. Al aprobar, la asistencia de esas fechas pasa a «Justificado» en
  `servicio-evaluaciones` y la familia recibe el aviso. Portería ve «Salidas de hoy» con quién
  recoge y su DNI, y registra la hora real de salida.
- **Portal del apoderado**: nueva pestaña para pedir justificaciones, papeletas y permisos y ver
  su estado.
- **Nóminas de matrícula** (`/nominas`, `/api/nominas`): estado por sección, detalle, PDF e
  impresión con espacio para firmas. Solo Dirección aprueba; si cambia el número de estudiantes,
  la nómina queda «desactualizada».
- **Tópico**: se registra si se avisó al apoderado y el destino (vuelve al aula, lo recoge el
  apoderado o traslado a un establecimiento de salud, con SIS o no). El traslado exige el nombre del
  establecimiento.
- **Violencia escolar y protocolo SíseVe**: nuevo tipo de incidencia «Violencia escolar». En las
  faltas graves y casos de violencia, TOE, Subdirección o Dirección registran el código SíseVe, la
  citación al apoderado, las medidas de protección y la derivación (Psicología, DEMUNA, CEM, Policía,
  Fiscalía o centro de salud). La papeleta muestra el avance de 0/4 a 4/4.
- **Tutor de aula** en «Secciones», con selector de docentes activos.

## Pendiente o fuera de alcance

- Varias pantallas (asistencia, notas, horarios, gestor de aula) siguen filtrando solo
  **secundaria**, como se decidió antes. Si el colegio tiene primaria, hay que revisar ese filtro.
- `servicio-disciplina` conserva plantillas Thymeleaf copiadas de `servicio-cursos` que no se usan.
  No se borraron.
- El registro en SíseVe sigue siendo manual en el portal del MINEDU; el sistema guarda el código y
  la fecha para el seguimiento.
- Las constancias de matrícula y de estudios en PDF quedan para una siguiente iteración.
