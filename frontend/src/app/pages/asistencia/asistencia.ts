import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { Estudiante, EstudianteService } from '../../services/estudiante.service';
import { AsignacionAcademica, CursoHorario, DocenteHorario, HorarioDTO, HorarioService, SeccionHorario } from '../../services/horario.service';
import { Asistencia, NotaService, SesionAsistencia } from '../../services/nota.service';
import { LiveBanner } from '../../shared/live-banner/live-banner';
import { ContinuarSeccion, SeccionRecordada, UltimaSeccionService } from '../../shared/continuar-seccion/continuar-seccion';
import { descargarXlsx } from '../../shared/excel/xlsx';
import { descargarPdf } from '../../shared/pdf/pdf-tabla';
import { PeriodoReporte, construirMatriz, describirPeriodo, rangoDelPeriodo, reportePdf, reporteXlsx } from './reporte-asistencia';

type EstadoAsistencia = 'PRESENTE' | 'TARDE' | 'FALTA' | 'JUSTIFICADO';
const ETIQUETAS: Record<EstadoAsistencia, string> = { PRESENTE: 'Presente', TARDE: 'Tarde', FALTA: 'Falta', JUSTIFICADO: 'Justificada' };
const DIAS_LARGOS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'setiembre', 'octubre', 'noviembre', 'diciembre'];

interface FilaAsistencia extends Estudiante {
  id: number;
  estadoAsistencia: EstadoAsistencia;
  justificacion: string;
}

export interface CursoCard {
  cursoId: number;
  seccionId: number;
  cursoNombre: string;
  seccionNombre: string;
  gradoNombre: string;
  docenteNombre: string;
}

