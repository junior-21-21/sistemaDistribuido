import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { catchError, retry } from 'rxjs/operators';
import { throwError, timer } from 'rxjs';

/** Estados que indican un problema pasajero de red o de un servicio reiniciándose. */
const TRANSITORIOS = new Set([0, 502, 503, 504]);
let ultimoAvisoConexion = 0;

/**
 * JWT Interceptor — Agrega el token Bearer a cada petición HTTP que vaya al backend (/api/*).
 * Reintenta automáticamente las consultas (GET) ante fallos pasajeros, para que la pantalla
 * cargue al primer clic aunque un servicio esté reiniciándose, y cierra sesión ante un 401.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('pad_token');
  const authService = inject(AuthService);
  const toasts = inject(ToastService);
  let clonedReq = req;

  // Solo agregar header si hay token y es un JWT (empieza con eyJ)
  if (token && token.startsWith('eyJ') && req.url.includes('/api/')) {
    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  const esConsulta = req.method === 'GET' || req.method === 'HEAD';

  return next(clonedReq).pipe(
    retry({
      count: esConsulta ? 2 : 0,
      delay: (error: HttpErrorResponse, intento) =>
        TRANSITORIOS.has(error.status) ? timer(500 * intento) : throwError(() => error)
    }),
    catchError((error: HttpErrorResponse) => {
      // Evitar loop infinito si el error 401/403 viene de endpoints de autenticación
      if (error.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/logout')) {
        // Un 401 indica una sesión inválida o vencida. Un 403 solo indica falta de permisos.
        authService.logout();
      } else if (TRANSITORIOS.has(error.status) && Date.now() - ultimoAvisoConexion > 8000) {
        ultimoAvisoConexion = Date.now();
        toasts.error('Problema de conexión',
          error.error?.message || 'Un servicio no respondió. Revisa tu conexión e inténtalo de nuevo.');
      }
      return throwError(() => error);
    })
  );
};
