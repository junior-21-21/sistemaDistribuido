import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap, catchError, map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { SeccionService } from '../../core/services/seccion.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ToastService } from '../../core/services/toast.service';
import { Estudiante, EstudianteService } from '../../services/estudiante.service';
import { EstadoPermiso, NuevoPermiso, Permiso, PermisoService, TIPO_PERMISO, TipoPermiso } from '../../services/permiso.service';

type Vista = 'solicitudes' | 'salidas' | 'registrar';

/**
 * Justificaciones, papeletas de salida y permisos. El apoderado acude al auxiliar o al tutor para lo del día;
 * lo formal (varios días) lo registra Secretaría y lo aprueba Dirección. Portería verifica las salidas de hoy.
 */
@Component({
  selector: 'app-permisos',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './permisos.html',
  styleUrl: './permisos.css'
})
export class Permisos implements OnInit {
  private readonly permisos = inject(PermisoService);
  private readonly estudiantes = inject(EstudianteService);
  private readonly seccionService = inject(SeccionService);
  private readonly realtime = inject(RealtimeService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tipos = TIPO_PERMISO;
  readonly esPortero = this.auth.hasRole('PORTERO') && !this.auth.hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA');
  readonly puedeRegistrar = this.auth.hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA');
  readonly apruebaDiario = this.auth.hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR');
  readonly apruebaFormal = this.auth.hasRole('DIRECTOR');
  readonly registraFormal = this.auth.hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR');
  readonly registraSalida = this.auth.hasAnyRole('PORTERO', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA');

  readonly vista = signal<Vista>(this.esPortero ? 'salidas' : 'solicitudes');
  readonly lista = signal<Permiso[]>([]);
  readonly salidas = signal<Permiso[]>([]);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly secciones = signal<Map<number, string>>(new Map());
  readonly enviando = signal(false);

  filtroEstado: '' | EstadoPermiso = 'PENDIENTE';
  filtroTexto = '';
  readonly texto = signal('');

  readonly visibles = computed(() => {
    const q = this.texto().trim().toLowerCase();
    return this.lista().filter(p => !q || p.estudianteNombre.toLowerCase().includes(q) || p.estudianteDni.includes(q));
  });
  readonly pendientes = computed(() => this.lista().filter(p => p.estado === 'PENDIENTE').length);
  readonly salidasPorRegistrar = computed(() => this.salidas().filter(p => !p.fechaSalida).length);

  // Registro
  readonly busqueda$ = new Subject<string>();
  readonly sugerencias = signal<Estudiante[]>([]);
  readonly elegido = signal<Estudiante | null>(null);
  consulta = '';
  form: NuevoPermiso = this.formVacio();

  // Revisión
  readonly revisando = signal<Permiso | null>(null);
  observacion = '';

  ngOnInit(): void {
    this.seccionService.getSecciones().subscribe({
      next: lista => this.secciones.set(new Map(lista.filter(s => s.id != null)
        .map(s => [s.id!, `${s.gradoNombre ?? ''} ${s.nombre}`.trim()]))),
      error: () => undefined
    });
    this.busqueda$.pipe(
      map(q => q.trim()),
      debounceTime(250),
      distinctUntilChanged(),
      switchMap(q => q.length < 2 ? of([]) : this.estudiantes.getEstudiantesPage(0, 8, q).pipe(
        map(p => p.content.filter(e => e.estado == null || e.estado === 'ACTIVO')),
        catchError(() => of([])))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(lista => this.sugerencias.set(lista));
    this.recargar();
    this.realtime.alCambiar('permisos').pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.recargar());
  }

  cambiarVista(v: Vista): void {
    this.vista.set(v);
    if (v === 'registrar') this.form = this.formVacio();
  }

  recargar(): void {
    this.cargando.set(true);
    this.error.set('');
    if (!this.esPortero) {
      this.permisos.listar(this.filtroEstado).subscribe({
        next: lista => { this.lista.set(lista); this.cargando.set(false); },
        error: (e: HttpErrorResponse) => { this.error.set(this.mensaje(e)); this.cargando.set(false); }
      });
    }
    this.permisos.salidasDeHoy().subscribe({
      next: lista => { this.salidas.set(lista); if (this.esPortero) this.cargando.set(false); },
      error: () => { this.salidas.set([]); if (this.esPortero) this.cargando.set(false); }
    });
  }

  elegir(e: Estudiante): void {
    this.elegido.set(e);
    this.form.estudianteId = e.id!;
    this.consulta = '';
    this.sugerencias.set([]);
  }

  quitarEstudiante(): void {
    this.elegido.set(null);
    this.form.estudianteId = 0;
  }

  cambiarTipo(tipo: TipoPermiso): void {
    this.form.tipo = tipo;
    const hoy = this.hoy();
    if (tipo === 'PAPELETA_SALIDA') { this.form.fechaDesde = hoy; this.form.fechaHasta = hoy; }
    if (tipo === 'PERMISO' && this.form.fechaDesde < hoy) this.form.fechaDesde = hoy;
  }

  tiposDisponibles(): TipoPermiso[] {
    return (['JUSTIFICACION', 'PAPELETA_SALIDA', 'PERMISO'] as TipoPermiso[])
      .filter(t => t !== 'PERMISO' || this.registraFormal);
  }

  puedeAprobarAhora(): boolean {
    return this.form.tipo === 'PERMISO' ? this.apruebaFormal : this.apruebaDiario;
  }

  puedeRevisar(p: Permiso): boolean {
    return p.estado === 'PENDIENTE' && (p.tipo === 'PERMISO' ? this.apruebaFormal : this.apruebaDiario);
  }

  registrar(): void {
    if (!this.form.estudianteId) { this.toast.error('Elige al estudiante'); return; }
    const datos: NuevoPermiso = {
      ...this.form,
      fechaHasta: this.form.tipo === 'PAPELETA_SALIDA' ? this.form.fechaDesde : (this.form.fechaHasta || this.form.fechaDesde),
      horaSalida: this.form.tipo === 'PAPELETA_SALIDA' ? this.form.horaSalida : null,
      personaRecoge: this.form.tipo === 'PAPELETA_SALIDA' ? this.form.personaRecoge?.trim() : null,
      dniPersonaRecoge: this.form.tipo === 'PAPELETA_SALIDA' ? (this.form.dniPersonaRecoge?.trim() || null) : null,
      motivo: this.form.motivo.trim(),
      aprobarAhora: this.puedeAprobarAhora() && !!this.form.aprobarAhora
    };
    this.enviando.set(true);
    this.permisos.registrar(datos).subscribe({
      next: p => {
        this.enviando.set(false);
        this.toast.exito(p.estado === 'APROBADA' ? 'Registrado y aprobado' : 'Solicitud registrada',
          p.asistenciasJustificadas ? `${p.asistenciasJustificadas} registro(s) de asistencia justificados` : p.estudianteNombre);
        this.quitarEstudiante();
        this.form = this.formVacio();
        this.filtroEstado = '';
        this.vista.set('solicitudes');
        this.recargar();
      },
      error: (e: HttpErrorResponse) => { this.enviando.set(false); this.toast.error('No se pudo registrar', this.mensaje(e)); }
    });
  }

  abrirRevision(p: Permiso): void {
    this.revisando.set(p);
    this.observacion = '';
  }

  decidir(aprobar: boolean): void {
    const p = this.revisando();
    if (!p) return;
    if (!aprobar && this.observacion.trim().length < 3) { this.toast.error('Indica el motivo del rechazo'); return; }
    this.enviando.set(true);
    this.permisos.revisar(p.id, aprobar, this.observacion.trim()).subscribe({
      next: r => {
        this.enviando.set(false);
        this.revisando.set(null);
        this.toast.exito(aprobar ? 'Solicitud aprobada' : 'Solicitud rechazada',
          r.asistenciasJustificadas ? `${r.asistenciasJustificadas} registro(s) de asistencia justificados` : 'Se avisó al apoderado');
        this.recargar();
      },
      error: (e: HttpErrorResponse) => { this.enviando.set(false); this.toast.error('No se pudo guardar', this.mensaje(e)); }
    });
  }

  marcarSalida(p: Permiso): void {
    this.permisos.registrarSalida(p.id).subscribe({
      next: r => {
        this.salidas.update(l => l.map(x => x.id === r.id ? r : x));
        this.toast.exito('Salida registrada', `${r.estudianteNombre} con ${r.personaRecoge ?? 'su apoderado'}`);
      },
      error: (e: HttpErrorResponse) => this.toast.error('No se pudo registrar la salida', this.mensaje(e))
    });
  }

  nombreSeccion(id: number | null): string {
    return id != null ? this.secciones().get(id) ?? '' : '';
  }

  rango(p: Permiso): string {
    const f = (d: string) => d.split('-').reverse().slice(0, 2).join('/');
    return p.fechaDesde === p.fechaHasta ? f(p.fechaDesde) : `${f(p.fechaDesde)} al ${f(p.fechaHasta)}`;
  }

  hora(h: string | null): string {
    return h ? h.slice(0, 5) : '';
  }

  hoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private formVacio(): NuevoPermiso {
    const hoy = this.hoy();
    return { estudianteId: 0, tipo: 'JUSTIFICACION', fechaDesde: hoy, fechaHasta: hoy, horaSalida: '', motivo: '',
      personaRecoge: '', dniPersonaRecoge: '', aprobarAhora: true };
  }

  private mensaje(e: HttpErrorResponse): string {
    return e.error?.message || e.error?.detail || (e.status === 403 ? 'No tienes permiso para esta acción.' : 'Intenta otra vez.');
  }
}
