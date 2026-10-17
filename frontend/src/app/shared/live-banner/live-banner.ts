import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventoCambio, RealtimeService } from '../../core/realtime/realtime.service';

/**
 * Aviso "hay cambios nuevos" para pantallas donde el usuario está editando: en lugar de
 * recargar y borrar lo que escribe, se le ofrece actualizar cuando le convenga.
 */
@Component({
  selector: 'app-live-banner',
  standalone: true,
  template: `
    @if (ultimo(); as evento) {
      <div class="live-banner" role="status">
        <span class="punto" aria-hidden="true"></span>
        <span class="texto"><strong>{{ evento.actor }}</strong> {{ evento.descripcion }}@if (cantidad() > 1) { <span> y {{ cantidad() - 1 }} cambio(s) más</span> }.</span>
        <button type="button" class="btn btn-sm btn-primary" (click)="actualizar()">
          <i class="bi bi-arrow-clockwise me-1"></i>Actualizar
        </button>
        <button type="button" class="cerrar" (click)="descartar()" aria-label="Ocultar aviso"><i class="bi bi-x-lg"></i></button>
      </div>
    }
  `,
  styles: [`
    .live-banner {
      display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap;
      margin-bottom: 1rem; padding: 0.7rem 0.9rem;
      border: 1px solid var(--brand-line); border-radius: var(--radius-md);
      background: var(--brand-soft); color: var(--ink);
      animation: bajar 0.25s ease-out;
    }
    @keyframes bajar { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: none; } }
    .punto { width: 9px; height: 9px; border-radius: 50%; background: var(--brand); animation: latir 1.6s infinite; }
    @keyframes latir { 0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--brand) 60%, transparent); } 80%, 100% { box-shadow: 0 0 0 7px transparent; } }
    .texto { flex: 1; min-width: 200px; font-size: 0.9rem; }
    .cerrar { border: 0; background: transparent; color: var(--ink-3); border-radius: 6px; padding: 2px 6px; }
    .cerrar:hover { background: var(--surface); color: var(--ink); }
  `]
})
export class LiveBanner implements OnInit {
  readonly modulos = input.required<string[]>();
  readonly refrescar = output<void>();

  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  readonly ultimo = signal<EventoCambio | null>(null);
  readonly cantidad = signal(0);

  ngOnInit(): void {
    this.realtime.cambios$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(evento => {
      if (evento.propio || !this.modulos().includes(evento.modulo)) return;
      this.ultimo.set(evento);
      this.cantidad.update(n => n + 1);
    });
  }

  actualizar(): void {
    this.descartar();
    this.refrescar.emit();
  }

  descartar(): void {
    this.ultimo.set(null);
    this.cantidad.set(0);
  }
}
