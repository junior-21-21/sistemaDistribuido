# Auditoría técnica — 12 de octubre de 2026

Alcance: backend (gateway + 7 microservicios), frontend Angular, PostgreSQL, RabbitMQ y
orquestación Docker. Todo se levantó, se corrigió y se volvió a verificar en Docker.

## Resultado final verificado

| Verificación | Resultado |
|---|---|
| `docker compose up -d` desde cero | 11/11 contenedores `healthy` en ~2 min |
| Errores en logs tras arranque en frío | 0 en los 11 contenedores |
| Pruebas backend (Maven + Testcontainers) | 53 aprobadas, 0 fallos |
| Pruebas unitarias frontend | 23 aprobadas, 0 fallos |
| Build de producción Angular | aprobado, sin advertencias |
| `ops/tests/system-smoke.ps1` | 50 verificaciones aprobadas |
| `ops/tests/gateway-contract.ps1` | 10 verificaciones aprobadas |

Las 4 pruebas E2E de Playwright no se ejecutaron: falta el navegador de Playwright en esta
máquina (`npx playwright install` lo descarga).

## Errores encontrados y corregidos

### Bloqueantes (el sistema no levantaba o no compilaba)

1. **Faltaban `docker-compose.yml`, `.env`, `.env.example`, `README.md`, `.gitignore` y `ops/`**
   en la carpeta. Se recuperaron del respaldo `sistemaDistribuido.rar` y se añadió
   `servicio-horarios`, que el compose del respaldo no tenía.
2. **El frontend no compilaba** (`asistencia.ts`): usaba `nombres/apellidos` del docente, pero
   el backend envía `nombre/apellido`. También estaba mal la interfaz `Docente`.
3. **`servicio-estudiantes` no arrancaba sin `BOOTSTRAP_ADMIN_PASSWORD`**, contradiciendo el
   README. Ahora omite la cuenta inicial con un aviso en el log.

### Funcionales (fallaban en uso real)

4. **HTTP 500 en `/api/usuarios/page`** (listado de usuarios del administrador): la consulta
   `LIKE %:q%` rompía el conteo paginado de Spring Data. Se cambió a `concat('%', :q, '%')`.
5. **Matrícula falsa**: `/matricula` abría un formulario simulado que mostraba "¡Matrícula
   registrada!" sin guardar nada. La ruta ahora usa el formulario real (`matricula-secretaria`)
   que llama a `POST /api/estudiantes/matricula` con apoderado y consentimientos.
6. **Gestor de aula roto**:
   - `inject()` llamado fuera del contexto de inyección (error NG0203 al cargar, asignar
     horario o liberar un bloque).
   - Crear asignaciones siempre fallaba (faltaba `horasSemanales`, obligatorio en backend).
   - El aula estaba fija en `aulaId: 1`; ahora se elige un aula activa compatible con el turno.
7. **`HorarioService.eliminar()` enviaba `DELETE /`** y los métodos del motor de horarios
   llamaban a `/generar`, `/estado`, etc. sin el prefijo `/api/horarios`.
8. **El motor de horarios era inalcanzable**: la ruta `/api/horarios/**` de cursos se evaluaba
   primero en el gateway. Se le dio prioridad (`order: -1`) a la ruta de `servicio-horarios`.
9. **Botones simulados en Estudiantes**: "Reporte Asistencias" y "Comunicado Masivo" solo
   mostraban mensajes de éxito sin hacer nada; se retiraron. "Exportar Lista" ahora descarga
   un CSV real compatible con Excel.
10. **Dashboard**: pedía `/api/actividades` con roles que el gateway rechaza (403 en cada carga
    de Docente, Secretaría, etc.) y no manejaba errores al cargar secciones.
11. **Textos con tildes rotas** ("acadÃ©mica", "SecciÃ³n") en la pantalla de Horarios y en el
    servicio de horarios; los scripts `.ps1` también mostraban tildes rotas en PowerShell 5.1
    (se guardaron con BOM UTF-8).
12. **Subidas de documentos**: nginx limitaba el cuerpo a 1 MB, mientras el backend acepta
    10 MB. Ahora admite hasta 12 MB.

### Seguridad

13. `servicio-horarios` permitía a cualquier usuario autenticado cambiar parámetros o lanzar el
    generador, y tenía `@CrossOrigin("*")`. Ahora lectura solo para Dirección/Coordinación
    Académica, escritura solo para Coordinación Académica, y valida los parámetros.
14. Límite de intentos de login en nginx (30/min por IP, ráfaga de 20) con mensaje claro en la
    pantalla de ingreso (HTTP 429).
15. Cabeceras de seguridad en nginx (`X-Frame-Options`, `X-Content-Type-Options`,
    `Referrer-Policy`, `Permissions-Policy`) y `server_tokens off`.
16. El gateway (8080) ahora solo escucha en `127.0.0.1`; el público entra por nginx (80).
17. `INTERNAL_API_KEY` se pasa a todos los servicios (antes solo a reportes); se documenta en
    `.env.example` para separarla de `JWT_SECRET`.
18. `servicio-notificaciones` tenía contraseña de BD por defecto `postgres`; ahora es obligatoria
    como en los demás servicios.

### Configuración y calidad

19. Healthchecks en los 8 servicios Java y en el frontend; el gateway espera a que los
    servicios estén sanos y el frontend al gateway (sin 502/503 durante el arranque).
20. Pools de conexión: cursos y horarios reservaban 50 conexiones cada uno; con los 8 servicios
    se superaba el `max_connections=100` de PostgreSQL. Ahora 15 máx. (configurable).
21. `servicio-horarios`: migración Flyway propia (`V4__motor_timefold.sql`) para sus tablas, en
    lugar de depender de `JPA_DDL_AUTO=update`; se quitaron dos pruebas copiadas de cursos que
    no compilaban; se reemplazó una API de Timefold obsoleta.
22. JVM con `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` en todas las imágenes.
23. Se eliminaron advertencias: dialecto de Hibernate redundante, deprecaciones Sass de Bootstrap,
    propiedad CORS inexistente en reportes.
24. Pruebas frontend: fallaban en Node 22+ porque su `localStorage` global oculta al de jsdom;
    se añadió `src/test-setup.ts`.
25. `gateway-contract.ps1` usaba `-SkipHttpErrorCheck` (solo PowerShell 7); ahora funciona en
    Windows PowerShell 5.1.

## Pendientes recomendados (no cambiados)

- **Política de contraseñas**: el backend acepta claves de 4 caracteres, aunque el README pide
  10 con mayúscula, minúscula, número y símbolo. Conviene endurecerla antes de usar datos reales.
- **Git**: hay 21 archivos de `target/` versionados y `cookies.txt`; `.gitignore` ya los excluye,
  pero hay que quitarlos del índice (`git rm --cached`).
- `servicio-horarios` aún arrastra las migraciones V1–V3 copiadas de cursos (no se pueden borrar
  sin romper bases ya migradas) y su motor Timefold no está conectado a la pantalla de Horarios,
  que genera las propuestas en el navegador.
- La cuenta demo `director` ya tiene la contraseña cambiada, por eso el smoke test la omite.
