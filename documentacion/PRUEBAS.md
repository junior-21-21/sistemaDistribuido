# Estrategia y ejecución de pruebas

La suite comprueba reglas de negocio, persistencia PostgreSQL real, migraciones Flyway,
autorización por roles, contratos HTTP del gateway, interfaz Angular y navegación en Chromium.

## Ejecución completa

Requisitos: Java 17 o superior, Maven, Node.js, dependencias de frontend instaladas y
Docker Desktop iniciado. Desde la raíz:

~~~powershell
.\ops\tests\run-all-tests.ps1
~~~

Opciones rápidas:

~~~powershell
.\ops\tests\run-all-tests.ps1 -SkipE2E
.\ops\tests\run-all-tests.ps1 -SkipE2E -SkipFrontendBuild
~~~

Los tests de integración crean PostgreSQL efímero con Testcontainers y lo eliminan al
terminar; no modifican la base de desarrollo.

## Último resultado verificado

Ejecución local del 28 de septiembre de 2026:

- Backend: 52 pruebas aprobadas, 0 fallos.
- Frontend unitario: 23 pruebas aprobadas, 0 fallos.
- Navegador Chromium: 4 pruebas E2E aprobadas, 0 fallos.
- Build Angular de producción: aprobado, 552.71 kB iniciales (108.16 kB estimados transferidos).
- Suite completa: aprobada en 3 min 13 s.
- Contrato Docker por roles: 10 verificaciones aprobadas, con identidades temporales eliminadas.
- Smoke de cuentas disponibles: 13 verificaciones aprobadas; varias claves demo ya habían
  sido cambiadas y se omitieron sin restablecer cuentas reales.
- Restauración: seis bases cifradas respaldadas y restauradas con evidencia persistente.
- Resiliencia: caída de Cursos observada como 504 y recuperación posterior a 200.
- Demo secundaria: 200 estudiantes, 30 docentes y 150 asignaciones; los modos con 350 o 0
  horarios fueron verificados sin conflictos. El modo completo incluye 400 notas, 1.000
  asistencias, 20 reportes disciplinarios y 600 consentimientos.

## Cobertura funcional

| Área | Verificaciones principales |
|---|---|
| Autenticación | login real, JWT, multirrol, obligatoriedad de cambio de clave y rutas protegidas |
| Usuarios y estudiantes | permisos de administración, documentos, salud, contactos internos y referencias |
| Cursos y horarios | carga curso–docente–sección, límites de horas, conflictos y roles académicos |
| Evaluaciones | notas, alcance docente, asistencia masiva, sesión diaria, cierre/reapertura y bloqueo posterior |
| Disciplina | validación de reportes y creación transaccional del outbox |
| Notificaciones | idempotencia, canales simulados/webhook, autorización y trazabilidad |
| Reportes | actividades, reconciliación de IDs blandos, Flyway y permisos de integridad |
| Gateway | proxy HTTP/JSON, correlación incluso en 401/403 y matriz de roles/multirrol |
| Frontend | servicios, guards, componentes, login, multirrol y cambio de clave |

## Smoke test del sistema Docker

Con el stack levantado y SEED_DEMO_DATA=true:

~~~powershell
.\ops\tests\system-smoke.ps1 -UseDemoCredentials
.\ops\tests\gateway-contract.ps1
~~~

`system-smoke.ps1` solo realiza login y consultas de lectura. Recorre frontend, gateway y todos los
microservicios con las cuentas demo de cada rol, valida JSON y propagación de correlación,
además de comprobar respuestas 401 y 403.
No debe usarse con credenciales demo en un entorno público.
Si algunas claves demo ya fueron cambiadas manualmente, se puede diagnosticar el resto sin
restablecerlas mediante `-SkipUnavailableAccounts`; el modo normal permanece estricto.

`gateway-contract.ps1` crea identidades temporales por rol, verifica contratos reales entre
gateway y microservicios (alcance docente, cierre/reapertura, auditoría y consentimientos) y
elimina en un bloque `finally` los usuarios y el período 2099 reservados para la prueba.

