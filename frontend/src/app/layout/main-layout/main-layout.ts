import { Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../sidebar/sidebar';
import { Navbar } from '../navbar/navbar';
import { CommandPalette } from '../../shared/command-palette/command-palette';
import { ToastStack } from '../../shared/toast-stack/toast-stack';
import { LayoutService } from '../../core/services/layout.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ToastService } from '../../core/services/toast.service';
import { NAV_ITEMS } from '../../core/navigation';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Navbar, CommandPalette, ToastStack],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css'
})
export class MainLayout implements OnInit, OnDestroy {
  readonly layout = inject(LayoutService);
  private readonly realtime = inject(RealtimeService);
  private readonly toasts = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.realtime.conectar();
    // Aviso discreto cuando un colega registra algo que te afecta.
    this.realtime.cambios$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(evento => {
      // Los ingresos por portería son cientos por mañana: se reflejan en su pantalla, sin avisos.
      if (evento.propio || evento.modulo === 'ingresos') return;
      const icono = NAV_ITEMS.find(item => item.modulos?.includes(evento.modulo))?.icon ?? 'bi-lightning-charge-fill';
      this.toasts.mostrar({ tipo: 'vivo', icono, titulo: evento.actor, detalle: `${evento.descripcion} · ahora mismo` });
    });
  }

  ngOnDestroy(): void {
    this.realtime.desconectar();
  }
}
