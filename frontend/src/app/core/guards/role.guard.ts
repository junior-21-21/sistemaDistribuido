import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRoles = route.data?.['roles'] as Array<string>;
  const currentRole = authService.getRole();

  if (authService.isLoggedIn() && currentRole && expectedRoles?.includes(currentRole)) {
    return true;
  }
  
  // Si no tiene el rol correcto, enviarlo a su inicio correspondiente
  return router.parseUrl('/dashboard');
};
