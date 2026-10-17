import { Injectable, signal } from '@angular/core';

/** Estado de la estructura principal: menú compacto, menú móvil y buscador. */
@Injectable({ providedIn: 'root' })
export class LayoutService {
  readonly menuCompacto = signal(this.leer('pad_menu_compacto') === '1');
  readonly menuMovilAbierto = signal(false);
  readonly buscadorAbierto = signal(false);

  alternarMenu(): void {
    if (window.matchMedia('(max-width: 991.98px)').matches) {
      this.menuMovilAbierto.update(v => !v);
      return;
    }
    this.menuCompacto.update(v => !v);
    try {
      localStorage.setItem('pad_menu_compacto', this.menuCompacto() ? '1' : '0');
    } catch {
      /* sin almacenamiento */
    }
  }

  private leer(clave: string): string | null {
    try {
      return localStorage.getItem(clave);
    } catch {
      return null;
    }
  }
}
