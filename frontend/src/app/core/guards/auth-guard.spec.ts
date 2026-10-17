import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const loginTree = { path: 'login' } as unknown as UrlTree;
  const claveTree = { path: 'clave' } as unknown as UrlTree;
  const auth = { isLoggedIn: vi.fn(), mustChangePassword: vi.fn() };
  const router = {
    parseUrl: vi.fn((url: string) => url === '/login' ? loginTree : claveTree)
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router }
      ]
    });
  });

  function ejecutar(url: string) {
    return TestBed.runInInjectionContext(() => authGuard(
      {} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));
  }

  it('envía al login cuando no existe sesión', () => {
    auth.isLoggedIn.mockReturnValue(false);
    expect(ejecutar('/dashboard')).toBe(loginTree);
  });

  it('obliga a cambiar clave sin generar un ciclo en esa misma ruta', () => {
    auth.isLoggedIn.mockReturnValue(true);
    auth.mustChangePassword.mockReturnValue(true);

    expect(ejecutar('/dashboard')).toBe(claveTree);
    expect(ejecutar('/cambiar-clave')).toBe(true);
  });
});
