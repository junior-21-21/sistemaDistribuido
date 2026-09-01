# Descripción del Proyecto: Plataforma Académica Distribuida

Este documento describe la arquitectura, tecnologías y estructura del proyecto "Plataforma Académica Distribuida". Su objetivo principal es servir como base de conocimiento para la redacción de informes técnicos, documentación de arquitectura o inducción de nuevos desarrolladores.

---

## 1. Visión General del Sistema
El sistema es una plataforma web orientada a la gestión y administración académica de una institución educativa. Provee distintos módulos para gestionar estudiantes, cursos, horarios, evaluaciones, reportes, disciplina y atención médica (tópico/fichas médicas).

Cuenta con diferentes roles de usuario, entre los que destacan:
- **Director / Administrador**
- **Docente**
- **Secretaría**
- **Enfermera**

La arquitectura está basada en **Microservicios**, donde el backend está fragmentado en distintos dominios de negocio conectados mediante un API Gateway, y el frontend es una **Single Page Application (SPA)**.

---

## 2. Arquitectura de Software

El sistema sigue un patrón de microservicios con las siguientes capas:

### 2.1 Backend (Java 17 + Spring Boot 3.x)
El backend utiliza las tecnologías del ecosistema de Spring y se divide en varios servicios independientes:

- **API Gateway (`api-gateway`)**: Sirve como punto único de entrada al sistema, enrutando las peticiones del frontend a los microservicios correspondientes utilizando `Spring Cloud Gateway`. Implementa tolerancia a fallos mediante el patrón Circuit Breaker (`resilience4j`).
- **Servicio de Estudiantes (`servicio-estudiantes`)**: Gestiona la información personal y las matrículas de los estudiantes. Utiliza `hibernate-envers` para mantener una auditoría de cambios en los registros.
- **Servicio de Cursos (`servicio-cursos`)**: Administra el catálogo de asignaturas y cursos dictados en la institución.
- **Servicio de Evaluaciones (`servicio-evaluaciones`)**: Responsable de la carga de notas, promedios y rúbricas de evaluación.
- **Servicio de Disciplina (`servicio-disciplina`)**: Administra reportes de incidencias, anotaciones por comportamiento y méritos.
- **Servicio de Reportes (`servicio-reportes`)**: Un servicio de agregación que consulta datos (vía HTTP) de otros microservicios (Estudiantes, Evaluaciones, Disciplina) para generar estadísticas y reportes consolidados.

**Dependencias Técnicas Notables en Backend:**
- **Lombok**: Para reducir el código repetitivo en las clases modelo.
- **MapStruct**: Para el mapeo automático entre Entidades (JPA) y DTOs (Data Transfer Objects).
- **Spring Security**: Para manejar la autenticación y autorización en los servicios críticos.
- **Spring Data JPA**: Como ORM principal para interactuar con la base de datos.

### 2.2 Base de Datos (SQL Server 2022)
El motor de base de datos relacional elegido es **Microsoft SQL Server 2022**. 
Cumpliendo con las buenas prácticas de microservicios, cada servicio maneja su propia base de datos lógica (por ejemplo: `AcademicoEstudiantes`, `AcademicoCursos`, etc.), evitando el acoplamiento a nivel de datos.

### 2.3 Frontend (Angular 21)
El cliente es una aplicación construida en **Angular v21** y TypeScript. 
- **Estilos y UI:** Se utiliza CSS puro potenciado por **Bootstrap 5** (solo para la grilla y utilidades). La interfaz sigue estrictamente un diseño visual de **"Glassmorphism"** combinado con un **"Dark Theme"** elegante.
- **Iconografía:** Todos los íconos del sistema son generados mediante `bootstrap-icons` (se ha evitado el uso de emojis por reglas de diseño).
- **PWA (Progressive Web App):** Tiene configurados Service Workers (`@angular/service-worker`), lo que indica la intención de cachear recursos o trabajar offline.
- **Testing:** Posee integración con **Playwright** para la ejecución de pruebas automatizadas End-to-End (E2E).

---

## 3. Infraestructura y Despliegue (Docker)
Todo el sistema está contenedorizado usando **Docker** y **Docker Compose**. Esto permite levantar toda la infraestructura (Base de Datos + API Gateway + 5 Microservicios + Frontend) en un entorno local con un único comando:

```bash
# Levantar el entorno completo en segundo plano
docker-compose up -d
```

### Puertos de los servicios en desarrollo:
- **Frontend Angular:** `http://localhost:80`
- **API Gateway:** `http://localhost:8080`
- **Servicio Estudiantes:** `8082`
- **Servicio Cursos:** `8083`
- **Servicio Evaluaciones:** `8084`
- **Servicio Disciplina:** `8081`
- **Servicio Reportes:** `8085`
- **Base de Datos SQL Server:** `1433`

---

## 4. Estructura de Directorios

El repositorio se organiza de la siguiente manera:
```text
/
├── backend/                  # Código fuente de todos los microservicios
│   ├── api-gateway/          # Enrutador
│   ├── servicio-cursos/      # Módulo de cursos
│   ├── servicio-disciplina/  # Módulo de comportamiento
│   ├── servicio-estudiantes/ # Módulo de estudiantes
│   ├── servicio-evaluaciones/# Módulo de calificaciones
│   └── servicio-reportes/    # Módulo de reportes (agregador)
├── frontend/                 # Proyecto Angular (SPA)
│   ├── src/                  # Componentes y páginas
│   └── tests/                # Pruebas E2E (Playwright)
├── SistemaAcademico/         # (Directorio adicional/legado)
├── docker-compose.yml        # Orquestación de contenedores Docker
└── napkin.md                 # Registro histórico de errores (Troubleshooting)
```

## 5. Prácticas y Reglas del Proyecto
- **Diseño Impecable:** La UI prioriza animaciones fluidas, contrastes limpios y un efecto visual de "cristal esmerilado".
- **Registro de Errores (`napkin.md`):** Se mantiene una bitácora de los problemas para evitar volver a cometerlos durante las iteraciones de despliegue.
- **Autenticación (Futuro MCP):** Existen miras para integrar capacidades de IA, permitiendo la indexación de los documentos y bases de conocimiento del sistema (Ej. a través de NotebookLM).
