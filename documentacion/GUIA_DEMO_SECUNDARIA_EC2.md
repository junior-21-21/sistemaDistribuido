# Guía de demostración de secundaria en EC2

## Objetivo y tamaño del escenario

Este escenario no pretende certificar una operación escolar real. Su objetivo es que la
presentación final muestre volumen, separación de roles y recorridos funcionales coherentes en
una sola instancia EC2.

La carga secundaria genera:

| Elemento | Cantidad |
|---|---:|
| Grados de secundaria | 5 |
| Secciones | 10: A y B por grado |
| Estudiantes | 200: 20 por sección, 40 por grado |
| Áreas por grado | 15 |
| Cursos registrados | 75 |
| Asignaciones curso-docente-sección | 150 |
| Docentes del catálogo secundario | 30 |
| Períodos semanales por sección | 35 |
| Bloques de horario | 350 |
| Notas sintéticas de primer bimestre | 400 |
| Asistencias sintéticas | 1,000 |
| Reportes disciplinarios sintéticos | 20 |
| Decisiones de consentimiento | 600 |

Los 20 estudiantes base creados por `SEED_DEMO_DATA` pertenecen al escenario funcional general.
Por ello la base puede mostrar 220 estudiantes totales, pero el padrón de secundaria mantiene
exactamente 200 y 20 en cada sección.

Al preparar este escenario, el script desactiva las asignaciones y retira los horarios académicos
sintéticos de primaria correspondientes a 2026. No elimina las fichas de esos 20 estudiantes base:
siguen disponibles para demostrar salud, psicología y portal. La medida evita cruces artificiales
de docentes o aulas y deja la carga académica activa concentrada en secundaria.

## Organización académica usada

- Sección A de cada grado: turno mañana, de 07:30 a 13:15.
- Sección B de cada grado: turno tarde, de 13:30 a 19:15.
- Cada turno tiene siete períodos pedagógicos de 45 minutos y dos descansos.
- Hay seis aulas compatibles por turno; cinco se usan simultáneamente y una queda disponible.
- Cada sección tiene capacidad 35, pero solo se cargan 20 estudiantes.

La carga semanal de cada sección suma 35 períodos:

| Área | Períodos |
|---|---:|
| Matemática | 5 |
| Comunicación | 5 |
| Ciencia y Tecnología | 4 |
| Ciencias Sociales | 3 |
| Educación Física | 3 |
| Inglés, DPCC, EPT, Arte y Religión | 2 cada una |
| Tutoría, Quechua, TIC, Razonamiento y Plan Lector | 1 cada una |

## Cuántos docentes tienen sentido

El mínimo matemático sería `350 / 30 = 11.67`, es decir, 12 docentes con 30 períodos, pero ese
número ignora especialidades y cruces simultáneos. Una plantilla ajustada podría funcionar con
18 docentes: Matemática, Comunicación y Ciencia separados por turno, y doce especialidades
compartidas entre mañana y tarde.

Para la demostración se usan **30 docentes**, uno por área y turno. Es deliberadamente holgado:
facilita explicar las especialidades, evita cruces y permite que el generador encuentre una
solución completa de forma estable. Ningún docente supera sus 30 períodos contratados dentro del
escenario secundario. Diez de ellos son además tutores de sección.

## Preparar EC2

Para ejecutar todos los microservicios, PostgreSQL, RabbitMQ y el frontend en la misma máquina se
recomienda al menos 4 vCPU, 8 GiB de memoria y 30 GiB de disco. Para la exposición pública abre
solo 80/443; restringe SSH a la IP del expositor. No es necesario publicar PostgreSQL, RabbitMQ ni
el puerto interno del gateway.

1. Copia `.env.example` a `.env`.
2. Define contraseñas y secretos distintos de los ejemplos.
3. Establece `SEED_DEMO_DATA=true` y conserva `NOTIFICATIONS_MODE=SIMULATED`.
4. Levanta y espera el stack:

~~~bash
docker compose up --build -d
docker compose ps
~~~

En un volumen nuevo, Flyway crea los esquemas y `servicio-estudiantes` crea las cuentas de rol.
Si activaste `SEED_DEMO_DATA` después del primer arranque, reinicia ese servicio antes de poblar:

