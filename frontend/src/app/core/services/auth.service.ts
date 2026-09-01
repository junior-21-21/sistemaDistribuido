import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface User {
  name: string;
  role: string;
  username?: string;
  email?: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  nombre: string;
  rol: string;
  email: string;
  message?: string;
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
          username: res.username,
          email: res.email
        };
        this.setSession(user);
      })
    );
  }



  private setSession(user: User) {
    localStorage.setItem('pad_role', user.role);
    localStorage.setItem('pad_user', JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private clearStorage() {
    localStorage.removeItem('pad_token');
    localStorage.removeItem('pad_role');
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

  hasRole(role: string): boolean {
    return this.getRole() === role;
  }

  hasAnyRole(...roles: string[]): boolean {
    const current = this.getRole();
    return roles.includes(current || '');
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
