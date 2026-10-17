import { Injectable, signal } from '@angular/core';

export type Tema = 'light' | 'dark';

/** Tema claro/oscuro. index.html aplica el inicial antes de pintar para evitar parpadeos. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly temaSignal = signal<Tema>(this.leerInicial());
  readonly tema = this.temaSignal.asReadonly();

  alternar(): void {
    this.aplicar(this.temaSignal() === 'dark' ? 'light' : 'dark');
  }

  aplicar(tema: Tema): void {
    this.temaSignal.set(tema);
    document.documentElement.setAttribute('data-theme', tema);
    try {
      localStorage.setItem('pad_tema', tema);
    } catch {
      /* almacenamiento bloqueado: el tema vale solo para esta visita */
    }
  }

  private leerInicial(): Tema {
    const actual = typeof document !== 'undefined' ? document.documentElement.getAttribute('data-theme') : null;
    return actual === 'dark' ? 'dark' : 'light';
  }
}
