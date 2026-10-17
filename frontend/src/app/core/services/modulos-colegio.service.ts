import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { ModuloColegio } from '../navigation';
import { RealtimeService } from '../realtime/realtime.service';

export type EstadoModulos = Partial<Record<ModuloColegio, boolean>>;

/**
 * Módulos opcionales del colegio (Psicología, Enfermería, Mantenimiento). Dirección los habilita
 * solo si el colegio tiene ese personal; el menú oculta las pantallas de los módulos apagados.
 */
@Injectable({ providedIn: 'root' })
export class ModulosColegioService {
  private readonly http = inject(HttpClient);
  private readonly realtime = inject(RealtimeService);

  /** undefined mientras no se conoce (el menú muestra todo hasta saberlo). */
  readonly estado = signal<EstadoModulos | undefined>(undefined);
  private escuchando = false;

  cargar(): void {
    this.http.get<EstadoModulos>('/api/institucion/modulos').subscribe({
      next: estado => this.estado.set(estado),
      error: () => { /* sin conexión: se mantiene el último estado conocido */ }
    });
    if (!this.escuchando) {
      this.escuchando = true;
      this.realtime.alCambiar('institucion').subscribe(() => this.cargar());
    }
  }

  cambiar(modulo: ModuloColegio, habilitado: boolean): Observable<EstadoModulos> {
    return this.http.put<EstadoModulos>(`/api/institucion/modulos/${modulo}`, { habilitado })
      .pipe(tap(estado => this.estado.set(estado)));
  }
}
