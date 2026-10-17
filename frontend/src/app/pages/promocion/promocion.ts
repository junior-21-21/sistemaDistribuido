import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, catchError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { AreaPendiente, Estudiante, EstudianteService, SituacionSugerida } from '../../services/estudiante.service';
import { CursoHorario, HorarioService, SeccionHorario } from '../../services/horario.service';

type Situacion = 'PROMOVIDO' | 'PERMANECE' | 'RECUPERACION';

interface Fila {
  estudiante: Estudiante;
  situacion: Situacion;
  /** Sugerencia calculada con las notas y la norma del MINEDU. */
  sugerida: SituacionSugerida['situacion'];
  motivo: string;
  /** Áreas que irá a recuperar (solo si la situación es RECUPERACION). */
  areas: Set<string>;
  /** Áreas con alguna competencia en C (sugeridas para recuperación). */
  areasConC: string[];
}

/** Estudiante con áreas en recuperación (enero-febrero) agrupadas. */
interface CasoRecuperacion {
  estudianteId: number;
  estudiante: string;
  gradoId?: number;
  seccionId?: number;
  anioOrigen: number;
  gradoOrigen: string;
  areas: AreaPendiente[];
}

/**
 * Cierre del año escolar según la RVM 094-2020-MINEDU (modificada por la RVM 048-2024-MINEDU):
 * 1) el sistema sugiere la situación final con las notas; Secretaría o Dirección la confirman;
 * 2) quienes van a recuperación registran su resultado y se cierra su caso (pasa, pasa con un
 *    área por subsanar hasta diciembre, o permanece); 3) se hace el seguimiento de las áreas por subsanar.
 */
@Component({
  selector: 'app-promocion',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './promocion.html',
  styleUrl: './promocion.css'
})
export class Promocion implements OnInit {
  private readonly horarioService = inject(HorarioService);
  private readonly estudianteService = inject(EstudianteService);
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly anio = new Date().getFullYear();
  readonly puedePromover = this.auth.hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR');
  readonly puedeCalificar = this.auth.hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR');

