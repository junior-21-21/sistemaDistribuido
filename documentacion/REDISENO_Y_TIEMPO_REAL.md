# Rediseño de la interfaz y actualizaciones en tiempo real

## Qué cambió para quien usa el sistema

- **Tema claro por defecto** (pensado para aulas y proyectores) y **tema oscuro** con un clic
  (luna/sol en la barra superior). Se recuerda por equipo.
- **Menú lateral con nombres**, agrupado por áreas: Académico, Convivencia y bienestar,
  Secretaría y Gestión. Se puede compactar a solo íconos. En el celular es un cajón deslizable.
- **Buscador universal**: `Ctrl + K` (o `/`) abre una lista para ir a cualquier pantalla
  escribiendo, por ejemplo, "notas", "faltas" o "matricular".
- **Inicio por rol**: saludo, la acción principal del rol destacada (Docente → Asistencia,
  Secretaría → Matricular, Mantenimiento → Tickets…), indicadores (Dirección), "Mis clases de
  hoy" con la clase en curso y la siguiente (Docente), tickets por atender (Mantenimiento) y la
  actividad del colegio.
- **Login nuevo**: recordar usuario, aviso de mayúsculas activadas y ayuda si olvidaste la clave.
- Textos corregidos con tildes y estados vacíos que explican qué hacer.

## Tiempo real

Cuando alguien registra notas, asistencia, una incidencia, un ticket, una matrícula, etc.:

1. Las personas cuyo rol puede ver ese módulo reciben **al instante** un aviso en la esquina,
   la campana suma un número y el menú marca la pantalla afectada.
2. Las listas (estudiantes, convivencia, psicología, secciones, cursos, cuentas, auditoría,
   notificaciones, portal de familias, inicio) **se actualizan solas**.
3. En pantallas donde se está escribiendo (notas, asistencia, horarios, gestor de aula,
   tópico, tickets) aparece un aviso **"Actualizar"** para no borrar lo que la persona escribe.
4. El indicador **En vivo** de la barra superior muestra el estado de la conexión y se
   reconecta solo si se cae la red.

### Cómo funciona

- El **API Gateway** es la única puerta de escritura. Un filtro (`CambiosGlobalFilter`)
  detecta cada `POST/PUT/PATCH/DELETE` terminado en 2xx y publica un evento con el módulo,
  la acción y el nombre de quien lo hizo. **No envía datos del registro**: la pantalla vuelve
  a consultar la API con sus propios permisos.
- El canal es **Server-Sent Events** en `GET /api/tiempo-real/stream` (requiere JWT). Cada
  usuario solo recibe eventos de módulos que su rol puede ver (`TiempoRealService.AUDIENCIA`).
  Por ejemplo, Tópico solo llega a Enfermería.
- Nginx sirve ese camino sin buffer y con conexión larga; hay un latido cada 25 s.
- En Angular, `RealtimeService` mantiene la conexión con reintentos exponenciales y expone
  `alCambiar('notas', …)` para que cada pantalla decida si recarga o muestra el aviso.

Prueba rápida desde la terminal (con un token válido):

```bash
curl -N -H "Authorization: Bearer <token>" http://localhost/api/tiempo-real/stream
```

## Carga con un solo clic y conexión entre servicios

- La interfaz usa Zone.js: cada respuesta del servidor actualiza la pantalla al instante (antes,
  en modo "zoneless", varias pantallas solo se pintaban con un segundo clic).
- Las consultas (GET) se reintentan solas ante fallos pasajeros (0, 502, 503, 504) y, si un
  servicio no responde, aparece un aviso claro.
- Gateway: el límite de espera por defecto de 1 s cortaba con error cualquier consulta lenta;
  ahora es de 25 s. Cada ruta tiene su propio circuit breaker, así que la caída de un servicio
  no bloquea a los demás, y responde 503 con un mensaje legible.
- Nginx vuelve a resolver la IP del gateway si este se reinicia.
- Los errores de petición (parámetro faltante, ruta inexistente) responden 400/404/405 en lugar de 500.

## Horarios con parámetros y edición libre

1. **Turno y jornada**: hora de inicio, minutos por período (40/45/50/60), períodos por día,
   días de clase (incluido sábado) y recreos (después de qué período y cuántos minutos).
2. **Reglas**: máximo del mismo curso por día, máximo de horas seguidas por docente, bloques
   dobles y aula fija por sección. Nunca se cruza un docente ni un aula.
3. **Generar** todo el turno, un grado o una sección; "Otra variante" propone otra distribución.

Después se edita a mano: arrastrar una clase para moverla o intercambiarla, arrastrar un curso
pendiente a una hora libre, clic en una hora para elegir curso (con el motivo si no se puede),
fijar con el alfiler lo que debe conservarse al regenerar y **Deshacer**. "Guardar todo" publica
todas las secciones en una sola operación (`PUT /api/horarios/lote`): si algo no cuadra no se guarda
nada. Los parámetros se recuerdan por turno en el navegador.

Si un docente tiene más períodos que la jornada (por ejemplo 50 en una semana de 35), el
generador lo indica por su nombre para que se reparta la carga.
