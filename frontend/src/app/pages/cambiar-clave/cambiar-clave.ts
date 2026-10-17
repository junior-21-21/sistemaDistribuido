import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cambiar-clave',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cambiar-clave.html',
  styleUrl: './cambiar-clave.css'
})
export class CambiarClave {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  claveActual = '';
  nuevaClave = '';
  confirmarClave = '';
  submitting = false;
  error = '';
  success = '';

  // Password visibility states
  mostrarClaveActual = false;
  mostrarNuevaClave = false;
  mostrarConfirmarClave = false;

  cambiar(): void {
    if (!this.formularioValido() || this.submitting) return;
    this.submitting = true;
    this.error = '';
    this.success = '';
    
    this.authService.changePassword(this.claveActual, this.nuevaClave).subscribe({
      next: () => {
        this.success = 'Tu contraseña se guardó correctamente. Entrando al sistema...';
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 2000);
      },
      error: error => {
        this.submitting = false;
        this.error = error.error?.message || this.primerError(error.error) || 'No se pudo actualizar la clave. Verifica que la clave actual sea correcta y que la nueva cumpla los requisitos.';
      }
    });
  }

  formularioValido(): boolean {
    return Boolean(
      this.claveActual
      && this.nuevaClave.length >= 4
      && this.nuevaClave === this.confirmarClave
    );
  }

  private primerError(value: unknown): string {
    if (!value || typeof value !== 'object') return '';
    const mensajes = Object.values(value as Record<string, unknown>);
    return typeof mensajes[0] === 'string' ? mensajes[0] : '';
  }
}