@Component({
  selector: 'app-asistencia',
  standalone: true,
  imports: [ContinuarSeccion, LiveBanner, CommonModule, FormsModule],
  templateUrl: './asistencia.html',
  styleUrl: './asistencia.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AsistenciaPage implements OnInit {
  private readonly ultimaSeccion = inject(UltimaSeccionService);
  private readonly notas = inject(NotaService);
  private readonly estudiantesService = inject(EstudianteService);
  private readonly horariosService = inject(HorarioService);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly estados = ['PRESENTE', 'TARDE', 'FALTA', 'JUSTIFICADO'] as const;
  fecha = this.fechaLocal();
  seccionId: number | null = null;
  cursoId: number | null = null;
  horarioId: number | null = null;
  docenteId: number | null = null;
  secciones: SeccionHorario[] = [];

  // Nav
  vistaActual: 'grados' | 'secciones' | 'cursos' | 'gestor' = 'grados';
  gradoNavegacion: any = null;
  cursos: CursoHorario[] = [];
  docentes: DocenteHorario[] = [];
  horarios: HorarioDTO[] = [];
  horariosPropios: HorarioDTO[] = [];
  asignaciones: AsignacionAcademica[] = [];
  estudiantes: Estudiante[] = [];
  filas: FilaAsistencia[] = [];
  sesion: SesionAsistencia | null = null;
  
  misCursos: CursoCard[] = [];

  loading = true;
  saving = false;
  error = '';
  mensaje = '';

  // Llamado de asistencia
  modo: 'lista' | 'uno' = 'lista';
  indiceActual = 0;
  leerEnVoz = false;
  filtro: EstadoAsistencia | null = null;
  confirmacion: 'abrir' | 'cerrar' | null = null;
  abriendo = false;

  // Exportación
  exportarAbierto = false;
  periodoExport: PeriodoReporte = 'SEMANA';
  referenciaExport = this.fechaLocal();
  exportando: 'PDF' | 'EXCEL' | null = null;
  errorExport = '';
  readonly periodos: { valor: PeriodoReporte; titulo: string; icono: string }[] = [
    { valor: 'DIA', titulo: 'Día', icono: 'bi-calendar-day' },
    { valor: 'SEMANA', titulo: 'Semana', icono: 'bi-calendar-week' },
    { valor: 'MES', titulo: 'Mes', icono: 'bi-calendar-month' }
  ];

  get cerrada(): boolean { return this.sesion?.estado === 'CERRADA'; }
  get bloqueado(): boolean { return this.cerrada || this.soloLectura; }
  /** SIN_INICIAR: nadie abrió el llamado de esta clase; EN_CURSO: abierto; CERRADO: consolidado. */
  get estadoLlamado(): 'SIN_INICIAR' | 'EN_CURSO' | 'CERRADO' {
    return !this.sesion ? 'SIN_INICIAR' : this.cerrada ? 'CERRADO' : 'EN_CURSO';
  }
  get textoEstado(): string {
    if (this.estadoLlamado === 'CERRADO') {
      const hora = this.sesion?.fechaCierre ? ` a las ${this.sesion.fechaCierre.slice(11, 16)}` : '';
      return `Cerrado${hora}${this.sesion?.cerradaPor ? ` por ${this.sesion.cerradaPor}` : ''}`;
    }
    return this.estadoLlamado === 'EN_CURSO' ? 'Llamado en curso' : 'Sin iniciar';
  }
  get fechaFutura(): boolean { return this.fecha > this.fechaLocal(); }
  get fechaLarga(): string {
    const f = new Date(`${this.fecha}T12:00:00`);
    return `${DIAS_LARGOS[f.getDay()]}, ${f.getDate()} de ${MESES[f.getMonth()]}`;
  }
  get seccionActual(): SeccionHorario | undefined { return this.secciones.find(s => s.id === this.seccionId); }
  get nombreSeccion(): string { const s = this.seccionActual; return s ? `${s.gradoNombre} ${s.nombre}` : ''; }
  get turnoSeccion(): string { return this.seccionActual?.turno === 'TARDE' ? 'Turno tarde' : 'Turno mañana'; }
  get nombreCurso(): string { return this.cursos.find(c => c.id === this.cursoId)?.nombre ?? 'Curso'; }
  get nombreDocente(): string {
    const d = this.docentes.find(x => x.id === this.docenteId);
    return d ? `${d.nombre} ${d.apellido}` : '';
  }
  get bloqueActual(): HorarioDTO | undefined { return this.horarios.find(h => h.id === this.horarioId); }
  get etiquetaBloque(): string {
    const h = this.bloqueActual;
    return h ? `${h.horaInicio.slice(0, 5)} – ${h.horaFin.slice(0, 5)}` : 'Sin bloque';
  }
  get porcentajeAsistencia(): number {
    if (!this.filas.length) return 0;
    return Math.round(((this.contar('PRESENTE') + this.contar('TARDE')) * 100) / this.filas.length);
  }
  get filasVisibles(): FilaAsistencia[] {
    return this.filtro ? this.filas.filter(f => f.estadoAsistencia === this.filtro) : this.filas;
  }
  get actual(): FilaAsistencia | undefined { return this.filas[this.indiceActual]; }
  get rangoExport(): { desde: string; hasta: string } { return rangoDelPeriodo(this.periodoExport, this.referenciaExport); }
  get descripcionExport(): string {
    const r = this.rangoExport;
    return describirPeriodo(this.periodoExport, r.desde, r.hasta);
  }

  etiqueta(estado: EstadoAsistencia): string { return ETIQUETAS[estado]; }
  iniciales(fila: FilaAsistencia): string {
    return `${fila.nombre?.trim()[0] ?? ''}${fila.apellido?.trim()[0] ?? ''}`.toUpperCase();
  }
  numero(fila: FilaAsistencia): number { return this.filas.indexOf(fila) + 1; }

  // --- Inicio y cierre del llamado, siempre con confirmación ---
  pedirInicio(): void {
    if (this.soloLectura || this.fechaFutura || !this.horarioId || !this.filas.length) return;
    this.confirmacion = 'abrir';
  }

  pedirCierre(): void {
    const invalida = this.filas.find(fila => fila.estadoAsistencia === 'JUSTIFICADO' && !fila.justificacion.trim());
    if (invalida) {
      this.error = `Escribe la justificación de ${invalida.nombre} ${invalida.apellido} antes de cerrar.`;
      return;
    }
    this.confirmacion = 'cerrar';
  }

  confirmar(): void {
    const accion = this.confirmacion;
    this.confirmacion = null;
    if (accion === 'cerrar') { this.guardar(true); return; }
    if (accion !== 'abrir' || !this.horarioId || !this.seccionId || !this.cursoId || !this.docenteId) return;
    this.abriendo = true;
    this.error = '';
    this.notas.abrirSesion({ horarioId: this.horarioId, seccionId: this.seccionId, cursoId: this.cursoId,
      docenteId: this.docenteId, fecha: this.fecha }).subscribe({
      next: sesion => {
        this.sesion = sesion;
        this.abriendo = false;
        this.indiceActual = 0;
        this.filtro = null;
        this.mensaje = 'Llamado iniciado: todos empiezan como presentes, marca solo a quien faltó o llegó tarde.';
        if (this.modo === 'uno') this.leerNombre();
        this.cdr.markForCheck();
      },
      error: error => {
        this.abriendo = false;
        this.error = error.error?.message || 'No se pudo iniciar el llamado de asistencia.';
        this.cdr.markForCheck();
      }
    });
  }

  // --- Marcado ---
  marcar(fila: FilaAsistencia, estado: EstadoAsistencia): void {
    if (this.bloqueado) return;
    fila.estadoAsistencia = estado;
    if (estado !== 'JUSTIFICADO') fila.justificacion = '';
    this.filas = [...this.filas];
    this.cdr.markForCheck();
  }

  ciclar(fila: FilaAsistencia): void {
    const orden = this.estados;
    this.marcar(fila, orden[(orden.indexOf(fila.estadoAsistencia) + 1) % orden.length]);
  }

  alternarFiltro(estado: EstadoAsistencia): void { this.filtro = this.filtro === estado ? null : estado; }

  cambiarModo(modo: 'lista' | 'uno'): void {
    this.modo = modo;
    this.filtro = null;
    if (modo === 'uno') {
      this.leerNombre();
      setTimeout(() => document.querySelector('.uno-por-uno')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 60);
    }
  }

  marcarActual(estado: EstadoAsistencia): void {
    const fila = this.actual;
    if (!fila) return;
    this.marcar(fila, estado);
    // La justificada espera el motivo; las demás pasan al siguiente estudiante
    if (estado !== 'JUSTIFICADO') setTimeout(() => this.siguiente(), 180);
  }

  siguiente(): void { this.irA(this.indiceActual + 1); }
  anterior(): void { this.irA(this.indiceActual - 1); }

  private irA(indice: number): void {
    if (indice < 0 || indice >= this.filas.length) return;
    this.indiceActual = indice;
    this.leerNombre();
    this.cdr.markForCheck();
  }

  leerNombre(): void {
    const fila = this.actual;
    if (!this.leerEnVoz || !fila || typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const voz = new SpeechSynthesisUtterance(`${fila.nombre} ${fila.apellido}`);
    voz.lang = 'es-PE';
    voz.rate = 0.95;
    window.speechSynthesis.speak(voz);
  }

  @HostListener('document:keydown', ['$event'])
  atajos(evento: KeyboardEvent): void {
    if (evento.key === 'Escape') { this.confirmacion = null; this.exportarAbierto = false; this.cdr.markForCheck(); return; }
    const objetivo = evento.target as HTMLElement | null;
    if (this.vistaActual !== 'gestor' || this.modo !== 'uno' || this.estadoLlamado !== 'EN_CURSO' || this.confirmacion
      || this.exportarAbierto || objetivo?.closest('input, select, textarea')) return;
    const teclas: Record<string, EstadoAsistencia> = { p: 'PRESENTE', t: 'TARDE', f: 'FALTA', j: 'JUSTIFICADO' };
    const estado = teclas[evento.key.toLowerCase()];
    if (estado) { evento.preventDefault(); this.marcarActual(estado); }
    else if (evento.key === 'ArrowRight') { evento.preventDefault(); this.siguiente(); }
    else if (evento.key === 'ArrowLeft') { evento.preventDefault(); this.anterior(); }
  }

  // --- Exportación PDF / Excel ---
  abrirExportar(): void {
    this.referenciaExport = this.fecha;
    this.errorExport = '';
    this.exportarAbierto = true;
  }

  exportar(formato: 'PDF' | 'EXCEL'): void {
    if (!this.seccionId || this.exportando) return;
    const { desde, hasta } = this.rangoExport;
    this.exportando = formato;
    this.errorExport = '';
    this.notas.getAsistenciasPorRango(this.seccionId, desde, hasta, this.cursoId).subscribe({
      next: registros => {
        this.exportando = null;
        if (!registros.length) {
          this.errorExport = `No hay asistencia registrada de ${this.nombreCurso} en ${this.descripcionExport}.`;
          this.cdr.markForCheck();
          return;
        }
        const horas = new Map(this.horarios.map(h => [h.id, h.horaInicio.slice(0, 5)]));
        const alumnos = this.filas.length ? this.filas : this.estudiantes
          .filter(e => e.seccionId === this.seccionId && e.id != null).map(e => ({ ...e, id: e.id! }));
        const matriz = construirMatriz(registros, alumnos, id => horas.get(id) ?? '');
        const contexto = { periodo: this.periodoExport, desde, hasta, curso: this.nombreCurso, seccion: this.nombreSeccion,
          turno: this.turnoSeccion.replace('Turno ', ''), docente: this.nombreDocente,
          generadoPor: this.auth.currentUser()?.username ?? 'usuario' };
        const archivo = `asistencia-${this.nombreCurso}-${this.nombreSeccion}-${desde}${desde === hasta ? '' : '_' + hasta}`
          .toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9_-]+/g, '-');
        if (formato === 'PDF') descargarPdf(archivo, reportePdf(matriz, contexto));
        else descargarXlsx(`${archivo}.xlsx`, reporteXlsx(matriz, contexto));
        this.mensaje = `Reporte ${formato === 'PDF' ? 'PDF' : 'Excel'} descargado: ${this.descripcionExport}.`;
        this.exportarAbierto = false;
        this.cdr.markForCheck();
      },
      error: error => {
        this.exportando = null;
        this.errorExport = error.error?.message || 'No se pudo generar el reporte.';
        this.cdr.markForCheck();
      }
    });
  }
  get soloLectura(): boolean { return this.auth.hasAnyRole('DIRECTOR', 'SUBDIRECTOR'); }
  get esDocenteRestringido(): boolean {
    return this.auth.hasRole('DOCENTE')
      && !this.auth.hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA');
  }
  get cursosDisponibles(): CursoHorario[] {
    const ids = new Set(this.horarios.map(item => item.cursoId));
    return this.cursos.filter(item => ids.has(item.id));
  }
  get horariosDisponibles(): HorarioDTO[] {
    const dia = this.diaSeleccionado();
    const delCurso = this.horarios.filter(item => item.cursoId === Number(this.cursoId));
    const delDia = delCurso.filter(item => item.diaSemana === dia);
    return delDia.length ? delDia : delCurso;
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
    this.seccionId = seccion.id;
    if (this.esDocenteRestringido) {
      this.vistaActual = 'gestor';
      this.cambiarSeccion();
    } else {
      this.generarCardsParaSeccion();
      this.vistaActual = 'cursos';
    }
  }
  
  navegarACurso(card: CursoCard): void {
    this.seccionId = card.seccionId;
    this.cursoId = card.cursoId;
    this.vistaActual = 'gestor';
    this.cambiarSeccion();
  }

  volverAGrados(): void {
    this.vistaActual = 'grados';
    this.gradoNavegacion = null;
    this.seccionId = null;
  }

  volverASecciones(): void {
    this.vistaActual = 'secciones';
    this.seccionId = null;
  }
  
  volverACursos(): void {
    if (this.esDocenteRestringido) {
      this.vistaActual = 'cursos';
      this.seccionId = null;
      this.cursoId = null;
    } else {
      this.vistaActual = 'cursos';
      this.cursoId = null;
    }
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
  
  private generarCardsParaSeccion(): void {
    if (!this.seccionId) return;
    // Agrupamos asignaciones unicas por curso en esta seccion
    const unicos = new Map<number, AsignacionAcademica>();
    this.asignaciones.filter(a => a.seccionId === this.seccionId).forEach(a => {
      if (!unicos.has(a.cursoId)) unicos.set(a.cursoId, a);
    });
    
    this.misCursos = Array.from(unicos.values()).map(a => {
      const c = this.cursos.find(x => x.id === a.cursoId);
      const s = this.secciones.find(x => x.id === a.seccionId);
      const d = this.docentes.find(x => x.id === a.docenteId);
      return {
        cursoId: a.cursoId,
        seccionId: a.seccionId,
        cursoNombre: c?.nombre || 'Curso Desconocido',
        seccionNombre: s?.nombre || '',
        gradoNombre: s?.gradoNombre || '',
        docenteNombre: d ? `${d.nombre} ${d.apellido}` : 'Sin asignar'
      };
    });
  }
  
  private generarCardsMisCursos(): void {
    const unicos = new Map<string, AsignacionAcademica>();
    this.asignaciones.forEach(a => {
      const key = `${a.cursoId}-${a.seccionId}`;
      if (!unicos.has(key)) unicos.set(key, a);
    });
    
    this.misCursos = Array.from(unicos.values()).map(a => {
      const c = this.cursos.find(x => x.id === a.cursoId);
      const s = this.secciones.find(x => x.id === a.seccionId);
      return {
        cursoId: a.cursoId,
        seccionId: a.seccionId,
        cursoNombre: c?.nombre || 'Curso Desconocido',
        seccionNombre: s?.nombre || '',
        gradoNombre: s?.gradoNombre || '',
        docenteNombre: 'Docente asignado'
      };
    });
  }

  // ----------------

  ngOnInit(): void {
    const anio = new Date().getFullYear();
    forkJoin({
      secciones: this.horariosService.listarSecciones(),
      cursos: this.horariosService.listarCursos(),
      docentes: this.horariosService.listarDocentes(),
      estudiantes: this.estudiantesService.getEstudiantes(),
      asignaciones: this.esDocenteRestringido
        ? this.horariosService.listarMisAsignaciones(anio)
        : this.horariosService.listarAsignaciones(anio).pipe(catchError(() => of([]))),
      horariosPropios: this.esDocenteRestringido
        ? this.horariosService.listarMisHorarios()
        : of([] as HorarioDTO[])
    }).subscribe({
      next: data => {
        this.asignaciones = data.asignaciones || [];
        this.horariosPropios = data.horariosPropios || [];
        const idsSeccion = new Set(this.asignaciones.map(item => item.seccionId));
        const idsCurso = new Set(this.asignaciones.map(item => item.cursoId));
        
        this.secciones = data.secciones.filter(item => item.activa !== false && item.nivelNombre === 'SECUNDARIA'
          && (!this.esDocenteRestringido || idsSeccion.has(item.id)));
        this.cursos = data.cursos.filter(item => item.estado === 'ACTIVO'
          && (!this.esDocenteRestringido || idsCurso.has(item.id)));
        this.docentes = data.docentes.filter(item => item.estado === 'ACTIVO');
        this.estudiantes = data.estudiantes;
        
        if (this.esDocenteRestringido) {
          this.generarCardsMisCursos();
          this.vistaActual = 'cursos';
        } else {
          this.vistaActual = 'grados';
        }
        
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => this.fallar('No se pudieron cargar los catálogos de asistencia.')
    });
  }

  cambiarSeccion(): void {
    if (!this.seccionId) return;
    this.loading = true;
    if (this.esDocenteRestringido) {
      this.procesarHorarios(this.horariosPropios.filter(item => item.seccionId === this.seccionId));
      return;
    }
    this.horariosService.listarPorSeccion(this.seccionId).subscribe({
      next: horarios => this.procesarHorarios(horarios),
      error: () => this.fallar('No se pudo cargar el horario de la sección.')
    });
  }

  cambiarContexto(): void {
    this.seleccionarHorarioCompatible();
    this.cargarRegistros();
  }

  marcarTodos(estado: FilaAsistencia['estadoAsistencia']): void {
    if (this.bloqueado) return;
    this.filas = this.filas.map(fila => ({ ...fila, estadoAsistencia: estado, justificacion: estado === 'JUSTIFICADO' ? fila.justificacion : '' }));
    this.cdr.markForCheck();
  }

  guardar(cerrar = false): void {
    if (this.soloLectura) {
      this.error = 'Esta vista es de supervisión. El registro corresponde al docente, auxiliar o convivencia.';
      return;
    }
    if (!this.horarioId || !this.seccionId || !this.cursoId || !this.docenteId || !this.filas.length || this.cerrada) {
      this.error = 'Selecciona una sesión válida con estudiantes antes de guardar.';
      return;
    }
    const invalida = this.filas.find(fila => fila.estadoAsistencia === 'JUSTIFICADO' && !fila.justificacion.trim());
    if (invalida) {
      this.error = `Escribe la justificación de ${invalida.nombre} ${invalida.apellido}.`;
      return;
    }
    this.saving = true;
    this.error = '';
    this.mensaje = '';
    const payload: Asistencia[] = this.filas.map(fila => ({
      estudianteId: fila.id,
      horarioId: this.horarioId!,
      docenteId: this.docenteId!,
      seccionId: this.seccionId!,
      cursoId: this.cursoId!,
      fecha: this.fecha,
      estado: fila.estadoAsistencia,
      justificacion: fila.justificacion || undefined
    }));
    this.notas.registrarAsistenciaMasiva(payload).pipe(
      switchMap(() => this.notas.obtenerSesion(this.horarioId!, this.fecha)),
      switchMap(sesion => cerrar && sesion.id ? this.notas.cerrarSesion(sesion.id) : of(sesion))
    ).subscribe({
      next: sesion => {
        this.sesion = sesion;
        this.saving = false;
        this.mensaje = cerrar ? 'Llamado cerrado: la asistencia quedó consolidada.' : 'Avance guardado. Puedes seguir marcando.';
        this.cdr.markForCheck();
      },
      error: error => {
        this.saving = false;
        this.error = error.error?.message || error.error || 'No se pudo guardar la asistencia.';
        this.cdr.markForCheck();
      }
    });
  }

  contar(estado: string): number { return this.filas.filter(item => item.estadoAsistencia === estado).length; }

  cargarRegistros(): void {
    this.error = '';
    this.mensaje = '';
    if (!this.seccionId || !this.horarioId) {
      this.filas = [];
      this.sesion = null;
      this.loading = false;
      this.error = 'No existe un bloque de horario para el curso y fecha seleccionados.';
      this.cdr.markForCheck();
      return;
    }
    forkJoin({
      registros: this.notas.getAsistenciasPorFecha(this.seccionId, this.fecha),
      sesion: this.notas.obtenerSesion(this.horarioId, this.fecha).pipe(catchError(() => of(null)))
    }).subscribe({
      next: data => {
        this.indiceActual = 0;
        this.filtro = null;
        const registrados = new Map(data.registros.filter(item => item.horarioId === this.horarioId).map(item => [item.estudianteId, item]));
        this.filas = this.estudiantes.filter(item => item.seccionId === this.seccionId && item.id != null && (item.estado ?? 'ACTIVO') === 'ACTIVO').map(item => {
          const registro = registrados.get(item.id!);
          return { ...item, id: item.id!, estadoAsistencia: (registro?.estado as FilaAsistencia['estadoAsistencia']) || 'PRESENTE', justificacion: registro?.justificacion || '' };
        });
        this.sesion = data.sesion;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => this.fallar('No se pudieron cargar los registros de asistencia.')
    });
  }

  private procesarHorarios(horarios: HorarioDTO[]): void {
    this.horarios = horarios;
    if (!this.cursoId) {
      this.cursoId = this.cursosDisponibles[0]?.id ?? null;
    }
    this.seleccionarHorarioCompatible();
    this.cargarRegistros();
  }

  private seleccionarHorarioCompatible(): void {
    const horario = this.horariosDisponibles[0];
    this.horarioId = horario?.id ?? null;
    this.docenteId = horario?.docenteId ?? null;
  }
  private diaSeleccionado(): string {
    const dias = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];
    return dias[new Date(`${this.fecha}T12:00:00`).getDay()];
  }
  private fechaLocal(): string {
    const hoy = new Date();
    const offset = hoy.getTimezoneOffset() * 60000;
    return new Date(hoy.getTime() - offset).toISOString().slice(0, 10);
  }
  private fallar(mensaje: string): void { this.loading = false; this.error = mensaje; this.cdr.markForCheck(); }
}
