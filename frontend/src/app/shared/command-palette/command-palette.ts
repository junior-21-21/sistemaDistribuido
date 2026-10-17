import { Component, ElementRef, HostListener, computed, effect, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { LayoutService } from '../../core/services/layout.service';
import { ThemeService } from '../../core/services/theme.service';
import { NavItem, itemsParaRoles } from '../../core/navigation';
import { ModulosColegioService } from '../../core/services/modulos-colegio.service';

interface Comando {
  id: string;
  titulo: string;
  detalle: string;
  icono: string;
  grupo: string;
  ejecutar: () => void;
  texto: string;
}

/** Buscador universal: Ctrl/⌘ + K o "/" abre una lista para ir a cualquier pantalla. */
@Component({
  selector: 'app-command-palette',
  standalone: true,
  templateUrl: './command-palette.html',
  styleUrl: './command-palette.css'
})
export class CommandPalette {
  private readonly modulos = inject(ModulosColegioService);
  readonly layout = inject(LayoutService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);

  private readonly campo = viewChild<ElementRef<HTMLInputElement>>('campo');
  readonly consulta = signal('');
  readonly seleccion = signal(0);

  private readonly comandos = computed<Comando[]>(() => {
    // Se recalcula al abrir, por si cambió la sesión.
    this.layout.buscadorAbierto();
    const pantallas = itemsParaRoles(this.auth.getRoles(), this.modulos.estado()).map((item: NavItem) => ({
      id: item.path,
      titulo: item.label,
      detalle: item.descripcion,
      icono: item.icon,
      grupo: 'Ir a',
      ejecutar: () => this.router.navigateByUrl(item.path),
      texto: this.normalizar(`${item.label} ${item.descripcion} ${item.claves ?? ''} ${item.grupo}`)
    }));
    const acciones: Comando[] = [
      { id: 'tema', titulo: 'Cambiar tema claro / oscuro', detalle: 'Ajusta los colores para tu ambiente', icono: 'bi-circle-half',
        grupo: 'Acciones', ejecutar: () => this.theme.alternar(), texto: 'tema oscuro claro modo noche colores' },
      { id: 'clave', titulo: 'Cambiar mi contraseña', detalle: 'Actualiza tu clave de acceso', icono: 'bi-key',
        grupo: 'Acciones', ejecutar: () => this.router.navigateByUrl('/cambiar-clave'), texto: 'contraseña clave password seguridad' },
      { id: 'salir', titulo: 'Cerrar sesión', detalle: 'Salir de la plataforma', icono: 'bi-box-arrow-right',
        grupo: 'Acciones', ejecutar: () => this.auth.logout(), texto: 'salir cerrar sesion logout' },
    ];
    return [...pantallas, ...acciones];
  });

  readonly resultados = computed(() => {
    const terminos = this.normalizar(this.consulta()).split(/\s+/).filter(Boolean);
    if (terminos.length === 0) return this.comandos();
    return this.comandos().filter(comando => terminos.every(t => comando.texto.includes(t)));
  });

  constructor() {
    effect(() => {
      if (this.layout.buscadorAbierto()) {
        this.consulta.set('');
        this.seleccion.set(0);
        queueMicrotask(() => this.campo()?.nativeElement.focus());
      }
    });
  }

  @HostListener('document:keydown', ['$event'])
  atajos(evento: KeyboardEvent): void {
    const objetivo = evento.target as HTMLElement | null;
    const escribiendo = !!objetivo && (objetivo.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(objetivo.tagName));
    if ((evento.ctrlKey || evento.metaKey) && evento.key.toLowerCase() === 'k') {
      evento.preventDefault();
      this.layout.buscadorAbierto.update(v => !v);
      return;
    }
    if (evento.key === '/' && !escribiendo && !this.layout.buscadorAbierto()) {
      evento.preventDefault();
      this.layout.buscadorAbierto.set(true);
      return;
    }
    if (!this.layout.buscadorAbierto()) return;
    const total = this.resultados().length;
    if (evento.key === 'Escape') {
      this.cerrar();
    } else if (evento.key === 'ArrowDown') {
      evento.preventDefault();
      this.seleccion.update(i => (total ? (i + 1) % total : 0));
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault();
      this.seleccion.update(i => (total ? (i - 1 + total) % total : 0));
    } else if (evento.key === 'Enter') {
      evento.preventDefault();
      const elegido = this.resultados()[this.seleccion()];
      if (elegido) this.ejecutar(elegido);
    }
  }

  escribir(valor: string): void {
    this.consulta.set(valor);
    this.seleccion.set(0);
  }

  ejecutar(comando: Comando): void {
    this.cerrar();
    comando.ejecutar();
  }

  cerrar(): void {
    this.layout.buscadorAbierto.set(false);
  }

  private normalizar(texto: string): string {
    return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }
}