~~~bash
docker compose restart servicio-estudiantes
~~~

Las cuentas demo exigen cambiar la contraseña en el primer ingreso. Haz ese recorrido antes del
día de exposición y guarda las nuevas claves de demostración; el seed no restablece contraseñas
que ya fueron cambiadas.

## Modo 1: datos listos para presentar

Este modo carga los 200 estudiantes y deja persistidos los 350 bloques sin conflictos.

En EC2/Linux:

~~~bash
sh ops/demo/prepare-secundaria.sh 20 ready
~~~

En Windows/PowerShell:

~~~powershell
.\ops\demo\prepare-secundaria.ps1 -EstudiantesPorSeccion 20
~~~

El comando termina con `DEMO SECUNDARIA OK`. Puede repetirse: los registros se actualizan o se
ignoran por su clave estable, sin duplicar el padrón.

## Modo 2: probar el generador de horarios

Este modo conserva cursos, docentes, secciones y 150 asignaciones, pero elimina los horarios de
secundaria y la asistencia sintética que dependía de ellos.

~~~bash
sh ops/demo/prepare-secundaria.sh 20 generator
~~~

~~~powershell
.\ops\demo\prepare-secundaria.ps1 -EstudiantesPorSeccion 20 -VaciarHorarios
~~~

Después:

1. Ingresa como `coord.academica`.
2. Abre **Carga y horarios**.
3. Selecciona turno mañana y pulsa **Generar propuesta**.
4. Revisa cada sección y el filtro por docente. No debe haber un docente ni aula repetidos en el
   mismo período.
5. Guarda las cinco secciones del turno mañana, una por una.
6. Cambia a turno tarde, genera la segunda propuesta y guarda sus cinco secciones.
7. Exporta una sección como CSV y otra mediante la vista de impresión/PDF.
8. Ejecuta el verificador con `esperar_horarios=true`:

~~~bash
docker compose --profile demo run --rm --no-deps --entrypoint psql demo-data \
  -h postgres -U postgres -v estudiantes_por_seccion=20 -v esperar_horarios=true \
  -f /demo/verify-secundaria-2026.sql
~~~

El backend rechazará bloques que excedan los períodos de la asignación, crucen docente/aula,
usen un turno incorrecto, un aula inactiva o un aula menor que la capacidad de la sección.

## Recorrido recomendado para la exposición

1. **Secretaría:** filtra los 200 estudiantes, abre una ficha, revisa apoderado y consentimientos.
2. **Coordinación académica:** muestra las 150 cargas, genera el horario y cierra el primer
   bimestre con motivo.
3. **Docente:** comprueba que solo ve sus secciones; registra nota, asistencia y una incidencia.
4. **Coordinación de convivencia:** revisa los 20 casos sintéticos y su estado.
5. **Enfermería y Psicología:** enseña ficha médica, atención y derivación de los estudiantes base.
6. **Mantenimiento:** registra y cambia el estado de un ticket de aula.
7. **Apoderado:** consulta el portal con una de las cuentas demo ya preparada.
8. **Director:** consulta reportes y auditoría, y reabre el bimestre indicando el motivo.
9. **Administrador del sistema:** muestra usuarios, multirrol y activación/desactivación sin entrar
   en información académica.

## Qué observar durante la prueba de volumen

- Los listados deben paginar; no intentes renderizar los 200 estudiantes en una sola tabla.
- El docente `docente1` debe ver las secciones A de Matemática que tiene asignadas, no las B.
- Cada sección secundaria debe devolver exactamente 20 estudiantes.
- El planificador debe completar 35 períodos por sección.
- El cierre del bimestre debe impedir altas, cambios y eliminaciones de notas.
- Tras reabrir, la edición vuelve a estar disponible y las dos acciones quedan en auditoría.
- `docker compose logs --since 10m` no debe mostrar errores repetitivos ni reinicios de servicios.

No ejecutes `docker compose down -v` antes de la exposición: elimina el volumen de PostgreSQL. Si
quieres ensayar desde cero, haz primero un backup y reserva esa operación para un ambiente local.
