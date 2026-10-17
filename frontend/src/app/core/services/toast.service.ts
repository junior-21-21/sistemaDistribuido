import { Injectable, signal } from '@angular/core';

export type TipoToast = 'info' | 'exito' | 'error' | 'vivo';

export interface Toast {
  id: number;
  tipo: TipoToast;
  titulo: string;
  detalle?: string;
  icono?: string;
}

/** Avisos breves en la esquina de la pantalla. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private secuencia = 0;
  private readonly toastsSignal = signal<Toast[]>([]);
  readonly toasts = this.toastsSignal.asReadonly();

  mostrar(toast: Omit<Toast, 'id'>, duracionMs = 4500): void {
    const id = ++this.secuencia;
    this.toastsSignal.update(lista => [...lista.slice(-3), { ...toast, id }]);
    setTimeout(() => this.cerrar(id), duracionMs);
  }

  exito(titulo: string, detalle?: string): void {
    this.mostrar({ tipo: 'exito', titulo, detalle, icono: 'bi-check-circle-fill' });
  }

  error(titulo: string, detalle?: string): void {
    this.mostrar({ tipo: 'error', titulo, detalle, icono: 'bi-exclamation-octagon-fill' }, 6500);
  }

  cerrar(id: number): void {
    this.toastsSignal.update(lista => lista.filter(t => t.id !== id));
  }
}
