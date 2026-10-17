import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  credentials = { username: this.usuarioRecordado(), password: '' };
  isLoading = false;
  isPasswordVisible = false;
  errorMessage = '';
  mayusculas = false;
  recordarUsuario = !!this.credentials.username;
  readonly anio = new Date().getFullYear();
  readonly saludo = this.calcularSaludo();
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  login(): void {
    if (this.isLoading || !this.credentials.username || !this.credentials.password) return;
    this.isLoading = true;
    this.errorMessage = '';
    const username = this.credentials.username.trim();
    this.guardarUsuario(username);
    this.authService.login(username, this.credentials.password).subscribe({
      next: response => {
        if (!response) {
          this.errorMessage = 'Usuario o contraseña incorrectos.';
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }
        this.router.navigate([response.debeCambiarClave ? '/cambiar-clave' : '/dashboard']);
      },
      error: error => {
        this.errorMessage = error.status === 401
          ? 'Usuario o contraseña incorrectos.'
          : error.status === 403
            ? 'Tu cuenta no tiene acceso habilitado.'
            : error.status === 429
              ? 'Demasiados intentos de ingreso. Espera un minuto e inténtalo de nuevo.'
              : 'No pudimos conectar con el servidor. Intenta nuevamente.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  detectarMayusculas(evento: KeyboardEvent): void {
    this.mayusculas = typeof evento.getModifierState === 'function' && evento.getModifierState('CapsLock');
  }

  private calcularSaludo(): string {
    const hora = new Date().getHours();
    if (hora < 12) return '¡Buenos días!';
    if (hora < 19) return '¡Buenas tardes!';
    return '¡Buenas noches!';
  }

  private usuarioRecordado(): string {
    try {
      return localStorage.getItem('pad_usuario_recordado') ?? '';
    } catch {
      return '';
    }
  }

  private guardarUsuario(username: string): void {
    try {
      if (this.recordarUsuario) localStorage.setItem('pad_usuario_recordado', username);
      else localStorage.removeItem('pad_usuario_recordado');
    } catch {
      /* sin almacenamiento */
    }
  }
}
