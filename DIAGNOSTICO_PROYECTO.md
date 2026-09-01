# Diagnóstico del Proyecto: Estado Actual vs. Visión Completa

## Estado del Desarrollo (Auditoría del Código Existente)

### BACKEND: Lo que YA existe (Construido)

| Microservicio | Entidades | Controllers | Services | Repositories | Estado |
|---|---|---|---|---|---|
| **servicio-estudiantes** | `Estudiante`, `Usuario`, `Apoderado`, `ApoderadoEstudiante`, `FichaMedica`, `Documento`, `DocumentoAdjunto`, `DerivacionPsicologica` | `EstudianteController`, `AuthController`, `ApoderadoController`, `DocumentoController`, `FichaMedicaController`, `DerivacionController` | `EstudianteService`, `UsuarioService`, `DocumentoService`, `ApoderadoService`, `CustomUserDetailsService` | `ApoderadoRepository`, `ApoderadoEstudianteRepository`, `DocumentoRepository`, `DocumentoAdjuntoRepository`, `FichaMedicaRepository`, `DerivacionPsicologicaRepository` | Funcional |
| **servicio-cursos** | `Nivel`, `Grado`, `Seccion`, `Aula`, `Docente`, `Curso`, `Horario`, `PeriodoEscolar`, `TicketMantenimiento` | `NivelController`, `GradoController`, `SeccionController`, `AulaController`, `DocenteController`, `CursoController`, `HorarioController`, `PeriodoEscolarController`, `TicketMantenimientoController` | `NivelService`, `GradoService`, `SeccionService`, `AulaService`, `DocenteService`, `CursoService`, `HorarioService`, `PeriodoEscolarService` | `CursoRepository`, `DocenteRepository`, `HorarioRepository`, `TicketMantenimientoRepository` | Funcional |
| **servicio-evaluaciones** | `Nota`, `Asistencia` | `NotaController`, `AsistenciaController` | `NotaService`, `AsistenciaService` | `NotaRepository`, `AsistenciaRepository` | Funcional |
| **servicio-disciplina** | `ReporteDisciplinario` | `ReporteDisciplinarioController` | `ReporteDisciplinarioService` | `ReporteDisciplinarioRepository` | Funcional |
| **servicio-reportes** | N/A (Agregador) | `EstadisticasController`, `AlertaDesercionController` | `AlertaDesercionService` | N/A | Parcial |
| **api-gateway** | N/A | N/A | N/A | N/A | Configurado |

**Total Backend:** 20 Entidades JPA | 21 Controllers | 18 Services | 13 Repositories

---

### FRONTEND: Lo que YA existe (Construido)

| Componente | Ruta | Roles Autorizados | Estado |
|---|---|---|---|
| `LoginComponent` | `/login` | Todos | Funcional |
| `Dashboard` | `/dashboard` | Todos (autenticados) | Funcional |
| `Estudiantes` | `/estudiantes` | DIRECTOR, SECRETARIA | Funcional |
| `Evaluaciones` | `/evaluaciones` | DOCENTE | Funcional |
| `Topico` | `/topico`, `/fichas-medicas` | ENFERMERA | Funcional |
| `Tickets` | `/tickets` | DOCENTE, DIRECTOR | Funcional |
| `Derivaciones` | `/derivaciones` | DOCENTE, DIRECTOR | Funcional |
| `Horarios` | `/horarios` | DIRECTOR, SECRETARIA | Funcional |
| `Disciplina` | `/disciplina` | DOCENTE, DIRECTOR | Funcional |
| `Personal` | `/personal` | DIRECTOR | Funcional |

**Infraestructura Angular:**
- Guards: `authGuard` + `roleGuard` (RBAC implementado)
- Interceptores: Carpeta existe
- Services: `auth.service.ts`, `estudiante.service.ts`, `nota.service.ts`, `horario.service.ts`, `disciplina.service.ts`, `topico.service.ts`, `derivacion.service.ts`, `ticket.service.ts`
- Layout: `MainLayout` (sidebar/navbar)
- Models: Solo `estudiante.model.ts` definido

