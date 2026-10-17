# Operación local

## Datos de demostración

Con `SEED_DEMO_DATA=true`, reinicia `servicio-estudiantes` para crear usuarios, apoderados,
vínculos, fichas y atenciones. Después ejecuta `docker compose --profile demo run --rm demo-data`
para poblar cursos, docentes, secciones, horarios, notas, asistencia, disciplina y actividades.
El proceso es idempotente y puede repetirse sin duplicar los registros demo.

Para el escenario completo de secundaria con 200 estudiantes, dos secciones por grado y prueba
del generador de horarios, sigue `documentacion/GUIA_DEMO_SECUNDARIA_EC2.md`. También puedes usar
`ops/demo/prepare-secundaria.ps1` en Windows o `sh ops/demo/prepare-secundaria.sh` en Linux.

La cuenta `ADMIN_SISTEMA` se crea al iniciar `servicio-estudiantes` cuando
`BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` tiene una contraseña válida. El nombre y correo se toman de
`BOOTSTRAP_SYSTEM_ADMIN_USERNAME` y `BOOTSTRAP_SYSTEM_ADMIN_EMAIL`; la cuenta debe cambiar la
clave en su primer ingreso.

## Integridad distribuida y notificaciones

Los servicios de estudiantes, evaluaciones y disciplina guardan los eventos de notificación en
una outbox dentro de su misma transacción. Un publicador programado los entrega y registra
intentos y errores para poder reintentarlos. Reportes reconcilia periódicamente los IDs blandos
entre bases; Director y `ADMIN_SISTEMA` también pueden iniciarlo con
`POST /api/integridad/reconciliar` y consultar incidencias en `GET /api/integridad/incidencias`.

El entorno local usa `NOTIFICATIONS_MODE=SIMULATED`. Para entregas reales cambia el modo a
`WEBHOOK`, configura URLs HTTPS y `NOTIFICATIONS_WEBHOOK_TOKEN`, y define en
`NOTIFICATIONS_REQUIRED_CHANNELS` los canales que deben estar disponibles al arrancar. Cada
petición incluye una clave de idempotencia para que el proveedor evite envíos duplicados.

## Compilación precompilada para poco espacio o red inestable

Los Dockerfiles normales siguen compilando dentro de Docker. Como alternativa, después de ejecutar
`mvn package` en un servicio se puede crear solo su imagen runtime, sin descargar dependencias Maven
dentro de BuildKit. Desde el directorio del servicio:

```powershell
docker build -f ../../ops/docker/Dockerfile.java-prebuilt -t sistemadistribuido-servicio-estudiantes:latest .
```

Para el frontend, ejecuta primero `npm run build` y después, desde `frontend`:

```powershell
docker build -f ../ops/docker/Dockerfile.frontend-prebuilt -t sistemadistribuido-frontend:latest .
```

## Observabilidad

Define `TRACING_SAMPLE_RATE=1.0` para desarrollo (en producción usa una fracción menor) e inicia el perfil con `docker compose --profile observability up -d`. Grafana queda en
`http://localhost:3000`, Prometheus en `http://localhost:9090` y Zipkin en
`http://localhost:9411`. Cambia `GRAFANA_PASSWORD` en `.env` antes de compartir el entorno.

## Respaldos cifrados

Define una clave larga y exclusiva en `BACKUP_ENCRYPTION_KEY` y ejecuta
`docker compose --profile operations up -d backup-postgres`. Cada copia se cifra con
AES-256-CBC/PBKDF2, genera SHA-256 y se restaura en una base temporal para comprobarla.
El volumen `postgres-backups` conserva treinta días por defecto.

Una copia no se considera recuperable hasta que la verificación de restauración termina
correctamente. Guarda la clave de cifrado fuera del repositorio y fuera del mismo equipo.

Para una verificación manual de un solo ciclo en PowerShell usa
`$env:BACKUP_RUN_ONCE='true'; docker compose --profile operations run --rm backup-postgres`;
el proceso termina después de respaldar y restaurar temporalmente las seis bases.
