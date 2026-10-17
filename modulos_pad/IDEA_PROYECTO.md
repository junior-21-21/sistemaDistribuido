# Plataforma Académica Distribuida para Instituciones Educativas

## 1. Planteamiento del Problema y Filosofía del Sistema
Las instituciones educativas requieren sistemas escalables para gestionar estudiantes y cursos de manera eficiente. El crecimiento del volumen de datos y los picos de tráfico exigen una solución tecnológica que no colapse, asegurando alta disponibilidad y concurrencia.

Este proyecto nace bajo dos pilares innegociables:
1. **Hacerle la vida fácil al usuario:** Liberar a docentes y administrativos de la burocracia del papel, automatizando tareas repetitivas (como promedios o búsqueda de legajos) mediante interfaces rápidas, amigables y pensadas para la realidad del colegio.
2. **Cumplir estrictamente con las normativas de Educación:** Garantizar que todo flujo (evaluación por competencias, traslados, emisión de actas) sea 100% compatible y alineado con los requerimientos legales del Ministerio de Educación (ej. SIAGIE).

## 2. Servicios Sugeridos (Ecosistema de Microservicios)
El sistema se divide en dominios independientes, lo que permite que si un módulo experimenta fallos o alta demanda, el resto de la plataforma siga operando con normalidad.

* **Servicio de Estudiantes (Uso Administrativo):** Gestiona el ciclo de vida del alumno y su historial. Es de uso exclusivo para secretaría y dirección. Permite mantener el legajo virtual y justificaciones de asistencia. Incluye un submódulo crítico de **Gestión de Apoderados**, que registra quién es el tutor legal autorizado (padres, abuelos, etc.) para retirar al menor del colegio, garantizando la seguridad y responsabilidad legal.
* **Servicio de Cursos:** Administra el catálogo institucional. Abarca la programación del año escolar, la estructuración de niveles, grados y secciones, y la publicación de la agenda digital del aula.
* **Servicio de Evaluaciones y Disciplina:** Motor central del Portal Docente. Procesa las calificaciones y aloja el **Módulo de Comportamiento**, que reemplaza las antiguas "papeletas físicas" por un registro digital e inmediato de incidencias (tardanzas, méritos y faltas disciplinarias). También incluye el Módulo de Bienestar escolar (prevención de fatiga, alertas de sobrecarga de exámenes).
* **Servicio de Reportes e Integración Gubernamental:** Módulo de analítica y cumplimiento. Genera alertas tempranas de vulnerabilidad (riesgo de deserción), emite documentos oficiales y plantillas compatibles con los sistemas del Ministerio de Educación (actas de evaluación consolidadas), agilizando el trabajo burocrático del docente.

## 3. Funcionalidades Core
* **Matrículas, Traslados e Inclusión:** Flujo automatizado para ratificaciones y traslados. Incluye validación de vacantes respetando la capacidad máxima por aula para evitar hacinamiento. Añade **Gestión Documental Digital**, permitiendo a secretaría adjuntar escaneos de DNI, partidas de nacimiento o resoluciones de traslado (SIAGIE) directamente en la ficha del alumno, eliminando los archiveros físicos de cartón.
* **Gestión de Cursos:** Creación de sílabos, asignación de docentes evitando conflictos de horario y aplicación de reglas ergonómicas para evitar jornadas maratónicas o "horas puente" excesivas en el personal.
* **Registro de Notas (Alineación MINEDU):** Cuaderno digital estrictamente alineado con las normativas vigentes. Soporta de forma nativa la **Evaluación por Competencias** (escalas literales AD, A, B, C) con conversión algorítmica y promedios automáticos, además del sistema vigesimal clásico para ciertos grados. Cuenta con fórmulas predefinidas y un historial inmodificable para auditoría de cambios.

## 4. Conceptos Distribuidos Aplicados
Para que la plataforma logre una optimización real de la gestión académica y soporte concurrencia a nivel Enterprise, se sostiene en tres pilares tecnológicos:

* **Microservicios:** Cada uno de los servicios sugeridos (Estudiantes, Cursos, Evaluaciones, Reportes) opera como una aplicación independiente. Esto significa que se pueden asignar más recursos al Servicio de Matrículas durante inicio de año escolar sin afectar el costo o rendimiento del Servicio de Evaluaciones.
* **Docker:** Toda la plataforma se empaqueta en contenedores aislados. Esto garantiza despliegues predecibles: el código funcionará exactamente igual en el entorno de desarrollo local que en los servidores de producción, facilitando actualizaciones rápidas sin tiempos de inactividad.
* **Bases de Datos Distribuidas:** Se abandona el modelo tradicional de una única base de datos centralizada que representa un punto único de fallo. El Servicio de Estudiantes y Matrículas puede utilizar una base de datos relacional para garantizar la exactitud en cupos y registros, mientras que el Servicio de Reportes puede consultar bases de datos no relacionales optimizadas para lectura masiva, impidiendo que la generación de un reporte pesado bloquee el sistema.

