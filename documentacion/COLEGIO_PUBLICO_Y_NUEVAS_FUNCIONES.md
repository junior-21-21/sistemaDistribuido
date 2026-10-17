# Cómo funciona un colegio público (MINEDU) y qué hace el sistema

Resumen operativo para orientar el diseño. Verificar siempre la norma vigente del año
(RM de matrícula, norma de evaluación) en gob.pe/minedu o con la UGEL.

## Matrícula

- **Es gratuita.** No puede condicionarse a la cuota de APAFA, uniformes, útiles, donaciones
  ni exámenes. El aporte a APAFA es voluntario y se gestiona aparte. *El sistema no cobra
  matrícula y lo muestra en el formulario.*
- **Único documento exigible:** DNI del estudiante o acta de nacimiento.
- **Tipos:** nuevo ingreso, continuidad (quien ya estudiaba y fue promovido solo confirma
  que continúa) y traslado desde otra IE.
- **Traslado entrante:** el apoderado presenta la Ficha Única de Matrícula del SIAGIE; la
  resolución directoral de traslado la emite el colegio de destino; el certificado de
  estudios no es requisito.
- Todo se registra luego en el **SIAGIE** (trayectoria del estudiante).

**En el sistema:** matrícula por año con tipo y procedencia (tabla `matriculas`), aviso de
gratuidad y requisitos en pantalla, y "Trasladar a otra IE" desde la lista de estudiantes
(el estudiante queda como TRASLADADO y conserva todo su historial).

## Evaluación y promoción

- Escala del Currículo Nacional (CNEB): **AD** logro destacado, **A** logro esperado,
  **B** en proceso, **C** en inicio, por competencia.
- Al cierre del año el estudiante puede ser **promovido**, ir a **recuperación pedagógica**
  o **permanecer** en el grado. Quien no aprueba un área puede subsanarla (carpeta o periodo
  de recuperación). Las reglas exactas dependen de la norma de evaluación vigente
  (RVM 094-2020-MINEDU y sus modificatorias), por eso el sistema deja la decisión al colegio.

**En el sistema:**
- Notas en AD/A/B/C. Si se dicta un número (0–20) se convierte con la tabla habitual
  18–20 AD, 14–17 A, 11–13 B, 0–10 C (ajustable en `notas-voz.ts`).
- **Promoción y recuperación** (`/promocion`): por sección se marca Promovido / Recuperación /
  Permanece (Egresa en el último grado), las áreas por subsanar y la sección de destino.
  Se crea la matrícula de continuidad del año siguiente.
- **Recuperación pedagógica:** lista de áreas pendientes; Coordinación o Secretaría registra
  el resultado (subsanada con AD/A/B o no subsanada).

## Nuevas funciones de esta versión

| Función | Para quién | Dónde |
|---|---|---|
| Ingreso al colegio escaneando el DNI (lector USB o cámara), tardanza automática y aviso al apoderado | Auxiliar, Secretaría, Convivencia | Ingreso al colegio |
| Dictado de notas por voz ("Juan Quispe, A" / "número 5, B" / "Rosa Ccahuana, 15") | Docente | Notas |
| Repartir un curso entre varios docentes equilibrando sus horas | Coordinación académica | Carga y horarios → Carga académica |
| Matrícula con tipo (nuevo, continúa, traslado) y gratuidad visible | Secretaría | Matricular |
| Traslado a otra IE conservando historial | Secretaría, Dirección | Estudiantes |
| Pasar de grado con áreas por subsanar | Secretaría, Dirección | Promoción y recuperación |

Configuración del ingreso (variables de entorno de `servicio-estudiantes`):
`INGRESO_LIMITE_MANANA` (07:45), `INGRESO_LIMITE_TARDE` (13:45), `INGRESO_CAMBIO_TURNO` (12:30),
`INGRESO_NOTIFICAR` = TODOS | TARDANZAS | NINGUNO.

## Ideas para seguir innovando

1. **Alerta temprana de deserción** combinando faltas, tardanzas en portería y notas C.
2. **Libreta de notas digital** para familias con firma de recepción en el portal.
3. **Exportación al formato de carga del SIAGIE** (nómina y notas) para no digitar dos veces.
4. **Modo sin conexión** en la portería y en asistencia, que sincroniza al volver la red.
5. **Carnet escolar con QR** generado por el sistema para quienes no tienen DNI a la mano.
6. **Panel de tutoría**: vista única del estudiante (asistencia, conducta, salud, recuperación).
7. **Reserva de aulas y laboratorios** usando el mismo motor de horarios.
