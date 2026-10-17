# Certificación de correo, SMS y WhatsApp

**Estado al 28 de septiembre de 2026:** contrato, validaciones y ejecutor implementados;
certificación real pendiente porque no se proporcionaron proveedor, credenciales ni
destinatarios institucionales de prueba. El entorno local continúa en `SIMULATED`.

El sistema usa un contrato webhook común con `Idempotency-Key`, token Bearer, timeouts y
reintentos. En producción `NOTIFICATIONS_MODE=WEBHOOK` exige HTTPS, los tres endpoints
declarados y `NOTIFICATIONS_WEBHOOK_TOKEN`; la aplicación no inicia si falta un requisito.

## Contrato de salida

`POST` al webhook configurado, con encabezados:

- `Authorization: Bearer <token>`
- `Idempotency-Key: <evento>-<canal>`
- `Content-Type: application/json`

El cuerpo contiene `eventId`, `channel`, `to`, `subject` y `message`. Cualquier respuesta
no 2xx se trata como fallo y entra al ciclo de reintentos.

## Certificación real

1. Configura URLs HTTPS y token desde un almacén de secretos.
2. Usa destinatarios institucionales de prueba, nunca familias reales.
3. Reconstruye `servicio-notificaciones` y confirma que `/actuator/health` está `UP`.
4. Ejecuta:

```powershell
.\ops\tests\provider-certification.ps1 `
  -Username admin -Password '<secreto>' `
  -EmailDestination qa@institucion.edu.pe `
  -SmsDestination 51999000000 `
  -WhatsAppDestination 51999000000
```

La prueba solo aprueba si cada proveedor devuelve HTTP 2xx en modo `WEBHOOK`. Además del
resultado técnico, la institución debe confirmar recepción, remitente autorizado, tiempos
de entrega, cuotas, opt-out, soporte, protección contractual de datos y procedimiento de
incidentes. Esas comprobaciones dependen del proveedor contratado y no pueden certificarse
sin sus credenciales y destinatarios de prueba.
