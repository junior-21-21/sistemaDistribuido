import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface User {
  name: string;
  role: string;
  roles: string[];
  username?: string;
  email?: string;
  mustChangePassword?: boolean;
}

export interface LoginResponse {
  token: string;
  username: string;
  nombre: string;
  rol: string;
  roles?: string[];
  email: string;
  message?: string;
  debeCambiarClave?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private currentUserSignal = signal<User | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();

  private authUrl = '/api/auth';

  constructor() {
    this.checkStoredUser();
  }

  private checkStoredUser() {
    const storedUser = localStorage.getItem('pad_user');
    if (storedUser) {
      try {
        this.currentUserSignal.set(JSON.parse(storedUser));
      } catch {
        this.clearStorage();
      }
    }
  }

  /**
   * Login REAL contra el backend Spring Boot con JWT.
   * Retorna el LoginResponse con el token JWT.
   */
  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, { username, password }).pipe(
      tap((res) => {
        // Guardar token JWT real
        localStorage.setItem('pad_token', res.token);

        const user: User = {
          name: res.nombre,
          role: res.rol,
          roles: res.roles?.length ? res.roles : [res.rol],
          username: res.username,
          email: res.email,
          mustChangePassword: Boolean(res.debeCambiarClave)
        };
        this.setSession(user);
      })
    );
  }



  private setSession(user: User) {
    localStorage.setItem('pad_role', user.role);
    localStorage.setItem('pad_roles', JSON.stringify(user.roles));
    localStorage.setItem('pad_user', JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private clearStorage() {
    localStorage.removeItem('pad_token');
    localStorage.removeItem('pad_role');
    localStorage.removeItem('pad_roles');
    localStorage.removeItem('pad_user');
    // Limpiar claves antiguas por compatibilidad
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('pad_token');
  }

  getToken(): string | null {
    return localStorage.getItem('pad_token');
  }

  getRole(): string | null {
    return localStorage.getItem('pad_role') || localStorage.getItem('role');
  }

  getRoles(): string[] {
    const stored = localStorage.getItem('pad_roles');
    if (stored) {
      try {
        const roles = JSON.parse(stored);
        if (Array.isArray(roles)) return roles.filter(role => typeof role === 'string');
      } catch { /* compatibilidad con sesiones anteriores */ }
    }
    const legacyRole = this.getRole();
    return legacyRole ? [legacyRole] : [];
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    const current = this.getRoles();
    return roles.some(role => current.includes(role));
  }

  mustChangePassword(): boolean {
    return Boolean(this.currentUserSignal()?.mustChangePassword);
  }

  changePassword(claveActual: string, nuevaClave: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.authUrl}/cambiar-clave`, {
      claveActual,
      nuevaClave
    }).pipe(
      tap(() => {
        const user = this.currentUserSignal();
        if (user) this.setSession({ ...user, mustChangePassword: false });
      })
    );
  }

  logout() {
    // Intentar logout en backend (no bloqueante)
    this.http.post(`${this.authUrl}/logout`, {})
      .subscribe({ error: () => {} });

    this.clearStorage();
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }
}