## 5. Adaptaciones Estratégicas para el Sector Público
Al tratarse de una institución pública, el sistema debe contemplar la realidad socioeconómica y operativa:
* **Accesibilidad Ligera para Docentes (PWA):** Dado que los docentes pueden necesitar ingresar desde sus propios celulares en las aulas y los colegios a veces tienen internet limitado, el portal docente se diseñará como una Aplicación Web Progresiva (PWA) de muy bajo consumo, facilitando su labor sin requerir equipos de alta gama.
* **Eficiencia de Costos en la Nube:** Al manejar presupuestos limitados, la arquitectura de microservicios permitirá "apagar" o reducir al mínimo los servidores durante las noches o vacaciones (auto-scaling), generando un ahorro drástico en costos de infraestructura.
* **Flujos Masivos para Docentes:** Los profesores del sector público suelen manejar aulas sobrepobladas. El módulo de evaluaciones debe priorizar el ingreso rápido de notas (atajos de teclado, importación masiva por Excel) y el autocompletado para no sobrecargar su labor administrativa.

## 6. Gestión Compleja de Horarios y Turnos (Contexto Regional - Huamanga)
Dado que el colegio opera en Huamanga, Ayacucho, existen factores logísticos y regionales críticos que el sistema debe resolver automáticamente:
* **Manejo de Multi-Turno (Mañana y Tarde):** El módulo de Cursos y Horarios debe gestionar turnos independientes. Esto implica que una misma "Aula Física" (ej. Pabellón A, Aula 101) es ocupada por un grado en la mañana y por otro distinto en la tarde. El sistema requiere un control estricto de cruce de espacios físicos para asegurar que ningún ambiente sea doblemente asignado en el mismo rango horario.
* **Optimización para Docentes en Ambos Turnos:** Muchos docentes enseñan tanto en el turno mañana como en el de la tarde. El motor de horarios (Horarios Inteligentes) debe evitar generar "horas puente" excesivas (huecos de 3 o 4 horas libres no remuneradas entre clases) o sobrecarga continua, respetando el bienestar y la jornada laboral del maestro.
* **Flexibilidad del Calendario (Festividades y Clima):** En la sierra peruana, el calendario escolar suele verse interrumpido por festividades patronales (ej. Semana Santa en Ayacucho), huelgas o emergencias climáticas. El sistema debe tener la capacidad de "congelar" y desplazar todo el cronograma escolar y las asistencias en bloque de manera masiva, sin que el director tenga que mover las fechas de cada curso manualmente.

## 7. Módulos Adicionales y Tecnologías de Vanguardia (El Factor Único)
Para que esta plataforma sea una solución verdaderamente innovadora y única en el mercado, suma los siguientes valores agregados de alto impacto tecnológico:
* **Certificados Incorruptibles con Blockchain (Confianza Distribuida):** El mayor problema en el sector público es la falsificación de documentos. Al graduarse un alumno, su Certificado Oficial de Estudios se emite como un registro criptográfico inmutable en una red Blockchain ligera. Cualquier universidad puede escanear el código QR del certificado y la red distribuida validará matemáticamente que el documento es 100% auténtico y que sus notas jamás fueron adulteradas.
* **Inteligencia Artificial Preventiva (Machine Learning):** El módulo de "Alertas de Deserción Escolar" no usa simples reglas de negocio, sino un modelo predictivo. La IA analiza los patrones de tardanzas, notas y comportamiento del alumno para alertar a los tutores semanas antes de que ocurra una deserción, permitiendo intervención temprana.
* **Asistencia y Registro en Tiempo Real:** Todo el registro (notas, asistencias) se realizará de manera 100% online (dentro de la red del colegio), garantizando que la información y las alertas tempranas sean precisas y en tiempo real.
* **Identificación de Bajo Costo (Códigos QR):** Carnets de estudiante con Códigos QR únicos escaneables desde cualquier celular de gama baja en la puerta del colegio.
* **Módulo de Comunicación con Apoderados:** Alertas automatizadas vía WhatsApp o SMS para notificar inasistencias o citaciones urgentes directamente al tutor legal.
* **Módulo de Salud Escolar y Tópico:** Ficha médica digital (alergias, tipo de sangre, condiciones crónicas) y bitácora de atenciones gestionada por la Enfermera.
* **Módulo de Mantenimiento e Infraestructura:** Sistema rápido de tickets desde la PWA del docente para reportar daños en su aula (mobiliario roto, fallas eléctricas).
* **Tutoría y Orientación Psicológica:** Registro de derivaciones confidenciales al psicólogo del colegio y seguimiento de orientación vocacional.

