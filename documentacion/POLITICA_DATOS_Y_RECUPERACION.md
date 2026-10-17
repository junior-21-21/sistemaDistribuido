# Política operativa de datos y recuperación

Esta política es la base técnica propuesta para el colegio. Antes de incorporar datos
reales debe ser aprobada por Dirección y por el responsable legal o de protección de
datos de la institución; los plazos legales aplicables prevalecen sobre estos valores.

## Consentimiento

- Secretaría registra eventos inmutables `OTORGADO`, `DENEGADO` o `REVOCADO` para cada finalidad.
  Las decisiones iniciales se guardan en la misma transacción de matrícula:
  `DATOS_ACADEMICOS`, `SALUD` y `COMUNICACIONES`.
- Cada evento conserva estudiante, apoderado vinculado, versión de política, evidencia,
  actor y fecha. No se sobreescribe el evento anterior.
- Un cambio de texto de privacidad incrementa `DATA_CONSENT_POLICY_VERSION` y exige una
  nueva aceptación cuando corresponda.
- La atención educativa obligatoria y el consentimiento opcional deben distinguirse en
  el texto institucional; esta decisión no debe inferirse únicamente desde el software.

## Retención propuesta

| Categoría | Plazo operativo inicial | Disposición |
|---|---:|---|
| Actas y resultados académicos oficiales | 10 años desde el egreso | Archivo institucional o anonimización según norma aplicable |
| Asistencia y convivencia | 5 años desde el cierre escolar | Eliminación o anonimización aprobada |
| Salud y psicología | 5 años desde la salida del estudiante | Eliminación segura con acceso restringido |
| Auditoría técnica y accesos | 2 años | Eliminación programada |
| Trazas de notificación | 1 año | Eliminación programada, conservando métricas agregadas |
| Backups cifrados | 30 días | Eliminación automática del volumen de respaldos |

No se automatiza todavía la eliminación de expedientes académicos, clínicos o de
convivencia: requiere resolución institucional, revisión legal y un procedimiento de
excepción por investigaciones o reclamos. Los backups sí aplican retención automática.

## Recuperación

- Se realiza un backup cifrado diario de las seis bases PostgreSQL.
- Cada archivo tiene SHA-256 y se restaura inmediatamente en `pad_restore_verify`.
- La restauración compara la cantidad de tablas del origen y de la copia.
- Cada éxito se agrega a `/backups/restore-verifications.jsonl`; el ciclo completo actualiza
  `/backups/latest-success.txt`.
- Una restauración productiva nunca se ejecuta directamente sobre la base activa. Primero
  se restaura en un entorno aislado, se valida y se aprueba el cambio de servicio.

Prueba completa:

```powershell
$env:BACKUP_ENCRYPTION_KEY='una-frase-segura-del-almacen-de-secretos'
.\ops\tests\backup-restore.ps1
```

Última verificación técnica: 28 de septiembre de 2026, seis bases restauradas correctamente.
La aprobación legal de los plazos y finalidades continúa siendo una condición previa a usar
datos personales reales.
