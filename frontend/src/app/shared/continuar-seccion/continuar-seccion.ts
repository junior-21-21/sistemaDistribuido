import { Component, Injectable, inject, output, signal } from '@angular/core';

export interface SeccionRecordada {
  id: number;
  nombre: string;
  gradoId: number;
  gradoNombre?: string;
  nivelNombre?: string;
  turno?: string;
  [extra: string]: unknown;
}

/**
 * Recuerda la última sección abierta (en notas, asistencia, convivencia o estudiantes) para
 * volver a ella con un clic en lugar de elegir otra vez grado y sección.
 */
@Injectable({ providedIn: 'root' })
export class UltimaSeccionService {
  private readonly clave = 'pad_ultima_seccion';
  readonly seccion = signal<SeccionRecordada | null>(this.leer());

  guardar(seccion: SeccionRecordada | null | undefined): void {
    if (!seccion?.id) return;
    const copia: SeccionRecordada = {
      id: seccion.id, nombre: seccion.nombre, gradoId: seccion.gradoId,
      gradoNombre: seccion.gradoNombre, nivelNombre: seccion.nivelNombre, turno: seccion.turno
    };
    this.seccion.set(copia);
    try {
      localStorage.setItem(this.clave, JSON.stringify(copia));
    } catch {
      /* sin almacenamiento */
    }
  }

  private leer(): SeccionRecordada | null {
    try {
      const valor = localStorage.getItem(this.clave);
      return valor ? JSON.parse(valor) : null;
    } catch {
      return null;
    }
  }
}

@Component({
  selector: 'app-continuar-seccion',
  standalone: true,
  template: `
    @if (servicio.seccion(); as s) {
      <button type="button" class="continuar" (click)="continuar.emit(s)">
        <span class="icono"><i class="bi bi-bookmark-star-fill"></i></span>
        <span class="texto">
          <small>Continuar donde te quedaste</small>
          <strong>{{ s.gradoNombre }} {{ s.nombre }}</strong>
        </span>
        <i class="bi bi-arrow-right-circle-fill flecha"></i>
      </button>
    }
  `,
  styles: [`
    .continuar {
      display: inline-flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem;
      padding: 0.65rem 0.9rem 0.65rem 0.7rem; border: 0; border-radius: 4px 4px 14px 4px;
      background: var(--nota-amarilla); color: var(--ink); text-align: left;
      box-shadow: 0 10px 18px -12px rgba(60, 45, 20, 0.45); transform: rotate(-0.6deg);
      transition: transform var(--transition-fast);
    }
    .continuar:hover { transform: rotate(0) translateY(-2px); }
    .icono { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 10px; background: rgba(255, 255, 255, 0.6); color: var(--warn); font-size: 1.1rem; }
    .texto { display: flex; flex-direction: column; line-height: 1.2; }
    .texto small { font-family: var(--font-mano); font-size: 1rem; color: var(--ink-2); }
    .texto strong { font-family: var(--font-titulo); font-size: 1.1rem; }
    .flecha { font-size: 1.5rem; color: var(--brand); margin-left: 0.5rem; }
  `]
})
export class ContinuarSeccion {
  readonly servicio = inject(UltimaSeccionService);
  readonly continuar = output<SeccionRecordada>();
}