## 8. Stack Tecnológico y Arquitectura de Software
Para asegurar que el sistema sea mantenible, escalable y robusto, se ha definido el siguiente ecosistema tecnológico (pensado en una primera fase para despliegue en servidores locales vía contenedores):

* **Backend - Spring Boot (Java):** Se construirá la lógica de los microservicios utilizando el framework Spring Boot. Este estándar de la industria ofrece una gestión excelente de la concurrencia, seguridad integrada (Spring Security) y un ecosistema preparado para trabajar de forma nativa con arquitecturas distribuidas.
* **Frontend - Angular:** Las interfaces (tanto para el personal administrativo como para los docentes) se desarrollarán con Angular. Su arquitectura basada en componentes, inyección de dependencias y el uso de TypeScript garantizarán un código ordenado y facilitarán la conversión del sistema a una PWA (Aplicación Web Progresiva).
* **Base de Datos - PostgreSQL:** Se empleará PostgreSQL como motor de base de datos relacional primario.
* **Diseño y Normalización de Datos:** Se aplicarán estrictos estándares de ingeniería de software en el diseño de la base de datos. Esto incluye la **Normalización** (aplicando como mínimo la 3ra Forma Normal) para asegurar que no haya redundancia de información ni anomalías de actualización (ej. evitar que el nombre de un alumno se guarde en múltiples tablas). Además, se configurarán índices estratégicos, transacciones ACID y claves foráneas correctas para mantener la integridad referencial a pesar del alto volumen de matrículas y registros de un colegio público.

## 9. Seguridad, Auditoría y Continuidad del Negocio
Tratándose de registros académicos oficiales y datos de menores de edad, el sistema debe ser impenetrable y confiable:
* **Trazabilidad y Auditoría (Logs Inmutables):** Se implementará una tabla o servicio de auditoría estricto. Cada vez que un usuario modifique un dato crítico (ej. cambiar una nota final, eliminar una inasistencia o trasladar a un alumno), el sistema registrará silenciosamente quién realizó la acción (ID del usuario), la fecha/hora exacta, la dirección IP, el valor anterior y el valor nuevo. Esto previene fraudes y protege a la institución ante reclamos.
* **Política de Respaldos (Backups Automatizados):** Para cumplir con la normativa de protección de datos y evitar catástrofes informáticas, la arquitectura ejecutará copias de seguridad automáticas de PostgreSQL diariamente en horas de madrugada. En caso de fallo de hardware o ataques, el sistema podrá restaurarse al último punto verificado.

## 10. Roles y Perfiles de Acceso (RBAC Detallado)
El sistema estará enfocado principalmente en empoderar y facilitar el trabajo del Director y los Profesores, con una estricta separación de funciones:

* **Rol Director / Equipo Directivo:**
  * **Accesos:** Visibilidad y control total (lectura y escritura) sobre la estructuración institucional.
  * **Funciones Clave:** Aprobación final de Matrículas y traslados, creación y asignación de Horarios (evitando cruces de turno), estructuración del año escolar separando niveles (Primaria y Secundaria) con sus respectivos grados y secciones. Recibe reportes de mantenimiento, monitorea el panel de "Alertas de Deserción", estadísticas de rendimiento y envía comunicados masivos.
* **Rol Docente (Profesor):**
  * **Accesos:** Restringidos únicamente a los cursos, secciones y alumnos que figuran en su carga horaria asignada.
  * **Funciones Clave:** Ingreso ágil de calificaciones, registro de asistencia, anotación de incidencias de comportamiento (papeletas digitales), creación de tickets de mantenimiento para su aula y derivación de casos a tutoría/psicología.
* **Rol Enfermera / Personal de Tópico:**
  * **Accesos:** Restringido al módulo de Salud Escolar, sin acceso a notas ni comportamiento.
  * **Funciones Clave:** Llenado de fichas médicas de los estudiantes, registro de vacunas y tamizajes, y bitácora de emergencias o accidentes ocurridos dentro del colegio.
* **Rol Secretaría y Auxiliares (Soporte):**
  * **Accesos:** Gestión administrativa y operativa sin permisos sobre calificaciones académicas.
  * **Funciones Clave:** La Secretaría realiza el trabajo pesado de **registro y digitación de matrículas** (búsqueda de alumnos, escaneo de documentos), emisión de certificados y actualización del legajo digital para que el Director solo deba aprobarlas. Los auxiliares escanean los QR de ingreso, gestionan tardanzas masivas y hacen seguimiento a citaciones.
