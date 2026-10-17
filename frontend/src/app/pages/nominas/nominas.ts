import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { SeccionService } from '../../core/services/seccion.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ToastService } from '../../core/services/toast.service';
import { Seccion } from '../../core/models/seccion.model';
import { EstadoNomina, FilaNomina, NominaService } from '../../services/nomina.service';
import { descargarPdf } from '../../shared/pdf/pdf-tabla';

interface FilaSeccion {
  seccion: Seccion;
  etiqueta: string;
  estado: EstadoNomina | null;
}

/**
 * Nómina de matrícula por sección: Secretaría la revisa e imprime; Dirección la aprueba y firma
 * (RM 432-2020-MINEDU). Si luego cambia el número de estudiantes, queda desactualizada.
 */
@Component({
  selector: 'app-nominas',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './nominas.html',
  styleUrl: './nominas.css'
})
export class Nominas implements OnInit {
  private readonly nominas = inject(NominaService);
  private readonly seccionService = inject(SeccionService);
  private readonly realtime = inject(RealtimeService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly esDirector = this.auth.hasRole('DIRECTOR');
  readonly anio = signal(new Date().getFullYear());
  readonly anios = [this.anio() - 1, this.anio(), this.anio() + 1];
  readonly secciones = signal<Seccion[]>([]);
  readonly estados = signal<Map<number, EstadoNomina>>(new Map());
  readonly cargando = signal(false);
  readonly error = signal('');

  readonly seleccion = signal<FilaSeccion | null>(null);
  readonly detalle = signal<FilaNomina[]>([]);
  readonly cargandoDetalle = signal(false);
  readonly aprobando = signal(false);
  observacion = '';

  readonly filas = computed<FilaSeccion[]>(() => this.secciones()
    .filter(s => s.anioEscolar === this.anio() && s.activa !== false)
    .map(s => ({ seccion: s, etiqueta: this.etiqueta(s), estado: this.estados().get(s.id!) ?? null }))
    .sort((a, b) => (a.seccion.nivelNombre ?? '').localeCompare(b.seccion.nivelNombre ?? '')
      || a.seccion.gradoId - b.seccion.gradoId || a.seccion.nombre.localeCompare(b.seccion.nombre)));

  readonly resumen = computed(() => {
    const filas = this.filas();
    const conEstudiantes = filas.filter(f => (f.estado?.estudiantes ?? 0) > 0);
    return {
      secciones: filas.length,
      estudiantes: filas.reduce((t, f) => t + (f.estado?.estudiantes ?? 0), 0),
      aprobadas: filas.filter(f => f.estado?.aprobada && !f.estado.desactualizada).length,
      porAprobar: conEstudiantes.filter(f => !f.estado?.aprobada || f.estado.desactualizada).length
    };
  });

  ngOnInit(): void {
    this.seccionService.getSecciones().subscribe({
      next: lista => this.secciones.set(lista),
      error: () => this.error.set('No se pudieron cargar las secciones.')
    });
    this.cargar();
    this.realtime.alCambiar('nominas', 'estudiantes').pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
  }

  cambiarAnio(anio: number): void {
    this.anio.set(Number(anio));
    this.seleccion.set(null);
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.nominas.estado(this.anio()).subscribe({
      next: lista => {
        this.estados.set(new Map(lista.map(e => [e.seccionId, e])));
        this.cargando.set(false);
        const actual = this.seleccion();
        if (actual) this.seleccion.set({ ...actual, estado: this.estados().get(actual.seccion.id!) ?? null });
      },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(e.error?.message || 'No se pudo consultar las nóminas.'); }
    });
  }

  abrir(fila: FilaSeccion): void {
    this.seleccion.set(fila);
    this.observacion = '';
    this.cargandoDetalle.set(true);
    this.nominas.detalle(fila.seccion.id!).subscribe({
      next: filas => { this.detalle.set(filas); this.cargandoDetalle.set(false); },
      error: () => { this.detalle.set([]); this.cargandoDetalle.set(false); }
    });
  }

  aprobar(): void {
    const fila = this.seleccion();
    if (!fila) return;
    this.aprobando.set(true);
    this.nominas.aprobar(fila.seccion.id!, this.anio(), this.observacion).subscribe({
      next: estado => {
        this.aprobando.set(false);
        this.estados.update(m => new Map(m).set(estado.seccionId, estado));
        this.seleccion.set({ ...fila, estado });
        this.toast.exito('Nómina aprobada', `${fila.etiqueta}: ${estado.estudiantes} estudiantes`);
      },
      error: (e: HttpErrorResponse) => { this.aprobando.set(false); this.toast.error('No se pudo aprobar', e.error?.message); }
    });
  }

  situacion(e: EstadoNomina | null): 'vacia' | 'pendiente' | 'desactualizada' | 'aprobada' {
    if (!e || (!e.aprobada && e.estudiantes === 0)) return 'vacia';
    if (!e.aprobada) return 'pendiente';
    return e.desactualizada ? 'desactualizada' : 'aprobada';
  }

  descargar(): void {
    const fila = this.seleccion();
    if (!fila) return;
    const estado = fila.estado;
    descargarPdf(`nomina-${fila.etiqueta.replace(/\s+/g, '-').toLowerCase()}-${this.anio()}.pdf`, {
      titulo: 'Nómina de matrícula',
      subtitulo: `Año escolar ${this.anio()}`,
      datos: [
        ['Nivel', fila.seccion.nivelNombre ?? '—'],
        ['Grado y sección', fila.etiqueta],
        ['Turno', fila.seccion.turno === 'TARDE' ? 'Tarde' : 'Mañana'],
        ['Tutor(a)', fila.seccion.docenteTutorNombre ?? 'Sin asignar'],
        ['Aprobación', estado?.aprobada
          ? `${estado.aprobadaPor} · ${new Date(estado.fechaAprobacion!).toLocaleDateString('es-PE')}${estado.desactualizada ? ' (desactualizada)' : ''}`
          : 'Pendiente de aprobación de Dirección']
      ],
      columnas: [
        { titulo: 'N.º', ancho: 0.5, alinear: 'centro' },
        { titulo: 'DNI', ancho: 1.2, alinear: 'centro' },
        { titulo: 'Apellidos', ancho: 3 },
        { titulo: 'Nombres', ancho: 3 },
        { titulo: 'Observación', ancho: 2.3 }
      ],
      filas: this.detalle().map(f => [
        { texto: String(f.numero) }, { texto: f.dni }, { texto: f.apellidos, negrita: true }, { texto: f.nombres }, { texto: '' }
      ]),
      pie: `Total: ${this.detalle().length} estudiantes · Firma y sello de Dirección: ____________________`
    });
  }

  imprimir(): void {
    window.print();
  }

  private etiqueta(s: Seccion): string {
    return `${s.gradoNombre ?? ''} ${s.nombre}`.trim();
  }
}
