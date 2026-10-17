import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { LayoutService } from '../../core/services/layout.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ModulosColegioService } from '../../core/services/modulos-colegio.service';
import { GRUPOS, NavItem, ROL_ETIQUETA, iniciales, itemsParaRoles } from '../../core/navigation';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit {
  readonly authService = inject(AuthService);
  readonly layout = inject(LayoutService);
  private readonly realtime = inject(RealtimeService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly modulos = inject(ModulosColegioService);

  /** Pantallas según los roles de la persona y los módulos que el colegio tiene habilitados. */
  private readonly items = computed<NavItem[]>(() =>
    itemsParaRoles(this.authService.getRoles(), this.modulos.estado()));
  /** Cambios en vivo no vistos por pantalla (punto con número en el menú). */
  readonly pendientes = signal<Record<string, number>>({});

  readonly grupos = computed(() =>
    GRUPOS.map(grupo => ({ nombre: grupo, items: this.items().filter(item => item.grupo === grupo) }))
      .filter(grupo => grupo.items.length > 0));

  readonly usuario = this.authService.currentUser;
  readonly iniciales = computed(() => iniciales(this.usuario()?.name));
  readonly rolPrincipal = computed(() => {
    const roles = this.usuario()?.roles ?? [];
    return roles.map(rol => ROL_ETIQUETA[rol] ?? rol).join(' · ');
  });

  ngOnInit(): void {
    this.modulos.cargar();

    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(event => {
      this.limpiar(event.urlAfterRedirects);
      this.layout.menuMovilAbierto.set(false);
    });

    this.realtime.cambios$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(evento => {
      if (evento.propio || evento.modulo === 'ingresos') return;
      const actual = this.router.url;
      const afectados = this.items().filter(item => item.modulos?.includes(evento.modulo) && !actual.startsWith(item.path));
      if (afectados.length === 0) return;
      this.pendientes.update(mapa => {
        const copia = { ...mapa };
        for (const item of afectados) copia[item.path] = (copia[item.path] ?? 0) + 1;
        return copia;
      });
    });
  }

  /** Color del separador de cuaderno de cada grupo del menú. */
  claseGrupo(nombre: string): string {
    return ({
      'Académico': 'academico',
      'Convivencia y bienestar': 'convivencia',
      'Secretaría': 'secretaria',
      'Gestión': 'gestion'
    } as Record<string, string>)[nombre] ?? 'inicio';
  }

  cerrarMovil(): void {
    this.layout.menuMovilAbierto.set(false);
  }

  private limpiar(url: string): void {
    const clave = Object.keys(this.pendientes()).find(path => url.startsWith(path));
    if (!clave) return;
    this.pendientes.update(mapa => {
      const copia = { ...mapa };
      delete copia[clave];
      return copia;
    });
  }
}
