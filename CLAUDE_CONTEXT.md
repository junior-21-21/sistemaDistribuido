# CONTEXTO TÉCNICO ULTRA-COMPACTO: PLATAFORMA ACADÉMICA DISTRIBUIDA (PAD)
> **Instrucción para el LLM (Claude):** Este documento contiene el contexto completo de arquitectura, base de datos, reglas de negocio y restricciones técnicas del proyecto. Asimila esta información directamente sin requerir lecturas redundantes de código.

---

## 1. Ficha Técnica y Stack
- **Backend:** Java 17 | Spring Boot 3.x (Cloud Gateway, Data JPA, Security, Resilience4j, Lombok, MapStruct, Hibernate Envers).
- **Base de Datos:** Microsoft SQL Server 2022 (Patrón *Database-per-Service*).
- **Caché y Mensajería:** Redis (Patrón *Cache-Aside* con TTL diferenciado) | RabbitMQ (*Topic Exchange* + DLQ).
- **Frontend:** Angular 21 (Standalone components, TypeScript, PWA Service Worker, Bootstrap 5 utilidades).
- **Testing:** Playwright (Pruebas E2E, simulación offline y navegación por teclado).
- **Despliegue:** Docker & Docker Compose (`docker-compose up -d`).

---

## 2. Mapa de Microservicios y Puertos

| Servicio | Directorio | Puerto | Base de Datos Lógica | Responsabilidad Principal |
|---|---|---|---|---|
| **API Gateway** | `backend/api-gateway` | `8080` | N/A | Entrada única, enrutamiento, validación JWT, Circuit Breakers |
| **Estudiantes** | `backend/servicio-estudiantes` | `8082` | `AcademicoEstudiantes` | Alumnos, usuarios (RBAC), apoderados, legajos (VARBINARY), tópico/fichas médicas, psicología |
| **Cursos** | `backend/servicio-cursos` | `8083` | `AcademicoCursos` | Niveles, grados, secciones, aulas (doble turno), docentes, cursos, horarios, periodos, tickets averías |
| **Evaluaciones** | `backend/servicio-evaluaciones` | `8084` | `AcademicoEvaluaciones` | Notas (MINEDU: AD/A/B/C y vigesimal 0-20), control de asistencias por bloque horario |
| **Disciplina** | `backend/servicio-disciplina` | `8081` | `AcademicoDisciplina` | Papeletas digitales de conducta (tardanzas, faltas, méritos), alerta apoderado |
| **Reportes** | `backend/servicio-reportes` | `8085` | `AcademicoReportes` | Agregador WebClient, alertas tempranas de deserción con IA, reportes oficiales |
| **Frontend** | `frontend/` | `80` | N/A (IndexedDB local) | Single Page Application (PWA) para Dirección, Secretaría, Tópico y Docentes |
| **SQL Server** | Docker | `1433` | 5 esquemas lógicos | Persistencia relacional ACID |

---

## 3. Modelo de Datos y Soft Foreign Keys (Cross-Service)

Debido al aislamiento de esquemas (*Database-per-Service*), no existen `FOREIGN KEY` físicas entre distintas bases de datos. Se usan **Soft FKs (`BIGINT`)** validadas en la capa de servicios mediante Redis / REST.

```text
[AcademicoEstudiantes]
  - usuarios: (id, username[UK], password, nombre, apellido, email, rol, estado, estudiante_id[FK])
  - estudiantes: (id, nombre, apellido, dni[UK], email[UK], telefono, grado_id[SoftFK], seccion_id[SoftFK], estado)
  - apoderados: (id, nombre, apellido, dni[UK], email, telefono)
  - apoderado_estudiante: (id, estudiante_id[FK], apoderado_id[FK], parentesco, permiso_retiro[BIT])
  - fichas_medicas: (id, estudiante_id[FK-UK], tipo_sangre, alergias, condiciones_cronicas, tipo_seguro, contacto_emergencia_tel)
  - documentos_adjuntos: (id, estudiante_id[SoftFK], nombre_archivo, tipo_documento, content_type, tamano_bytes, datos[VARBINARY(MAX)])
  - derivaciones_psicologicas: (id, estudiante_id[SoftFK], docente_derivador_id[SoftFK], motivo, prioridad, estado, notas_psicologo)

[AcademicoCursos]
  - niveles: (id, nombre[UK: PRIMARIA/SECUNDARIA], activo)
  - grados: (id, nombre, nivel_id[FK], activo)
  - secciones: (id, nombre, grado_id[FK], turno[MANANA/TARDE], capacidad_maxima, activa, anio_escolar, docente_tutor_id)
  - aulas: (id, nombre, pabellon, capacidad_maxima, turno_disponible[MANANA/TARDE/AMBOS], activa)
  - docentes: (id, nombre, apellido, dni[UK], email[UK], especialidad, estado)
  - cursos: (id, codigo[UK], nombre, creditos, tipo, grado_id[SoftFK], estado)
  - horarios: (id, dia_semana, hora_inicio, hora_fin, turno, aula_id[SoftFK], seccion_id[SoftFK], curso_id[FK], docente_id[FK])
  - periodos_escolares: (id, anio_escolar, nombre, fecha_inicio, fecha_fin, estado[PLANIFICADO/ACTIVO/SUSPENDIDO/CERRADO], motivo_suspension)
  - tickets_mantenimiento: (id, docente_id[SoftFK], aula_id[SoftFK], categoria, descripcion, urgencia, estado)

[AcademicoEvaluaciones]
  - notas: (id, estudiante_id[SoftFK], curso_id[SoftFK], docente_id[SoftFK], seccion_id[SoftFK], competencia, bimestre, escala_literal[AD/A/B/C], valor_numerico[DECIMAL], fecha_registro[DATETIME2])
  - asistencias: (id, fecha, estado[PRESENTE/FALTA/TARDE/JUSTIFICADO], estudiante_id[SoftFK], horario_id[SoftFK], docente_id[SoftFK], justificacion)

[AcademicoDisciplina]
  - reportes_disciplinarios: (id, estudiante_id[SoftFK], docente_id[SoftFK], seccion_id[SoftFK], tipo[TARDANZA/FALTA_LEVE/FALTA_GRAVE/MERITO], descripcion, estado, apoderado_notificado[BIT], fecha_reporte)
```

