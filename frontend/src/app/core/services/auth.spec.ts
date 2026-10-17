import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('persiste token, roles múltiples y cambio obligatorio de clave al iniciar sesión', () => {
    service.login('coord.academica', 'ClaveFuerte123*').subscribe();

    const request = http.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      username: 'coord.academica', password: 'ClaveFuerte123*'
    });
    request.flush({
      token: 'jwt-prueba', username: 'coord.academica', nombre: 'Coordinadora Académica',
      rol: 'DIRECTOR', roles: ['DIRECTOR', 'DOCENTE'],
      email: 'coord@example.test', debeCambiarClave: true
    });

    expect(service.isLoggedIn()).toBe(true);
    expect(service.getToken()).toBe('jwt-prueba');
    expect(service.getRoles()).toEqual(['DIRECTOR', 'DOCENTE']);
    expect(service.hasAnyRole('DIRECTOR', 'DOCENTE')).toBe(true);
    expect(service.mustChangePassword()).toBe(true);
  });

  it('mantiene compatibilidad cuando el backend solo devuelve el rol principal', () => {
    service.login('director', 'ClaveFuerte123*').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jwt-director', username: 'director', nombre: 'Director', rol: 'DIRECTOR',
      email: 'director@example.test'
    });

    expect(service.getRoles()).toEqual(['DIRECTOR']);
    expect(service.hasRole('DIRECTOR')).toBe(true);
  });

  it('actualiza la sesión después de cambiar la clave', () => {
    service.login('admin', 'Temporal123*').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jwt-admin', username: 'admin', nombre: 'Administrador', rol: 'DIRECTOR',
      roles: ['DIRECTOR'], email: 'admin@example.test', debeCambiarClave: true
    });

    service.changePassword('Temporal123*', 'NuevaClave123*').subscribe();
    const request = http.expectOne('/api/auth/cambiar-clave');
    expect(request.request.body).toEqual({
      claveActual: 'Temporal123*', nuevaClave: 'NuevaClave123*'
    });
    request.flush({ message: 'Clave actualizada' });

    expect(service.mustChangePassword()).toBe(false);
  });
});
