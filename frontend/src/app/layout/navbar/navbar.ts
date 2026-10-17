import { Component, DestroyRef, NgZone, HostListener, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { LayoutService } from '../../core/services/layout.service';
import { ThemeService } from '../../core/services/theme.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { NAV_ITEMS, ROL_ETIQUETA, iniciales, itemPorRuta } from '../../core/navigation';
import { HaceTiempoPipe } from '../../shared/hace-tiempo.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, HaceTiempoPipe],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar implements OnInit, OnDestroy {
  readonly authService = inject(AuthService);
  readonly layout = inject(LayoutService);
  readonly theme = inject(ThemeService);
  readonly realtime = inject(RealtimeService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly zona = inject(NgZone);

  readonly url = signal(this.router.url);
  readonly seccion = computed(() => itemPorRuta(this.url()));
  readonly panelAbierto = signal(false);
  readonly menuUsuarioAbierto = signal(false);
  readonly usuario = this.authService.currentUser;
  readonly iniciales = computed(() => iniciales(this.usuario()?.name));
  readonly roles = computed(() => (this.usuario()?.roles ?? []).map(rol => ROL_ETIQUETA[rol] ?? rol));
  /** Reloj para refrescar los "hace X min" del panel. */
  readonly ahora = signal(Date.now());
  private reloj: ReturnType<typeof setInterval> | null = null;
  readonly esMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);

  readonly estadoTexto = computed(() => ({
    'en-vivo': 'En vivo',
    'conectando': 'Conectando…',
    'reconectando': 'Reconectando…',
    'desconectado': 'Sin conexión'
  })[this.realtime.estado()]);

  ngOnInit(): void {
    // Reloj fuera de Angular: la señal ya avisa a la vista cuando cambia.
    this.zona.runOutsideAngular(() => {
      this.reloj = setInterval(() => this.ahora.set(Date.now()), 30000);
    });
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(event => {
      this.url.set(event.urlAfterRedirects);
      this.panelAbierto.set(false);
      this.menuUsuarioAbierto.set(false);
    });
  }

  ngOnDestroy(): void {
    if (this.reloj) clearInterval(this.reloj);
  }

  alternarPanel(): void {
    this.menuUsuarioAbierto.set(false);
    this.panelAbierto.update(v => !v);
    if (this.panelAbierto()) this.realtime.marcarLeidos();
  }

  alternarMenuUsuario(): void {
    this.panelAbierto.set(false);
    this.menuUsuarioAbierto.update(v => !v);
  }

  abrirEvento(modulo: string): void {
    const destino = NAV_ITEMS.find(item => item.modulos?.includes(modulo)
      && item.roles.some(rol => this.authService.hasRole(rol)));
    this.panelAbierto.set(false);
    if (destino) this.router.navigateByUrl(destino.path);
  }

  iconoModulo(modulo: string): string {
    return NAV_ITEMS.find(item => item.modulos?.includes(modulo))?.icon ?? 'bi-lightning-charge';
  }

  @HostListener('document:keydown.escape')
  cerrarTodo(): void {
    this.panelAbierto.set(false);
    this.menuUsuarioAbierto.set(false);
  }

  @HostListener('document:click', ['$event'])
  clicFuera(evento: MouseEvent): void {
    const objetivo = evento.target as HTMLElement | null;
    if (!objetivo?.closest('.desplegable')) this.cerrarTodo();
  }
}
