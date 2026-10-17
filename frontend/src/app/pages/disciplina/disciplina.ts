import { Component, OnInit, ChangeDetectorRef, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisciplinaService, ProtocoloViolencia, ReporteDisciplinario } from '../../services/disciplina.service';
import { SeccionService } from '../../core/services/seccion.service';
import { Seccion } from '../../core/models/seccion.model';
import { AuthService } from '../../core/services/auth.service';
import { EstudianteService } from '../../core/services/estudiante.service';
import { Estudiante } from '../../core/models/estudiante.model';
import { AsignacionAcademica, HorarioService, SeccionHorario } from '../../services/horario.service';
import { catchError, forkJoin, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ContinuarSeccion, SeccionRecordada, UltimaSeccionService } from '../../shared/continuar-seccion/continuar-seccion';

@Component({
  selector: 'app-disciplina',
  standalone: true,
  imports: [ContinuarSeccion, CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './disciplina.html',
  styleUrl: './disciplina.css'
})
export class Disciplina implements OnInit {
  private readonly ultimaSeccion = inject(UltimaSeccionService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  private disciplinaService = inject(DisciplinaService);
  private authService = inject(AuthService);
  private estudianteService = inject(EstudianteService);
  private horarioService = inject(HorarioService);
  private seccionService = inject(SeccionService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  reportes: ReporteDisciplinario[] = [];
  loading = false;
  showModal = false;
  submitting = false;
  successMessage = '';
  errorMessage = '';
  secciones: SeccionHorario[] = [];
  estudiantes: Estudiante[] = [];
  estudiantesSeccion: Estudiante[] = [];
  asignaciones: AsignacionAcademica[] = [];
  seccionIdSeleccionada: number | null = null;

  // Nav
  vistaActual: 'grados' | 'secciones' | 'gestor' = 'grados';
  gradoNavegacion: any = null;

  get puedeCrear(): boolean {
    return !this.authService.hasAnyRole('DIRECTOR', 'SUBDIRECTOR')
      && this.authService.hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA');
  }

  get esDocenteRestringido(): boolean {
    return this.authService.hasRole('DOCENTE')
      && !this.authService.hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA');
  }

  tiposReporte = [
    { value: 'TARDANZA', label: 'Tardanza', plural: 'Tardanzas', color: 'tarde', icon: 'bi-alarm-fill' },
    { value: 'FALTA_LEVE', label: 'Falta leve', plural: 'Faltas leves', color: 'leve', icon: 'bi-exclamation-circle-fill' },
    { value: 'FALTA_GRAVE', label: 'Falta grave', plural: 'Faltas graves', color: 'grave', icon: 'bi-shield-fill-exclamation' },
    { value: 'LLAMADA_ATENCION', label: 'Llamada de atención', plural: 'Llamadas de atención', color: 'atencion', icon: 'bi-megaphone-fill' },
    { value: 'VIOLENCIA_ESCOLAR', label: 'Violencia escolar', plural: 'Casos de violencia', color: 'violencia', icon: 'bi-shield-fill-x' },
    { value: 'MERITO', label: 'Mérito', plural: 'Méritos', color: 'merito', icon: 'bi-star-fill' },
  ];

  reporteForm: FormGroup = this.fb.group({
    estudianteId: [null, Validators.required],
    tipo: ['TARDANZA', Validators.required],
    descripcion: ['', [Validators.required, Validators.minLength(10)]],
    observaciones: ['']
  });

  ngOnInit() {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('reportes-disciplinarios', 'disciplina').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.cargarReportes(); });
    this.cargarContexto();
  }

  cargarContexto(): void {
    const anio = new Date().getFullYear();
    const asignaciones$ = this.esDocenteRestringido
      ? this.horarioService.listarMisAsignaciones(anio)
      : this.horarioService.listarAsignaciones(anio);
    // El tutor ve también su aula aunque no dicte un curso en ella.
    const tutorias$ = this.esDocenteRestringido
      ? this.seccionService.getMisTutorias().pipe(catchError(() => of([] as Seccion[])))
      : of([] as Seccion[]);
    this.loading = true;
    forkJoin({
      secciones: this.horarioService.listarSecciones(),
      estudiantes: this.estudianteService.getEstudiantes(),
      asignaciones: asignaciones$,
      tutorias: tutorias$
    }).subscribe({
      next: ({ secciones, estudiantes, asignaciones, tutorias }) => {
        this.asignaciones = asignaciones.filter(item => item.activa !== false);
        const seccionesPermitidas = this.esDocenteRestringido
          ? new Set([...this.asignaciones.map(item => item.seccionId), ...tutorias.map(t => t.id!)])
          : null;
        this.secciones = secciones.filter(item =>
          item.activa !== false
          && item.nivelNombre === 'SECUNDARIA'
          && (!seccionesPermitidas || seccionesPermitidas.has(item.id))
        );
        this.estudiantes = estudiantes.filter(item => item.estado !== 'INACTIVO');
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se pudo cargar el contexto académico de convivencia.';
        this.cdr.detectChanges();
      }
    });
  }

  // --- Nav Logic ---
  get gradosDisponibles(): any[] {
    const uniqueMap = new Map();
    this.secciones.forEach(s => {
      if (s.gradoId && !uniqueMap.has(s.gradoId)) {
        uniqueMap.set(s.gradoId, { id: s.gradoId, nombre: s.gradoNombre });
      }
    });
    return Array.from(uniqueMap.values()).sort((a,b) => a.id - b.id);
  }

  /** Abre directamente la última sección usada (salta los pasos de grado y sección). */
  continuarUltima(seccion: SeccionRecordada): void {
    this.navegarAGrado({ id: seccion.gradoId, nombre: seccion.gradoNombre });
    this.navegarASeccion(seccion);
  }

  navegarAGrado(grado: any): void {
    this.gradoNavegacion = grado;
    this.vistaActual = 'secciones';
  }

  navegarASeccion(seccion: any): void {
    this.ultimaSeccion.guardar(seccion);
    this.seccionIdSeleccionada = seccion.id;
    this.vistaActual = 'gestor';
    this.actualizarSeccion();
  }

  volverAGrados(): void {
    this.vistaActual = 'grados';
    this.gradoNavegacion = null;
    this.seccionIdSeleccionada = null;
  }

  volverASecciones(): void {
    this.vistaActual = 'secciones';
    this.seccionIdSeleccionada = null;
  }

  getSeccionesDelGradoNav(): SeccionHorario[] {
    if (!this.gradoNavegacion) return [];
    return this.secciones.filter(s => s.gradoId?.toString() === this.gradoNavegacion.id?.toString());
  }

  getSeccionesMananaNav(): SeccionHorario[] {
    return this.getSeccionesDelGradoNav().filter(s => s.turno === 'MANANA');
  }

  getSeccionesTardeNav(): SeccionHorario[] {
    return this.getSeccionesDelGradoNav().filter(s => s.turno === 'TARDE');
  }
  // ----------------

  actualizarSeccion(): void {
    this.estudiantesSeccion = this.estudiantes.filter(
      item => item.seccionId === this.seccionIdSeleccionada
    );
    this.reporteForm.patchValue({
      estudianteId: this.estudiantesSeccion[0]?.id ?? null
    });
    this.cargarReportes();
  }

  cargarReportes() {
    if (!this.seccionIdSeleccionada) {
      this.reportes = [];
      this.loading = false;
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.disciplinaService.getReportesPorSeccion(this.seccionIdSeleccionada).subscribe({
      next: (data) => {
        this.reportes = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se pudieron cargar los reportes de la sección.';
        this.cdr.detectChanges();
      }
    });
  }

  abrirModal(estudianteId: number | null = null) {
    this.errorMessage = '';
    this.busquedaModal = '';
    this.reporteForm.reset({
      estudianteId,
      tipo: 'TARDANZA',
      descripcion: '',
      observaciones: ''
    });
    this.showModal = true;
  }
  cerrarModal() { this.showModal = false; }

  guardarReporte() {
    if (this.reporteForm.invalid) {
      this.reporteForm.markAllAsTouched();
      return;
    }
    this.submitting = true;
    if (!this.seccionIdSeleccionada) return;
    const docenteId = this.asignaciones.find(
      item => item.seccionId === this.seccionIdSeleccionada
    )?.docenteId;
    if (this.esDocenteRestringido && !docenteId) {
      this.errorMessage = 'No existe una carga docente activa para esta sección.';
      this.submitting = false;
      return;
    }
    const reporte: ReporteDisciplinario = {
      ...this.reporteForm.value,
      docenteId: this.esDocenteRestringido ? docenteId : undefined,
      seccionId: this.seccionIdSeleccionada
    };
    this.disciplinaService.crearReporte(reporte).subscribe({
      next: (data) => {
        this.reportes.unshift(data);
        this.submitting = false;
        this.showModal = false;
        this.reporteForm.reset({
          estudianteId: this.estudiantesSeccion[0]?.id ?? null,
          tipo: 'TARDANZA'
        });
        this.successMessage = 'Papeleta emitida. La familia recibe el aviso.';
        this.cdr.detectChanges();
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message ?? 'Error al emitir la papeleta digital.';
        this.cdr.detectChanges();
      }
    });
  }

  tipoBadgeClass(tipo: string): string {
    const map: Record<string, string> = {
      'TARDANZA': 'badge-tardanza',
      'FALTA_LEVE': 'badge-leve',
      'FALTA_GRAVE': 'badge-grave',
      'MERITO': 'badge-merito',
      'LLAMADA_ATENCION': 'badge-atencion',
      'VIOLENCIA_ESCOLAR': 'badge-grave'
    };
    return map[tipo] ?? '';
  }

  tipoLabel(tipo: string): string {
    return this.tiposReporte.find(t => t.value === tipo)?.label ?? tipo;
  }

  nombreEstudiante(estudianteId: number): string {
    const estudiante = this.estudiantes.find(item => item.id === estudianteId);
    return estudiante ? `${estudiante.nombre} ${estudiante.apellido}` : `Estudiante #${estudianteId}`;
  }

  getTardanzas() { return this.reportes.filter(r => r.tipo === 'TARDANZA').length; }
  getFaltasGraves() { return this.reportes.filter(r => r.tipo === 'FALTA_GRAVE').length; }
  getMeritos() { return this.reportes.filter(r => r.tipo === 'MERITO').length; }

  // --- Vista de la sección: filtros, agrupación por día y estudiantes a acompañar ---
  busqueda = '';
  filtroTipo: string | null = null;
  filtroEstudiante: number | null = null;
  limite = 25;
  busquedaModal = '';
  actualizandoId: number | null = null;

  /** Frases frecuentes por tipo: un toque llena la descripción. */
  readonly sugerencias: Record<string, string[]> = {
    TARDANZA: ['Llegó tarde después del recreo', 'Ingresó tarde a la primera hora sin justificación'],
    FALTA_LEVE: ['Uso del celular durante la clase', 'No trajo los materiales de trabajo', 'Incumple con el uniforme escolar'],
    FALTA_GRAVE: ['Agresión verbal a un compañero', 'Daño intencional al mobiliario del aula', 'Se retiró del colegio sin permiso'],
    MERITO: ['Ayudó a un compañero con dificultades de aprendizaje', 'Destacada participación en clase', 'Devolvió un objeto perdido'],
    LLAMADA_ATENCION: ['Conversa constantemente e interrumpe la clase', 'Sale del aula sin permiso'],
    VIOLENCIA_ESCOLAR: ['Agresión física a un compañero durante el recreo', 'Acoso reiterado a un compañero (burlas, exclusión)', 'Difusión de imágenes ofensivas de un compañero']
  };

  get puedeGestionarEstado(): boolean { return this.authService.hasRole('RESPONSABLE_CONVIVENCIA'); }
  /** TOE y Dirección completan el protocolo de violencia escolar (SíseVe, citación, medidas, derivación). */
  get puedeProtocolo(): boolean { return this.authService.hasAnyRole('RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR'); }
  readonly derivaciones = [
    { value: 'PSICOLOGIA', label: 'Psicología del colegio' }, { value: 'DEMUNA', label: 'DEMUNA' },
    { value: 'CEM', label: 'Centro Emergencia Mujer (CEM)' }, { value: 'POLICIA', label: 'Policía Nacional' },
    { value: 'FISCALIA', label: 'Fiscalía' }, { value: 'CENTRO_SALUD', label: 'Centro de salud' }
  ];
  protocoloDe: ReporteDisciplinario | null = null;
  protocolo: ProtocoloViolencia = {};
  guardandoProtocolo = false;

  aplicaProtocolo(r: ReporteDisciplinario): boolean { return r.tipo === 'VIOLENCIA_ESCOLAR' || r.tipo === 'FALTA_GRAVE'; }
  pasosProtocolo(r: ReporteDisciplinario): number {
    return [r.codigoSiseve, r.citacionApoderado, r.medidasProteccion, r.derivacionExterna].filter(Boolean).length;
  }
  nombreDerivacion(v?: string | null): string { return this.derivaciones.find(d => d.value === v)?.label ?? v ?? ''; }

  abrirProtocolo(r: ReporteDisciplinario): void {
    this.protocoloDe = r;
    this.protocolo = {
      codigoSiseve: r.codigoSiseve ?? '', fechaSiseve: r.fechaSiseve ?? '',
      citacionApoderado: r.citacionApoderado?.slice(0, 16) ?? '', medidasProteccion: r.medidasProteccion ?? '',
      derivacionExterna: r.derivacionExterna ?? '', fechaDerivacion: r.fechaDerivacion ?? ''
    };
  }

  guardarProtocolo(): void {
    const r = this.protocoloDe;
    if (!r?.id) return;
    const limpio = Object.fromEntries(Object.entries(this.protocolo)
      .map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v])) as ProtocoloViolencia;
    if (limpio.citacionApoderado && limpio.citacionApoderado.length === 16) limpio.citacionApoderado += ':00';
    this.guardandoProtocolo = true;
    this.disciplinaService.actualizarProtocolo(r.id, limpio).subscribe({
      next: actualizado => {
        Object.assign(r, actualizado);
        this.guardandoProtocolo = false;
        this.protocoloDe = null;
        this.successMessage = 'Protocolo actualizado.';
        this.cdr.detectChanges();
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3500);
      },
      error: err => {
        this.guardandoProtocolo = false;
        this.errorMessage = err?.error?.message ?? 'No se pudo guardar el protocolo.';
        this.cdr.detectChanges();
      }
    });
  }
  get seccionActual(): SeccionHorario | undefined { return this.secciones.find(s => s.id === this.seccionIdSeleccionada); }
  get nombreSeccion(): string { const s = this.seccionActual; return s ? `${s.gradoNombre} ${s.nombre}` : ''; }
  get turnoSeccion(): string { return this.seccionActual?.turno === 'TARDE' ? 'Turno tarde' : 'Turno mañana'; }
  get delMes(): number {
    const mes = new Date().toISOString().slice(0, 7);
    return this.reportes.filter(r => r.fechaReporte?.startsWith(mes)).length;
  }
  get pendientes(): number { return this.reportes.filter(r => (r.estado ?? 'EMITIDO') === 'EMITIDO').length; }

  contarTipo(tipo: string): number { return this.reportes.filter(r => r.tipo === tipo).length; }
  tipoInfo(tipo: string) { return this.tiposReporte.find(t => t.value === tipo) ?? this.tiposReporte[0]; }
  alternarTipo(tipo: string): void { this.filtroTipo = this.filtroTipo === tipo ? null : tipo; this.limite = 25; }
  alternarEstudiante(id: number): void { this.filtroEstudiante = this.filtroEstudiante === id ? null : id; this.limite = 25; }
  limpiarFiltros(): void { this.filtroTipo = null; this.filtroEstudiante = null; this.busqueda = ''; }

  get reportesFiltrados(): ReporteDisciplinario[] {
    const texto = this.normalizar(this.busqueda);
    return this.reportes
      .filter(r => !this.filtroTipo || r.tipo === this.filtroTipo)
      .filter(r => !this.filtroEstudiante || r.estudianteId === this.filtroEstudiante)
      .filter(r => !texto || this.normalizar(`${this.nombreDe(r)} ${r.descripcion}`).includes(texto))
      .sort((a, b) => (b.fechaReporte ?? '').localeCompare(a.fechaReporte ?? ''));
  }

  /** Papeletas visibles agrupadas por día («Hoy», «Ayer», «martes 13 de octubre»). */
  get grupos(): { dia: string; items: ReporteDisciplinario[] }[] {
    const grupos: { dia: string; items: ReporteDisciplinario[] }[] = [];
    for (const r of this.reportesFiltrados.slice(0, this.limite)) {
      const dia = this.etiquetaDia(r.fechaReporte);
      const ultimo = grupos[grupos.length - 1];
      if (ultimo?.dia === dia) ultimo.items.push(r); else grupos.push({ dia, items: [r] });
    }
    return grupos;
  }

  /** Estudiantes con más incidencias (una falta grave pesa como tres). */
  get aAcompanar(): { id: number; nombre: string; puntos: number; detalle: string }[] {
    const peso: Record<string, number> = { VIOLENCIA_ESCOLAR: 3, FALTA_GRAVE: 3, FALTA_LEVE: 1, LLAMADA_ATENCION: 1, TARDANZA: 1 };
    const mapa = new Map<number, { puntos: number; tipos: Record<string, number> }>();
    for (const r of this.reportes) {
      if (!peso[r.tipo]) continue;
      const item = mapa.get(r.estudianteId) ?? { puntos: 0, tipos: {} };
      item.puntos += peso[r.tipo];
      item.tipos[r.tipo] = (item.tipos[r.tipo] ?? 0) + 1;
      mapa.set(r.estudianteId, item);
    }
    return [...mapa.entries()].sort((a, b) => b[1].puntos - a[1].puntos).slice(0, 6).map(([id, v]) => ({
      id, nombre: this.nombreEstudiante(id), puntos: v.puntos,
      detalle: Object.entries(v.tipos)
        .map(([t, n]) => `${n} ${(n === 1 ? this.tipoLabel(t) : this.tipoInfo(t).plural).toLowerCase()}`).join(' · ')
    }));
  }

  get destacados(): { id: number; nombre: string; meritos: number }[] {
    const mapa = new Map<number, number>();
    this.reportes.filter(r => r.tipo === 'MERITO').forEach(r => mapa.set(r.estudianteId, (mapa.get(r.estudianteId) ?? 0) + 1));
    return [...mapa.entries()].sort((a, b) => b[1] - a[1]).slice(0, 4).map(([id, meritos]) => ({ id, nombre: this.nombreEstudiante(id), meritos }));
  }

  get maxPuntos(): number { return Math.max(1, ...this.aAcompanar.map(a => a.puntos)); }

  get estudiantesModal(): Estudiante[] {
    const texto = this.normalizar(this.busquedaModal);
    return this.estudiantesSeccion
      .filter(e => !texto || this.normalizar(`${e.apellido} ${e.nombre} ${e.dni ?? ''}`).includes(texto))
      .sort((a, b) => `${a.apellido}`.localeCompare(`${b.apellido}`, 'es'));
  }

  nombreDe(r: ReporteDisciplinario): string { return r.nombreEstudiante ?? this.nombreEstudiante(r.estudianteId); }
  iniciales(nombre: string): string {
    return nombre.split(' ').filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase();
  }
  hora(fecha?: string): string { return fecha ? fecha.slice(11, 16) : ''; }

  usarSugerencia(texto: string): void { this.reporteForm.patchValue({ descripcion: texto }); }
  elegirEstudiante(id: number | undefined): void { if (id != null) this.reporteForm.patchValue({ estudianteId: id }); }
  get estudianteElegido(): Estudiante | undefined {
    return this.estudiantesSeccion.find(e => e.id === this.reporteForm.value.estudianteId);
  }

  cambiarEstado(reporte: ReporteDisciplinario, estado: 'NOTIFICADO' | 'ARCHIVADO'): void {
    if (!reporte.id || this.actualizandoId) return;
    this.actualizandoId = reporte.id;
    this.disciplinaService.actualizarEstado(reporte.id, estado).subscribe({
      next: actualizado => {
        Object.assign(reporte, actualizado);
        this.actualizandoId = null;
        this.successMessage = estado === 'NOTIFICADO' ? 'Papeleta marcada como notificada a la familia.' : 'Papeleta archivada.';
        this.cdr.detectChanges();
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3500);
      },
      error: err => {
        this.actualizandoId = null;
        this.errorMessage = err?.error?.message ?? 'No se pudo cambiar el estado de la papeleta.';
        this.cdr.detectChanges();
      }
    });
  }

  private etiquetaDia(fecha?: string): string {
    if (!fecha) return 'Sin fecha';
    const dia = fecha.slice(0, 10);
    const hoy = new Date();
    const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const ayer = new Date(hoy); ayer.setDate(hoy.getDate() - 1);
    if (dia === iso(hoy)) return 'Hoy';
    if (dia === iso(ayer)) return 'Ayer';
    const d = new Date(`${dia}T12:00:00`);
    const texto = d.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  private normalizar(texto: string): string {
    return (texto ?? '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
  }
}
