import { Component, DestroyRef, ElementRef, HostListener, OnDestroy, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { SeccionService } from '../../core/services/seccion.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { Ingreso, IngresoService } from '../../services/ingreso.service';
import { Seccion } from '../../core/models/seccion.model';

type Resultado =
  | { tipo: 'ok'; ingreso: Ingreso }
  | { tipo: 'error'; mensaje: string };

/**
 * Portería: cada estudiante muestra su DNI y se escanea el código de barras.
 * Funciona con lectores USB (escriben el número y "Enter", como un teclado) y con la cámara.
 */
@Component({
  selector: 'app-ingreso',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './ingreso.html',
  styleUrl: './ingreso.css'
})
export class IngresoPage implements OnInit, OnDestroy {
  private readonly ingresos = inject(IngresoService);
  private readonly seccionService = inject(SeccionService);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly campo = viewChild<ElementRef<HTMLInputElement>>('campo');
  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');

  readonly puedeRegistrar = this.auth.hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'PORTERO');
  codigo = '';
  readonly enviando = signal(false);
  readonly resultado = signal<Resultado | null>(null);
  readonly recientes = signal<Ingreso[]>([]);
  readonly resumen = signal({ activos: 0, ingresaron: 0, puntuales: 0, tardanzas: 0 });
  readonly secciones = signal<Map<number, string>>(new Map());
  readonly camaraActiva = signal(false);
  readonly errorCamara = signal('');
  readonly reloj = signal(new Date());

  readonly porcentaje = computed(() => {
    const r = this.resumen();
    return r.activos ? Math.round((r.ingresaron / r.activos) * 100) : 0;
  });
  readonly faltan = computed(() => Math.max(0, this.resumen().activos - this.resumen().ingresaron));

  private controlesCamara: { stop: () => void } | null = null;
  private ultimoCodigo = '';
  private ultimoMomento = 0;
  private temporizadorReloj: ReturnType<typeof setInterval> | null = null;
  private temporizadorResultado: ReturnType<typeof setTimeout> | null = null;
  private audio: AudioContext | null = null;

  ngOnInit(): void {
    this.temporizadorReloj = setInterval(() => this.reloj.set(new Date()), 1000);
    this.seccionService.getSecciones().subscribe({
      next: lista => this.secciones.set(new Map(lista.filter(s => s.id != null)
        .map((s: Seccion) => [s.id!, `${s.gradoNombre ?? ''} ${s.nombre}`.trim()]))),
      error: () => this.secciones.set(new Map())
    });
    this.cargarResumen();
    // Otro lector (otra puerta) registró a alguien: se actualizan los contadores.
    this.realtime.alCambiar('ingresos').pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargarResumen());
    queueMicrotask(() => this.enfocar());
  }

  ngOnDestroy(): void {
    this.detenerCamara();
    if (this.temporizadorReloj) clearInterval(this.temporizadorReloj);
    if (this.temporizadorResultado) clearTimeout(this.temporizadorResultado);
    this.audio?.close().catch(() => undefined);
  }

  /** Mantiene el foco en el campo para que el lector USB siempre escriba ahí. */
  @HostListener('document:click', ['$event'])
  alHacerClic(evento: MouseEvent): void {
    const objetivo = evento.target as HTMLElement | null;
    if (!objetivo?.closest('input, select, textarea, button, a')) this.enfocar();
  }

  enviar(metodo: 'ESCANER' | 'CAMARA' | 'MANUAL' = 'ESCANER'): void {
    const codigo = this.codigo.trim();
    this.codigo = '';
    if (!codigo || !this.puedeRegistrar) return;
    // Evita registrar dos veces si el lector repite la lectura.
    const ahora = Date.now();
    if (codigo === this.ultimoCodigo && ahora - this.ultimoMomento < 2500) return;
    this.ultimoCodigo = codigo;
    this.ultimoMomento = ahora;

    this.enviando.set(true);
    this.ingresos.registrar(codigo, metodo).subscribe({
      next: ingreso => {
        this.enviando.set(false);
        this.mostrar({ tipo: 'ok', ingreso });
        this.sonar(ingreso.yaRegistrado ? 'aviso' : ingreso.estado === 'TARDANZA' ? 'aviso' : 'ok');
        if (!ingreso.yaRegistrado) {
          this.recientes.update(lista => [ingreso, ...lista.filter(i => i.id !== ingreso.id)].slice(0, 30));
          this.resumen.update(r => ({
            ...r,
            ingresaron: r.ingresaron + 1,
            puntuales: r.puntuales + (ingreso.estado === 'PUNTUAL' ? 1 : 0),
            tardanzas: r.tardanzas + (ingreso.estado === 'TARDANZA' ? 1 : 0)
          }));
        }
        this.enfocar();
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        this.mostrar({ tipo: 'error', mensaje: error.error?.message || 'No se pudo registrar. Intenta otra vez.' });
        this.sonar('error');
        this.enfocar();
      }
    });
  }

  async alternarCamara(): Promise<void> {
    if (this.camaraActiva()) {
      this.detenerCamara();
      return;
    }
    this.errorCamara.set('');
    this.camaraActiva.set(true);
    await new Promise(r => setTimeout(r));
    const video = this.video()?.nativeElement;
    if (!video) return;
    try {
      // La librería se descarga solo al usar la cámara.
      const { BrowserMultiFormatReader } = await import('@zxing/browser');
      const lector = new BrowserMultiFormatReader();
      this.controlesCamara = await lector.decodeFromVideoDevice(undefined, video, resultado => {
        if (!resultado) return;
        this.codigo = resultado.getText();
        this.enviar('CAMARA');
      });
    } catch {
      this.camaraActiva.set(false);
      this.errorCamara.set('No se pudo abrir la cámara. Revisa los permisos del navegador o usa el lector.');
    }
  }

  nombreSeccion(id: number | null | undefined): string {
    return id != null ? this.secciones().get(id) ?? `Sección ${id}` : '';
  }

  hora(valor: string): string {
    return valor?.slice(0, 5) ?? '';
  }

  private detenerCamara(): void {
    this.controlesCamara?.stop();
    this.controlesCamara = null;
    this.camaraActiva.set(false);
  }

  private cargarResumen(): void {
    this.ingresos.resumenHoy().subscribe({
      next: r => {
        this.resumen.set({ activos: r.estudiantesActivos, ingresaron: r.ingresaron, puntuales: r.puntuales, tardanzas: r.tardanzas });
        this.recientes.set(r.recientes.slice(0, 30));
      },
      error: () => undefined
    });
  }

  private mostrar(resultado: Resultado): void {
    this.resultado.set(resultado);
    if (this.temporizadorResultado) clearTimeout(this.temporizadorResultado);
    this.temporizadorResultado = setTimeout(() => this.resultado.set(null), 6000);
  }

  private enfocar(): void {
    if (this.puedeRegistrar) this.campo()?.nativeElement.focus();
  }

  /** Pitido corto para confirmar sin mirar la pantalla. */
  private sonar(tipo: 'ok' | 'aviso' | 'error'): void {
    try {
      this.audio ??= new AudioContext();
      const osc = this.audio.createOscillator();
      const gain = this.audio.createGain();
      osc.frequency.value = tipo === 'ok' ? 880 : tipo === 'aviso' ? 520 : 220;
      gain.gain.setValueAtTime(0.12, this.audio.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audio.currentTime + (tipo === 'error' ? 0.45 : 0.18));
      osc.connect(gain).connect(this.audio.destination);
      osc.start();
      osc.stop(this.audio.currentTime + 0.5);
    } catch {
      /* sin audio disponible */
    }
  }
}
