import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  const fallback = {} as UrlTree;
  const auth = {
    isLoggedIn: vi.fn(),
    getRoles: vi.fn()
  };
  const router = { parseUrl: vi.fn(() => fallback) };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('acepta cuando cualquiera de los roles de la cuenta coincide', () => {
    auth.isLoggedIn.mockReturnValue(true);
    auth.getRoles.mockReturnValue(['DIRECTOR', 'DOCENTE']);
    const route = { data: { roles: ['DOCENTE'] } } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, { url: '/evaluaciones' } as RouterStateSnapshot));

    expect(result).toBe(true);
  });

  it('redirige si ningún rol coincide', () => {
    auth.isLoggedIn.mockReturnValue(true);
    auth.getRoles.mockReturnValue(['APODERADO']);
    const route = { data: { roles: ['DIRECTOR'] } } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, { url: '/personal' } as RouterStateSnapshot));

    expect(result).toBe(fallback);
    expect(router.parseUrl).toHaveBeenCalledWith('/dashboard');
  });
});
