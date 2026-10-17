# Plataforma Académica Distribuida

Sistema académico con Angular 21, Spring Boot y PostgreSQL. La solución está separada en servicios de estudiantes, cursos, evaluaciones, disciplina y reportes, todos publicados a través de un API Gateway con autenticación JWT y autorización por roles.

## Inicio rápido con Docker

Requisitos: Docker Desktop y Docker Compose.

1. Copia `.env.example` como `.env`.
2. Reemplaza `DB_PASSWORD`, `JWT_SECRET`, `BOOTSTRAP_ADMIN_PASSWORD`, `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` y, si usarás pgAdmin, `PGADMIN_PASSWORD`.
3. Asegura que `JWT_SECRET` tenga al menos 32 bytes y que la contraseña inicial tenga mayúscula, minúscula, número, símbolo y 10 caracteres como mínimo.
4. Ejecuta:

```powershell
docker compose up --build -d
```

La aplicación estará en `http://localhost` y el gateway en `http://localhost:8080` (solo accesible desde la propia máquina). `docker compose ps` debe mostrar todos los contenedores como `healthy`; el arranque completo tarda unos dos minutos. PostgreSQL solo se publica en `127.0.0.1:5433`; los microservicios no exponen sus puertos al host.

Las cuentas iniciales de Director y Administrador del sistema se crean únicamente cuando sus respectivas contraseñas están configuradas y ambas deberán cambiarlas al primer ingreso. El Director gobierna y consulta auditoría; `ADMIN_SISTEMA` administra cuentas y permisos. Los datos de ejemplo están desactivados por defecto. Para una demostración local usa `SEED_DEMO_DATA=true`, reinicia `servicio-estudiantes` y ejecuta `docker compose --profile demo run --rm demo-data`.

La demostración extensa de secundaria —200 estudiantes, 30 docentes, carga académica y dos modos
de prueba de horarios— está descrita en [documentacion/GUIA_DEMO_SECUNDARIA_EC2.md](documentacion/GUIA_DEMO_SECUNDARIA_EC2.md).

## Servicios

| Componente | Puerto interno | Base de datos |
|---|---:|---|
| API Gateway | 8080 | — |
| Estudiantes y autenticación | 8082 | `academico_estudiantes` |
| Cursos y horarios | 8083 | `academico_cursos` |
| Evaluaciones y asistencia | 8084 | `academico_evaluaciones` |
| Disciplina | 8081 | `academico_disciplina` |
| Reportes y alertas | 8085 | `academico_reportes` |
| Notificaciones asíncronas | 8086 | `academico_notificaciones` |
| Motor de horarios (Timefold) | 8088 | `academico_horarios` |
| Frontend | 80 | — |

Los perfiles operativos opcionales y sus puertos están documentados en [`ops/README.md`](ops/README.md). El trazado está apagado en el arranque normal; al iniciar observabilidad define `TRACING_SAMPLE_RATE=1.0` (o un valor menor en producción).

## Seguridad aplicada

La distribución detallada de responsabilidades está en
[`documentacion/MATRIZ_ROLES.md`](documentacion/MATRIZ_ROLES.md).
El recorrido operativo completo está en
[`documentacion/FLUJO_SISTEMA.md`](documentacion/FLUJO_SISTEMA.md).

- JWT HS256 compartido, con clave obligatoria de 32 bytes o más.
- Autorización por roles en gateway y en los servicios sensibles.
- Una cuenta puede reunir varios roles; el rol principal se conserva solo por compatibilidad con clientes anteriores.
- Los IDs de usuario derivados del token no se aceptan desde formularios para tickets, derivaciones ni auditoría médica.
- Los documentos se validan por tamaño, tipo MIME, firma binaria y pertenencia al estudiante.
- CORS se limita mediante `CORS_ALLOWED_ORIGIN_PATTERNS`.
- No existen rutas Nginx que permitan saltarse el gateway.
- Los datos demo y credenciales iniciales son opt-in y no se imprimen en logs.
- Los eventos hacia notificaciones se escriben en una outbox transaccional y se reintentan sin perder la operación de negocio.
- Las referencias blandas entre bases se revisan mediante una reconciliación programada y consultable por Director o `ADMIN_SISTEMA`.
- Las lecturas del Docente se filtran en backend por su carga académica y nunca dependen solo de IDs solicitados.
- Coordinación Académica cierra bimestres y Dirección puede reabrirlos; ambos actos exigen motivo y quedan auditados.
- La matrícula guarda de forma atómica las decisiones versionadas de tratamiento de datos del apoderado.

## Verificación

Suite completa desde la raíz (52 backend, 23 frontend unitarias, 4 E2E y build):

```powershell
.\ops\tests\run-all-tests.ps1
```

Frontend:

```powershell
cd frontend
npm ci
npm audit
npm run build
npm test -- --watch=false
npx playwright test
```

Backend, desde cada servicio que tenga Maven Wrapper:

```powershell
mvnw.cmd test
```

El servicio de reportes usa la instalación local de Maven:

```powershell
cd backend/servicio-reportes
mvn test
```

Para conocer las mejoras futuras priorizadas consulta [DIAGNOSTICO_PROYECTO.md](DIAGNOSTICO_PROYECTO.md).
La guía reproducible y los casos de aceptación por rol están en
[`documentacion/PRUEBAS.md`](documentacion/PRUEBAS.md).
La política propuesta y la prueba de restauración están en
[`documentacion/POLITICA_DATOS_Y_RECUPERACION.md`](documentacion/POLITICA_DATOS_Y_RECUPERACION.md).