---

## 4. Reglas de Negocio Clave
1. **Evaluación por Competencias MINEDU:** Cuaderno digital con escala literal obligatoria (`AD` = Logro Destacado, `A` = Logro Esperado, `B` = En Proceso, `C` = En Inicio) y conversión a escala vigesimal (`0 - 20`).
2. **Contexto Regional Huamanga (Ayacucho):**
   - **Multi-Turno:** Mismas aulas físicas compartidas entre turno mañana y tarde sin cruces de horarios.
   - **Congelamiento de Calendario:** Posibilidad de suspender cronogramas escolares en bloque por festividades (Semana Santa) o lluvias/huelgas (`cursos.calendario.congelado`), pausando asistencias y límites de notas automáticamente.
3. **Seguridad y Roles (RBAC):**
   - `DIRECTOR`: Control total de matrículas, horarios y analítica de deserción.
   - `DOCENTE`: Solo ingresa notas/asistencias de su carga horaria asignada; emite tickets de averías y papeletas.
   - `SECRETARIA`: Digitación de matrículas, validación de aforo y carga de legajos en base de datos.
   - `ENFERMERA`: Acceso exclusivo al módulo de tópico y fichas médicas (sin acceso a notas ni conducta).
4. **Caché y Consistencia Eventual:**
   - Patrón *Cache-Aside* en Redis (`app:val:{modulo}:{entidad}:{id}`).
   - Datos estáticos (Grados/Cursos): TTL 12h-24h. Datos dinámicos (Alumnos/Docentes): TTL 15m-30m.
   - Invalidación reactiva vía RabbitMQ (`academic.exchange.topic` con routing key `val.entity.changed.*`).

---

## 5. Reglas de Frontend y UI
- **Estética:** *Glassmorphism* (fondos translúcidos, `backdrop-filter: blur`, bordes sutiles) + *Dark Theme* elegante.
- **Micro-interacciones (Emil Kowalski):** Animaciones fluidas, feedback inmediato de guardado sin recargar.
- **Iconos:** ESTÁ PROHIBIDO EL USO DE EMOJIS. Utilizar única y exclusivamente `bootstrap-icons` (ej. `<i class="bi bi-person"></i>`).
- **Ergonomía Docente (PWA):** Cuadrícula tipo Excel para aulas numerosas (>35 alumnos), scroll virtual, ciclado rápido de teclas (`A` -> `AD`) y funcionamiento *Offline-First* con `IndexedDB`.

---

## 6. Estructura del Repositorio
```text
/
├── backend/
│   ├── api-gateway/            # Puerto 8080 (Spring Cloud Gateway)
│   ├── servicio-estudiantes/   # Puerto 8082 (DB AcademicoEstudiantes)
│   ├── servicio-cursos/        # Puerto 8083 (DB AcademicoCursos)
│   ├── servicio-evaluaciones/  # Puerto 8084 (DB AcademicoEvaluaciones)
│   ├── servicio-disciplina/    # Puerto 8081 (DB AcademicoDisciplina)
│   └── servicio-reportes/      # Puerto 8085 (Agregador WebClient & IA)
├── frontend/                   # Angular 21 SPA/PWA + Playwright Tests
├── docker-compose.yml          # Orquestación de BD y servicios
├── napkin.md                   # Bitácora histórica de errores y soluciones
├── PAUTAS_DESARROLLO_SISTEMA.md# Pautas maestras del proyecto
└── DISENO_BASE_DE_DATOS.pdf    # Diagramas y diccionario técnico exportado
```

---

## 7. Instrucciones para Claude
- **Tono:** Directo, conciso, orientado al código y a la solución técnica.
- **Estándares:** Siempre mantener separación estricta de microservicios, compatibilidad con Java 17 / Spring Boot 3 / Angular 21, cero emojis en frontend y registrar fallos en `napkin.md`.