  readonly pestana = signal<'promocion' | 'recuperacion' | 'subsanacion'>(
    this.auth.hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR') ? 'promocion' : 'subsanacion');
  readonly secciones = signal<SeccionHorario[]>([]);
  readonly cursos = signal<CursoHorario[]>([]);
  readonly seccionOrigen = signal<number | null>(null);
  readonly seccionDestino = signal<number | null>(null);
  readonly filas = signal<Fila[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly sinNotas = signal(false);
  readonly areas = signal<AreaPendiente[]>([]);
  readonly filtroSubsanacion = signal<'PENDIENTE' | ''>('PENDIENTE');
  readonly error = signal('');
  /** Sección elegida para cada caso de recuperación al cerrarlo. */
  readonly destinoCaso = signal<Record<number, number | null>>({});
  readonly cerrando = signal<number | null>(null);

  readonly origen = computed(() => this.secciones().find(s => s.id === this.seccionOrigen()) ?? null);

  /** Grados del nivel en orden (para saber el número de grado y el siguiente). */
  private readonly gradosDelNivel = computed(() => {
    const nivel = this.origen()?.nivelNombre;
    return [...new Map(this.secciones().filter(s => !nivel || s.nivelNombre === nivel)
      .map(s => [s.gradoId, s.gradoNombre])).entries()].sort((a, b) => a[0] - b[0]);
  });

  readonly numeroGrado = computed(() => {
    const origen = this.origen();
    return origen ? this.gradosDelNivel().findIndex(([id]) => id === origen.gradoId) + 1 : 0;
  });

  /** Secciones del grado siguiente (mismo nivel), destino natural de los promovidos. */
  readonly destinos = computed(() => {
    const origen = this.origen();
    return origen ? this.seccionesSiguientes(origen.gradoId) : [];
  });

  readonly esUltimoGrado = computed(() => !!this.origen() && this.destinos().length === 0);
  readonly cursosDelGrado = computed(() => this.cursos().filter(c => c.gradoId === this.origen()?.gradoId).map(c => c.nombre));

  readonly resumen = computed(() => {
    const f = this.filas();
    return {
      promovidos: f.filter(x => x.situacion === 'PROMOVIDO').length,
      permanecen: f.filter(x => x.situacion === 'PERMANECE').length,
      recuperacion: f.filter(x => x.situacion === 'RECUPERACION').length,
      areas: f.filter(x => x.situacion === 'RECUPERACION').reduce((t, x) => t + x.areas.size, 0),
      cambiados: f.filter(x => x.sugerida !== 'SIN_NOTAS' && x.situacion !== x.sugerida).length
    };
  });

  readonly casos = computed<CasoRecuperacion[]>(() => {
    const mapa = new Map<string, CasoRecuperacion>();
    // Solo casos abiertos: la matrícula del año de origen sigue en RECUPERACION
    for (const a of this.areas().filter(x => x.tipo === 'RECUPERACION' && x.situacionOrigen === 'RECUPERACION')) {
      const clave = `${a.estudianteId}-${a.anioOrigen}`;
      const caso = mapa.get(clave) ?? {
        estudianteId: a.estudianteId, estudiante: a.estudiante, gradoId: a.gradoId, seccionId: a.seccionId,
        anioOrigen: a.anioOrigen, gradoOrigen: a.gradoOrigen, areas: []
      };
      caso.areas.push(a);
      mapa.set(clave, caso);
    }
    return [...mapa.values()].sort((a, b) => a.estudiante.localeCompare(b.estudiante, 'es'));
  });

  readonly subsanaciones = computed(() => this.areas().filter(a => a.tipo === 'SUBSANACION'
    && (!this.filtroSubsanacion() || a.estado === this.filtroSubsanacion())));

  readonly pendientesRecuperacion = computed(() => this.casos().length);
  readonly pendientesSubsanacion = computed(() => this.areas().filter(a => a.tipo === 'SUBSANACION' && a.estado === 'PENDIENTE').length);

  ngOnInit(): void {
    forkJoin({ secciones: this.horarioService.listarSecciones(), cursos: this.horarioService.listarCursos() }).subscribe({
      next: ({ secciones, cursos }) => {
        this.secciones.set(secciones.filter(s => s.activa !== false)
          .sort((a, b) => a.gradoId - b.gradoId || a.nombre.localeCompare(b.nombre)));
        this.cursos.set(cursos);
      },
      error: () => this.error.set('No se pudieron cargar las secciones.')
    });
    this.cargarAreas();
    this.realtime.alCambiar('recuperacion', 'estudiantes').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cargarAreas());
  }

  private seccionesSiguientes(gradoId: number): SeccionHorario[] {
    const nivel = this.secciones().find(s => s.gradoId === gradoId)?.nivelNombre;
    const grados = [...new Map(this.secciones().filter(s => s.nivelNombre === nivel)
      .map(s => [s.gradoId, s.gradoNombre])).entries()].sort((a, b) => a[0] - b[0]);
    const indice = grados.findIndex(([id]) => id === gradoId);
    const siguiente = grados[indice + 1]?.[0];
    return this.secciones().filter(s => s.gradoId === siguiente);
  }

  // ------------------------------------------------------------------ paso 1: situación final

  elegirOrigen(id: number): void {
    this.seccionOrigen.set(id);
    this.seccionDestino.set(this.destinos()[0]?.id ?? null);
    this.cargando.set(true);
    this.error.set('');
    const nombreCurso = (cursoId: number) => this.cursos().find(c => c.id === cursoId)?.nombre ?? `Área ${cursoId}`;
    forkJoin({
      estudiantes: this.estudianteService.getEstudiantes(),
      sugerencias: this.estudianteService.situacionSugerida(id, this.numeroGrado() || 1, this.anio)
        .pipe(catchError(() => of([] as SituacionSugerida[])))
    }).subscribe({
      next: ({ estudiantes, sugerencias }) => {
        const porEstudiante = new Map(sugerencias.map(s => [s.estudianteId, s]));
        this.sinNotas.set(sugerencias.length === 0);
        this.filas.set(estudiantes
          .filter(e => e.seccionId === id && (e.estado ?? 'ACTIVO') === 'ACTIVO')
          .sort((a, b) => a.apellido.localeCompare(b.apellido, 'es'))
          .map(estudiante => {
            const s = porEstudiante.get(estudiante.id!);
            const sugerida = s?.situacion ?? 'SIN_NOTAS';
            const areasConC = (s?.areasConC ?? []).map(nombreCurso);
            const situacion: Situacion = sugerida === 'SIN_NOTAS' ? 'PROMOVIDO' : sugerida;
            return {
              estudiante, situacion, sugerida, areasConC,
              motivo: s?.motivo ?? 'Sin notas registradas: decide según el acta',
              areas: new Set(situacion === 'RECUPERACION' ? areasConC : [])
            };
          }));
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudieron cargar los estudiantes.');
      }
    });
  }

  cambiarSituacion(fila: Fila, situacion: Situacion): void {
    fila.situacion = situacion;
    if (situacion === 'RECUPERACION' && fila.areas.size === 0) fila.areasConC.forEach(a => fila.areas.add(a));
    this.filas.update(lista => [...lista]);
  }

  alternarArea(fila: Fila, area: string): void {
    fila.areas.has(area) ? fila.areas.delete(area) : fila.areas.add(area);
    this.filas.update(lista => [...lista]);
  }

  /** Vuelve a la sugerencia calculada para todos. */
  usarSugerencias(): void {
    this.filas.update(lista => lista.map(f => {
      const situacion: Situacion = f.sugerida === 'SIN_NOTAS' ? f.situacion : f.sugerida;
      return { ...f, situacion, areas: new Set(situacion === 'RECUPERACION' ? f.areasConC : []) };
    }));
  }

  etiqueta(situacion: string): string {
    return { PROMOVIDO: this.esUltimoGrado() ? 'Egresa' : 'Promovido', RECUPERACION: 'Recuperación',
      PERMANECE: 'Permanece', SIN_NOTAS: 'Sin notas' }[situacion] ?? situacion;
  }

  confirmar(): void {
    const origen = this.origen();
    if (!origen) return;
    const destino = this.destinos().find(s => s.id === Number(this.seccionDestino()));
    if (this.resumen().promovidos > 0 && !destino && !this.esUltimoGrado()) {
      this.error.set('Elige la sección de destino para los promovidos.');
      return;
    }
    const sinAreas = this.filas().find(f => f.situacion === 'RECUPERACION' && f.areas.size === 0);
    if (sinAreas) {
      this.error.set(`Marca las áreas que ${sinAreas.estudiante.nombre} debe recuperar.`);
      return;
    }
    const r = this.resumen();
    const texto = `Se registrará el cierre de ${origen.gradoNombre} ${origen.nombre}: ${r.promovidos} ${this.esUltimoGrado() ? 'egresan' : 'promovidos'}, `
      + `${r.recuperacion} van a recuperación y ${r.permanecen} permanecen.`
      + (r.cambiados ? `\n\nOjo: cambiaste ${r.cambiados} situación(es) respecto a lo que indican las notas.` : '') + '\n\n¿Confirmar?';
    if (!confirm(texto)) return;
    this.guardando.set(true);
    this.error.set('');
    this.estudianteService.promover({
      anioOrigen: this.anio,
      gradoOrigen: `${origen.gradoNombre ?? ''}`.trim(),
      decisiones: this.filas().map(f => ({
        estudianteId: f.estudiante.id!,
        situacion: this.esUltimoGrado() && f.situacion === 'PROMOVIDO' ? 'EGRESADO' : f.situacion,
        gradoDestinoId: f.situacion === 'PROMOVIDO' ? destino?.gradoId ?? null : null,
        seccionDestinoId: f.situacion === 'PROMOVIDO' ? destino?.id ?? null : null,
        areasPendientes: [],
        areasRecuperacion: f.situacion === 'RECUPERACION' ? [...f.areas] : []
      }))
    }).subscribe({
      next: res => {
        this.guardando.set(false);
        this.toasts.exito('Cierre registrado', `${res.promovidos} promovidos · ${res.permanecen} permanecen · ${res.enRecuperacion} en recuperación`);
        this.filas.set([]);
        this.seccionOrigen.set(null);
        this.cargarAreas();
        if (res.enRecuperacion > 0) this.pestana.set('recuperacion');
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.error.set(e.error?.message || 'No se pudo registrar el cierre.');
      }
    });
  }

  // ------------------------------------------------------------------ paso 2: recuperación

  cargarAreas(): void {
    this.http.get<AreaPendiente[]>('/api/recuperacion').subscribe({
      next: lista => this.areas.set(lista),
      error: () => this.areas.set([])
    });
  }

  registrarResultado(area: AreaPendiente, estado: 'SUBSANADA' | 'NO_SUBSANADA', calificacion: string): void {
    this.http.put<AreaPendiente>(`/api/recuperacion/${area.id}`, { estado, calificacion, observacion: area.observacion ?? '' }).subscribe({
      next: () => this.cargarAreas(),
      error: (e: HttpErrorResponse) => this.error.set(e.error?.message || 'No se pudo registrar el resultado.')
    });
  }

  /** Lo que dice la norma con los resultados registrados hasta ahora. */
  pronostico(caso: CasoRecuperacion): { texto: string; clase: string; listo: boolean } {
    const pendientes = caso.areas.filter(a => a.estado === 'PENDIENTE').length;
    const noRecuperadas = caso.areas.filter(a => a.estado === 'NO_SUBSANADA');
    if (pendientes) return { texto: `Faltan ${pendientes} resultado(s)`, clase: 'espera', listo: false };
    if (noRecuperadas.length === 0) return { texto: this.ultimoGradoCaso(caso) ? 'Egresa' : 'Pasa de grado', clase: 'pasa', listo: true };
    if (noRecuperadas.length === 1) {
      return { texto: `${this.ultimoGradoCaso(caso) ? 'Egresa' : 'Pasa de grado'} y subsana ${noRecuperadas[0].cursoNombre} hasta diciembre`, clase: 'pasa-con', listo: true };
    }
    return { texto: `Permanece en el grado (${noRecuperadas.length} áreas sin recuperar)`, clase: 'permanece', listo: true };
  }

  private ultimoGradoCaso(caso: CasoRecuperacion): boolean {
    return !!caso.gradoId && this.seccionesSiguientes(caso.gradoId).length === 0;
  }

  /** Secciones posibles al cerrar: del grado siguiente si pasa, del mismo grado si permanece. */
  opcionesCaso(caso: CasoRecuperacion): SeccionHorario[] {
    if (!caso.gradoId) return [];
    const permanece = caso.areas.filter(a => a.estado === 'NO_SUBSANADA').length >= 2;
    return permanece ? this.secciones().filter(s => s.gradoId === caso.gradoId) : this.seccionesSiguientes(caso.gradoId);
  }

  seccionDelCaso(caso: CasoRecuperacion): number | null {
    const elegida = this.destinoCaso()[caso.estudianteId];
    const opciones = this.opcionesCaso(caso);
    return elegida && opciones.some(o => o.id === elegida) ? elegida
      : (opciones.find(o => o.id === caso.seccionId)?.id ?? opciones[0]?.id ?? null);
  }

  elegirDestinoCaso(caso: CasoRecuperacion, id: number): void {
    this.destinoCaso.update(m => ({ ...m, [caso.estudianteId]: Number(id) }));
  }

  cerrarCaso(caso: CasoRecuperacion): void {
    const pronostico = this.pronostico(caso);
    if (!pronostico.listo) return;
    const ultimo = this.ultimoGradoCaso(caso);
    const seccion = this.secciones().find(s => s.id === this.seccionDelCaso(caso));
    const permanece = pronostico.clase === 'permanece';
    if (!ultimo && !seccion && !permanece) {
      this.error.set('Elige la sección de destino.');
      return;
    }
    if (!confirm(`${caso.estudiante}: ${pronostico.texto}. ¿Cerrar su recuperación?`)) return;
    this.cerrando.set(caso.estudianteId);
    this.estudianteService.cerrarRecuperacion(caso.estudianteId, {
      anioOrigen: caso.anioOrigen,
      gradoDestinoId: permanece ? null : seccion?.gradoId ?? null,
      seccionDestinoId: permanece ? null : seccion?.id ?? null,
      seccionPermanenciaId: permanece ? seccion?.id ?? null : null,
      ultimoGrado: ultimo
    }).subscribe({
      next: r => {
        this.cerrando.set(null);
        this.toasts.exito('Recuperación cerrada', `${caso.estudiante}: ${r.detalle}`);
        this.cargarAreas();
      },
      error: (e: HttpErrorResponse) => {
        this.cerrando.set(null);
        this.error.set(e.error?.message || 'No se pudo cerrar la recuperación.');
      }
    });
  }

  nombreSeccion(id?: number | null): string {
    const s = this.secciones().find(x => x.id === id);
    return s ? `${s.gradoNombre} ${s.nombre}` : '';
  }
}