## Prueba manual de aceptación por rol

1. Abre `http://localhost` en una ventana privada.
2. Inicia sesión con una cuenta del rol indicado.
3. Comprueba que solo aparecen sus módulos.
4. Abre una URL de un módulo no autorizado y confirma que vuelve al panel.
5. Para validar el backend, repite la operación HTTP con el JWT y espera `403`.

| Rol | Debe poder hacer | Debe quedar bloqueado |
|---|---|---|
| Director | Dashboard, reportes, auditoría y supervisión de notificaciones | Administrar cuentas, registrar notas/asistencia o gestionar tickets |
| Administrador del sistema | Crear usuarios, asignar roles, cambiar estados y restablecer claves | Datos clínicos, notas, asistencia y portal familiar |
| Coordinación académica | Secciones, carga docente, horarios y lectura de rendimiento | Escribir notas como docente |
| Coordinación de convivencia | Asistencia, disciplina, derivaciones y notificaciones | Administración técnica y salud |
| Secretaría | Matrículas, apoderados, documentos, secciones y apoyo de horarios | Auditoría, salud y gestión de tickets |
| Docente | Sus cargas, notas, asistencia, disciplina, derivaciones y creación de tickets | Resolver tickets o administrar usuarios |
| Auxiliar | Asistencia y disciplina | Notas, usuarios, salud y tickets |
| Psicología | Consultar y atender derivaciones | Crear derivaciones como docente o acceder a salud |
| Enfermería | Fichas médicas y atenciones de tópico | Rendimiento, usuarios y derivaciones psicológicas |
| Mantenimiento | Consultar aulas/tickets y registrar seguimiento o resolución | Crear tickets, ver estudiantes o datos académicos |
| Apoderado | Ver únicamente estudiantes vinculados, notas, asistencia y documentos | Consultar estudiantes de otra familia o módulos internos |

Para Mantenimiento, marca un ticket como `RESUELTO` sin notas y verifica que tanto la
interfaz como el backend lo rechazan. Luego añade las notas y confirma el cambio.

## Comandos por capa

~~~powershell
mvn -f backend\servicio-estudiantes\pom.xml test
npm --prefix frontend run test:unit
npm --prefix frontend run test:e2e
npm --prefix frontend run build
~~~

## Carga, caída y recuperación

Prueba de carga sin dependencias adicionales, con 500 solicitudes y concurrencia 20:

~~~powershell
node .\ops\tests\load-smoke.mjs
~~~

Los umbrales predeterminados son error menor o igual a 1 % y p95 menor o igual a 750 ms.
Para probar una API autenticada configura `LOAD_URL` y `LOAD_TOKEN`.

Caída y recuperación controlada de Cursos a través del circuit breaker del gateway:

~~~powershell
.\ops\tests\resilience-smoke.ps1 -UseDemoCredentials
~~~

El script siempre vuelve a iniciar el servicio en un bloque `finally`.

Última línea base local, con 1.000 solicitudes y concurrencia 25 en cada escenario:

| Ruta | Errores | p95 | Solicitudes/s |
|---|---:|---:|---:|
| `/actuator/health` | 0 % | 43,06 ms | 1.032,21 |
| `/api/aulas` autenticada | 0 % | 94,39 ms | 397,15 |

## Backup y restauración

~~~powershell
$env:BACKUP_ENCRYPTION_KEY='<secreto-desde-el-almacén>'
.\ops\tests\backup-restore.ps1
~~~

El ejecutor reconstruye la imagen operativa, cifra las seis bases, verifica SHA-256,
restaura cada copia en una base temporal, compara tablas y exige los archivos de evidencia.

## Proveedores reales

La guía y el comando de certificación están en
[`CERTIFICACION_PROVEEDORES.md`](CERTIFICACION_PROVEEDORES.md). La prueba no puede aprobarse
en modo `SIMULATED`; requiere proveedor, secretos y destinatarios institucionales reales.
