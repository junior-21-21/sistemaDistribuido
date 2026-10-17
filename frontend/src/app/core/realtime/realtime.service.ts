import { Injectable, NgZone, computed, inject, signal } from '@angular/core';
import { Observable, Subject, debounceTime, filter } from 'rxjs';
import { AuthService } from '../services/auth.service';

export interface EventoCambio {
  id: string;
  modulo: string;
  accion: 'CREAR' | 'ACTUALIZAR' | 'ELIMINAR';
  descripcion: string;
  actor: string;
  actorUsuario?: string | null;
  fecha: string;
  propio?: boolean;
}

export type EstadoConexion = 'desconectado' | 'conectando' | 'en-vivo' | 'reconectando';

const MAX_EVENTOS = 40;
const MODULOS_SILENCIOSOS = new Set(['ingresos']);

/**
 * Conexión Server-Sent Events con el gateway (/api/tiempo-real/stream).
 * Se usa fetch en lugar de EventSource porque el canal exige el JWT en Authorization.
 * Reintenta con espera exponencial y deja de hacerlo al cerrar sesión.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);

  private readonly estadoSignal = signal<EstadoConexion>('desconectado');
  private readonly eventosSignal = signal<EventoCambio[]>([]);
  private readonly noLeidosSignal = signal(0);
  private readonly cambios = new Subject<EventoCambio>();

  readonly estado = this.estadoSignal.asReadonly();
  readonly eventos = this.eventosSignal.asReadonly();
  readonly noLeidos = this.noLeidosSignal.asReadonly();
  readonly enVivo = computed(() => this.estadoSignal() === 'en-vivo');
  /** Todos los cambios, incluidos los propios. */
  readonly cambios$ = this.cambios.asObservable();

  private controller: AbortController | null = null;
  private reintentos = 0;
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private activo = false;

  conectar(): void {
    if (this.activo) return;
    this.activo = true;
    // La conexión vive fuera de Angular para no disparar detección de cambios con cada latido;
    // solo se vuelve a entrar al recibir un evento real.
    this.zone.runOutsideAngular(() => this.abrir());
  }

  desconectar(): void {
    this.activo = false;
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = null;
    this.controller?.abort();
    this.controller = null;
    this.estadoSignal.set('desconectado');
    this.eventosSignal.set([]);
    this.noLeidosSignal.set(0);
  }

  marcarLeidos(): void {
    this.noLeidosSignal.set(0);
  }

  /**
   * Emite (agrupando ráfagas) cuando cambia alguno de los módulos indicados.
   * Úsalo para recargar la vista: `realtime.alCambiar('notas').subscribe(() => this.cargar())`.
   */
  alCambiar(...modulos: string[]): Observable<EventoCambio> {
    const conjunto = new Set(modulos);
    return this.cambios.pipe(
      filter(evento => conjunto.size === 0 || conjunto.has(evento.modulo)),
      debounceTime(600)
    );
  }

  private async abrir(): Promise<void> {
    const token = this.auth.getToken();
    if (!this.activo || !token) {
      this.estadoSignal.set('desconectado');
      return;
    }
    this.estadoSignal.set(this.reintentos === 0 ? 'conectando' : 'reconectando');
    this.controller = new AbortController();

    try {
      const respuesta = await fetch('/api/tiempo-real/stream', {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: this.controller.signal,
        cache: 'no-store'
      });
      if (respuesta.status === 401) {
        // Sesión vencida: el interceptor HTTP se encargará al próximo pedido.
        this.estadoSignal.set('desconectado');
        this.activo = false;
        return;
      }
      if (!respuesta.ok || !respuesta.body) throw new Error(`HTTP ${respuesta.status}`);
      await this.leer(respuesta.body);
      throw new Error('Conexión cerrada');
    } catch (error) {
      if (!this.activo || (error instanceof DOMException && error.name === 'AbortError')) return;
      this.programarReintento();
    }
  }

  private async leer(cuerpo: ReadableStream<Uint8Array>): Promise<void> {
    const lector = cuerpo.getReader();
    const decodificador = new TextDecoder();
    let buffer = '';
    for (;;) {
      const { done, value } = await lector.read();
      if (done) return;
      buffer += decodificador.decode(value, { stream: true }).replace(/\r\n/g, '\n');
      let corte: number;
      while ((corte = buffer.indexOf('\n\n')) >= 0) {
        const bloque = buffer.slice(0, corte);
        buffer = buffer.slice(corte + 2);
        this.procesarBloque(bloque);
      }
    }
  }

  private procesarBloque(bloque: string): void {
    let tipo = 'message';
    const datos: string[] = [];
    for (const linea of bloque.split('\n')) {
      if (linea.startsWith(':')) continue; // latido
      if (linea.startsWith('event:')) tipo = linea.slice(6).trim();
      else if (linea.startsWith('data:')) datos.push(linea.slice(5).replace(/^ /, ''));
    }
    if (datos.length === 0 && tipo === 'message') return; // solo latido
    this.zone.run(() => this.aplicarBloque(tipo, datos));
  }

  private aplicarBloque(tipo: string, datos: string[]): void {
    if (tipo === 'conectado') {
      this.reintentos = 0;
      this.estadoSignal.set('en-vivo');
      return;
    }
    if (tipo !== 'cambio' || datos.length === 0) return;
    try {
      const evento = JSON.parse(datos.join('\n')) as EventoCambio;
      evento.propio = !!evento.actorUsuario && evento.actorUsuario === this.auth.currentUser()?.username;
      // Los ingresos por portería son muy frecuentes: actualizan pantallas pero no la campana.
      if (!MODULOS_SILENCIOSOS.has(evento.modulo)) {
        this.eventosSignal.update(lista => [evento, ...lista].slice(0, MAX_EVENTOS));
        if (!evento.propio) this.noLeidosSignal.update(n => n + 1);
      }
      this.cambios.next(evento);
    } catch {
      /* evento malformado: se ignora */
    }
  }

  private programarReintento(): void {
    this.reintentos++;
    this.estadoSignal.set('reconectando');
    const espera = Math.min(30000, 1000 * 2 ** Math.min(this.reintentos - 1, 5));
    this.temporizador = setTimeout(() => this.abrir(), espera);
  }
}
