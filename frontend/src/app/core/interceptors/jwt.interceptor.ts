import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

/**
 * JWT Interceptor — Agrega el token Bearer a cada petición HTTP
 * que vaya al backend (/api/*).
 * 
 * Lee el token de localStorage ('pad_token').
 * Si no hay token o no es un JWT válido, la petición pasa sin header.
 * También intercepta errores 401/403 para cerrar sesión automáticamente.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('pad_token');
  const authService = inject(AuthService);
  let clonedReq = req;

  // Solo agregar header si hay token y es un JWT (empieza con eyJ)
  if (token && token.startsWith('eyJ') && req.url.includes('/api/')) {
    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Evitar loop infinito si el error 401/403 viene de endpoints de autenticación
      if ((error.status === 401 || error.status === 403) && !req.url.includes('/auth/login') && !req.url.includes('/auth/logout')) {
        // Token expirado o inválido, o no tiene permisos.
        // Forzamos logout para limpiar el estado y redirigir.
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