**Docker Compose:** Orquestación completa de 7 contenedores (SQL Server + 5 microservicios + Gateway + Frontend)

---

## Lo que FALTA implementar (Gap Analysis)

### Prioridad ALTA (Core Incompleto)

| Item | Descripción | Impacto |
|---|---|---|
| **Redis Cache** | No hay contenedor Redis en `docker-compose.yml` ni configuración de `spring-boot-starter-data-redis` | Las validaciones inter-servicio (Soft FKs) usan REST puro sin caché, colapsará en picos |
| **RabbitMQ Broker** | No hay contenedor RabbitMQ ni `spring-boot-starter-amqp` | Sin alertas asíncronas a apoderados, sin invalidación de caché, sin IA de deserción en tiempo real |
| **Repositorios faltantes** | `servicio-estudiantes` no tiene `EstudianteRepository` ni `UsuarioRepository` explícitos; `servicio-cursos` no tiene repos para `Nivel`, `Grado`, `Seccion`, `Aula`, `PeriodoEscolar` | Spring Data JPA podría auto-generarlos pero sin queries custom optimizadas |
| **Models TypeScript** | Solo existe `estudiante.model.ts`; faltan interfaces para `Nota`, `Asistencia`, `Docente`, `Horario`, `ReporteDisciplinario`, `FichaMedica`, etc. | TypeScript pierde type-safety, propenso a bugs silenciosos |

### Prioridad MEDIA (Funcionalidades Avanzadas Pendientes)

| Item | Descripción |
|---|---|
| **Endpoints de validación inter-servicio** | No existen los endpoints ligeros `GET /api/v1/estudiantes/validar/{id}` ni `GET /api/v1/cursos/validar-asignacion` |
| **Módulo PWA Offline-First** | Service Worker registrado pero sin lógica de `IndexedDB` ni cola de sincronización |
| **Cuadrícula tipo Excel para notas** | No implementada la digitación masiva con scroll virtual y ciclado de teclado |
| **Importación masiva por Excel** | No existe endpoint de upload `.xlsx` ni parser backend |
| **Auditoría Hibernate Envers** | Anotaciones `@Audited` presentes en `Estudiante`, `Apoderado`, `Documento` pero sin endpoint de consulta de historial ni tabla `revinfo` verificada |
| **Congelamiento de calendario** | Entidad `PeriodoEscolar` tiene campos de suspensión pero sin lógica de propagación a Evaluaciones |
| **Validación de cruce de horarios** | No implementada la lógica de verificar colisiones de aula+turno+hora |

### Prioridad BAJA (Futuro / Diferenciador)

| Item | Descripción |
|---|---|
| **Blockchain para certificados** | No iniciado |
| **Machine Learning predictivo** | `AlertaDesercionService` existe pero sin modelo ML real |
| **Carnets QR** | No iniciado |
| **Integración WhatsApp/SMS** | No iniciado (requiere RabbitMQ primero) |
| **Backups automatizados SQL Server** | No configurados en Docker |

---

## Próximos Pasos Recomendados (Roadmap Sugerido)

### Fase 1: Cerrar infraestructura distribuida
1. Agregar contenedores **Redis** y **RabbitMQ** al `docker-compose.yml`
2. Implementar `RedisCacheConfig` en cada microservicio consumidor
3. Implementar `RabbitMQConfig` con el Topic Exchange y las 4 colas definidas
4. Crear endpoints ligeros de validación inter-servicio

### Fase 2: Completar type-safety y repositorios
5. Crear interfaces TypeScript para todas las entidades
6. Agregar repositorios JPA faltantes con queries custom (`@Query`)
7. Implementar validación de cruce de horarios en `HorarioService`

### Fase 3: Funcionalidades de negocio críticas
8. Implementar lógica de congelamiento de calendario con propagación RabbitMQ
9. Construir cuadrícula de notas con ciclado de teclado y scroll virtual
10. Implementar soporte Offline-First con IndexedDB

### Fase 4: Diferenciadores
11. Modelo ML de deserción real
12. Integración WhatsApp Business API
13. Generación de certificados con QR
