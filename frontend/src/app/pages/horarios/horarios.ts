import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, catchError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { LiveBanner } from '../../shared/live-banner/live-banner';
import {
  AsignacionAcademica,
  AulaHorario,
  CursoHorario,
  DocenteHorario,
  HorarioDTO,
  HorarioService,
  RepartoResultado,
  SeccionHorario
} from '../../services/horario.service';
import {
  BloqueTiempo,
  DIAS_SEMANA,
  DIA_ETIQUETA,
  ParametrosHorario,
  construirBloques,
  generarHorario,
  horaFin,
  parametrosPorDefecto,
  sanearParametros
} from './horario-generador';

interface Clase {
  asignacionId: number;
  cursoId: number;
  curso: string;
  docenteId: number;
  docente: string;
  aulaId: number;
  aula: string;
  fijada?: boolean;
}

/** dia -> lista indexada por período */
type Matriz = Record<string, (Clase | null)[]>;

interface Carga {
  asignacionId: number;
  cursoId: number;
  curso: string;
  docenteId: number;
  docente: string;
  periodos: number;
}

interface Arrastre {
  origen: 'celda' | 'pendiente';
  seccionId: number;
  dia?: string;
  periodo?: number;
  carga: Carga;
}

type Vista = 'seccion' | 'docente';
type Alcance = 'TURNO' | 'GRADO' | 'SECCION';

@Component({
  selector: 'app-horarios',
  standalone: true,
  imports: [LiveBanner, CommonModule, FormsModule],
  templateUrl: './horarios.html',
  styleUrl: './horarios.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Horarios implements OnInit {
  private readonly horarioService = inject(HorarioService);
  private readonly authService = inject(AuthService);
  private readonly toasts = inject(ToastService);

  readonly anioEscolar = new Date().getFullYear();
  readonly diasDisponibles = DIAS_SEMANA;

  etiquetaDia(dia: string): string {
    return DIA_ETIQUETA[dia] ?? dia;
  }
  /** Solo Coordinación Académica publica horarios; Dirección y Secretaría los consultan. */
  readonly soloLectura = !this.authService.hasAnyRole('DIRECTOR', 'SUBDIRECTOR');

  // Catálogos
  readonly cargando = signal(true);
  readonly docentes = signal<DocenteHorario[]>([]);
  readonly cursos = signal<CursoHorario[]>([]);
  readonly aulas = signal<AulaHorario[]>([]);
  readonly secciones = signal<SeccionHorario[]>([]);
  readonly asignaciones = signal<AsignacionAcademica[]>([]);
  private horariosGuardados: HorarioDTO[] = [];

  // Parámetros
  readonly turno = signal<'MANANA' | 'TARDE'>('MANANA');
  readonly parametros = signal<ParametrosHorario>(this.leerParametros('MANANA'));
  readonly bloques = computed<BloqueTiempo[]>(() => construirBloques(this.parametros()));
  readonly finJornada = computed(() => horaFin(this.parametros()));
  /** Períodos tras los que puede ir un recreo (1..n-1). */
  readonly numerosRecreo = computed(() => Array.from({ length: Math.max(0, this.parametros().periodosPorDia - 1) }, (_, i) => i + 1));
  readonly alcance = signal<Alcance>('TURNO');
  readonly gradoAlcance = signal<number | null>(null);
  readonly seccionAlcance = signal<number | null>(null);
  private semilla = 1;

  // Horario en edición (secciones del turno actual)
  readonly matrices = signal<Map<number, Matriz>>(new Map());
  readonly cambiosSinGuardar = signal<Set<number>>(new Set());
  private readonly historial: Map<number, Matriz>[] = [];
  readonly puedeDeshacer = signal(false);
  readonly noCoinciden = signal(0);

  // Vista
  readonly vista = signal<Vista>('seccion');
  readonly seccionActiva = signal<number | null>(null);
  readonly docenteActivo = signal<number | null>(null);
  readonly arrastre = signal<Arrastre | null>(null);
  readonly celdaEditando = signal<{ dia: string; periodo: number } | null>(null);
  readonly generando = signal(false);
  readonly guardando = signal(false);
  readonly mensaje = signal<{ tipo: 'ok' | 'error' | 'info'; texto: string } | null>(null);
  readonly panelCargaAbierto = signal(false);

  // Reparto de un curso entre varios docentes
  readonly repartoCurso = signal('');
  readonly repartoSecciones = signal<Set<number>>(new Set());
  readonly repartoDocentes = signal<Set<number>>(new Set());
  readonly repartoHoras = signal<number | null>(null);
  readonly repartoResultado = signal<RepartoResultado | null>(null);
  readonly repartiendo = signal(false);

  /** Nombres de curso disponibles en las secciones del turno (Matemática, Comunicación…). */
  readonly cursosReparto = computed(() => {
    const grados = new Set(this.seccionesTurno().map(s => s.gradoId));
    return [...new Set(this.cursos().filter(c => grados.has(c.gradoId)).map(c => c.nombre))].sort((a, b) => a.localeCompare(b, 'es'));
  });

  /** Secciones del turno que tienen el curso elegido, con su docente y horas actuales. */
  readonly seccionesReparto = computed(() => {
    const nombre = this.repartoCurso();
    if (!nombre) return [];
    return this.seccionesTurno().flatMap(seccion => {
      const curso = this.cursos().find(c => c.gradoId === seccion.gradoId && c.nombre === nombre);
      if (!curso) return [];
      const actual = this.asignaciones().filter(a => a.seccionId === seccion.id && a.cursoId === curso.id);
      return [{
        seccion,
        cursoId: curso.id,
        horas: actual.reduce((t, a) => t + a.horasSemanales, 0),
        docentes: actual.map(a => a.docenteNombre ?? '').filter(Boolean).join(', ')
      }];
    });
  });

  /** Horas que ya tiene cada docente este año (para elegir con criterio). */
  readonly cargaPorDocente = computed(() => {
    const mapa = new Map<number, number>();
    this.asignaciones().forEach(a => mapa.set(a.docenteId, (mapa.get(a.docenteId) ?? 0) + a.horasSemanales));
    return mapa;
  });

  // Formulario de nueva carga
  nuevaCarga = { cursoId: null as number | null, docenteId: null as number | null, periodos: 2 };
  readonly guardandoCarga = signal(false);

  readonly seccionesTurno = computed(() =>
    this.secciones().filter(s => this.normalizarTurno(s.turno) === this.turno()));

  readonly gradosTurno = computed(() => {
    const grados = new Map<number, string>();
    this.seccionesTurno().forEach(s => grados.set(s.gradoId, s.gradoNombre || `Grado ${s.gradoId}`));
    return [...grados].map(([id, nombre]) => ({ id, nombre }));
  });

  readonly aulasTurno = computed(() => this.aulas().filter(aula => {
    const disponibilidad = this.normalizarTurno(aula.turnoDisponible);
    return aula.activa !== false && (disponibilidad === 'AMBOS' || disponibilidad === this.turno());
  }));

  readonly matrizActiva = computed<Matriz | null>(() => {
    const id = this.seccionActiva();
    return id != null ? this.matrices().get(id) ?? null : null;
  });

  readonly seccionActivaInfo = computed(() => this.secciones().find(s => s.id === this.seccionActiva()) ?? null);

  /** Cargas de la sección activa con los períodos que faltan ubicar. */
  readonly pendientes = computed(() => {
    const id = this.seccionActiva();
    const matriz = this.matrizActiva();
    if (id == null) return [];
    return this.cargasDe(id).map(carga => ({
      ...carga,
      ubicados: matriz ? this.contar(matriz, c => c.asignacionId === carga.asignacionId) : 0
    })).map(item => ({ ...item, faltan: Math.max(0, item.periodos - item.ubicados) }));
  });

  readonly progreso = computed(() => {
    const lista = this.pendientes();
    const total = lista.reduce((s, c) => s + c.periodos, 0);
    const ubicados = lista.reduce((s, c) => s + Math.min(c.periodos, c.ubicados), 0);
    return { total, ubicados, porcentaje: total ? Math.round((ubicados / total) * 100) : 0 };
  });

  /** Resumen por sección para las pestañas. */
  readonly resumenSecciones = computed(() => this.seccionesTurno().map(seccion => {
    const cargas = this.cargasDe(seccion.id);
    const total = cargas.reduce((s, c) => s + c.periodos, 0);
    const matriz = this.matrices().get(seccion.id);
    const ubicados = matriz ? this.contar(matriz, () => true) : 0;
    return { seccion, total, ubicados, sinGuardar: this.cambiosSinGuardar().has(seccion.id) };
  }));

  /** Horario de un docente reunido desde todas las secciones del turno. */
  readonly matrizDocente = computed<Record<string, ({ clase: Clase; seccion: string } | null)[]>>(() => {
    const docenteId = this.docenteActivo();
    const resultado: Record<string, ({ clase: Clase; seccion: string } | null)[]> = {};
    const p = this.parametros();
    for (const dia of p.dias) resultado[dia] = Array(p.periodosPorDia).fill(null);
    if (docenteId == null) return resultado;
    for (const [seccionId, matriz] of this.matrices()) {
      const seccion = this.secciones().find(s => s.id === seccionId);
      for (const dia of p.dias) {
        (matriz[dia] ?? []).forEach((clase, periodo) => {
          if (clase?.docenteId === docenteId && periodo < p.periodosPorDia) {
            resultado[dia][periodo] = { clase, seccion: `${seccion?.gradoNombre ?? ''} ${seccion?.nombre ?? ''}`.trim() };
          }
        });
      }
    }
    return resultado;
  });

  readonly docentesConCarga = computed(() => {
    const ids = new Set(this.asignaciones()
      .filter(a => this.seccionesTurno().some(s => s.id === a.seccionId))
      .map(a => a.docenteId));
    return this.docentes().filter(d => ids.has(d.id));
  });

  readonly cursosParaNuevaCarga = computed(() => {
    const seccion = this.secciones().find(s => s.id === Number(this.nuevaCargaSeccion()));
    return seccion ? this.cursos().filter(c => c.gradoId === seccion.gradoId) : [];
  });
  readonly nuevaCargaSeccion = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarCatalogos();
  }

  // ------------------------------------------------------------ parámetros

  cambiarTurno(turno: 'MANANA' | 'TARDE'): void {
    if (turno === this.turno()) return;
    if (this.cambiosSinGuardar().size && !confirm('Hay cambios sin guardar en este turno. ¿Cambiar de turno de todos modos?')) return;
    this.turno.set(turno);
    this.parametros.set(this.leerParametros(turno));
    this.reconstruirDesdeGuardado();
  }

  actualizarParametro<K extends keyof ParametrosHorario>(clave: K, valor: ParametrosHorario[K]): void {
    const nuevos = sanearParametros({ ...this.parametros(), [clave]: valor });
    this.aplicarParametros(nuevos);
  }

  alternarDia(dia: string): void {
    const dias = this.parametros().dias.includes(dia)
      ? this.parametros().dias.filter(d => d !== dia)
      : [...this.parametros().dias, dia];
    if (!dias.length) return;
    this.actualizarParametro('dias', dias);
  }

  agregarRecreo(): void {
    const p = this.parametros();
    const usados = new Set(p.recreos.map(r => r.despuesDe));
    const despuesDe = Array.from({ length: p.periodosPorDia - 1 }, (_, i) => i + 1).find(n => !usados.has(n));
    if (!despuesDe) return;
    this.actualizarParametro('recreos', [...p.recreos, { despuesDe, minutos: 15 }]);
  }

  actualizarRecreo(indice: number, campo: 'despuesDe' | 'minutos', valor: number): void {
    const recreos = this.parametros().recreos.map((r, i) => i === indice ? { ...r, [campo]: Number(valor) } : r);
    this.actualizarParametro('recreos', recreos);
  }

  quitarRecreo(indice: number): void {
    this.actualizarParametro('recreos', this.parametros().recreos.filter((_, i) => i !== indice));
  }

  restablecerParametros(): void {
    this.aplicarParametros(parametrosPorDefecto(this.turno()));
  }

  private aplicarParametros(nuevos: ParametrosHorario): void {
    this.parametros.set(nuevos);
    try {
      localStorage.setItem(`pad_horario_parametros_${this.turno()}`, JSON.stringify(nuevos));
    } catch {
      /* sin almacenamiento: los parámetros valen solo en esta visita */
    }
    // Ajusta las grillas a la nueva cantidad de días/períodos sin perder lo que sigue cabiendo.
    this.matrices.update(actual => {
      const copia = new Map<number, Matriz>();
      for (const [id, matriz] of actual) copia.set(id, this.redimensionar(matriz, nuevos));
      return copia;
    });
  }

  // ------------------------------------------------------------ generación

  generar(nuevaVariante = false): void {
    if (this.soloLectura) return;
    const objetivo = this.seccionesObjetivo();
    if (!objetivo.length) {
      this.avisar('error', 'Elige el grado o la sección que quieres generar.');
      return;
    }
    const sinCarga = objetivo.filter(s => this.cargasDe(s.id).length === 0);
    if (sinCarga.length === objetivo.length) {
      this.avisar('error', 'Esas secciones no tienen carga académica. Agrega cursos y docentes en “Carga académica”.');
      return;
    }
    const aulas = this.aulasTurno();
    if (!aulas.length) {
      this.avisar('error', 'No hay aulas activas disponibles para este turno.');
      return;
    }
    if (nuevaVariante) this.semilla++;
    this.guardarHistorial();
    this.generando.set(true);

    // Se deja respirar a la interfaz para mostrar el indicador antes de calcular.
    setTimeout(() => {
      const p = this.parametros();
      const idsObjetivo = new Set(objetivo.map(s => s.id));
      const docentesOcupados = new Set<string>();
      const aulasOcupadas = new Set<string>();
      const fijadas: { seccionId: number; dia: string; periodo: number; asignacionId: number; aulaId: number }[] = [];

      for (const [seccionId, matriz] of this.matrices()) {
        for (const dia of p.dias) {
          (matriz[dia] ?? []).forEach((clase, periodo) => {
            if (!clase) return;
            if (!idsObjetivo.has(seccionId)) {
              docentesOcupados.add(`${clase.docenteId}|${dia}|${periodo}`);
              aulasOcupadas.add(`${clase.aulaId}|${dia}|${periodo}`);
            } else if (clase.fijada) {
              fijadas.push({ seccionId, dia, periodo, asignacionId: clase.asignacionId, aulaId: clase.aulaId });
            }
          });
        }
      }

      const resultado = generarHorario({
        parametros: p,
        secciones: objetivo.map(s => ({ id: s.id, cargas: this.cargasDe(s.id) })),
        aulas: aulas.map(a => a.id),
        docentesOcupados,
        aulasOcupadas,
        fijadas,
        semilla: this.semilla
      });

      this.matrices.update(actual => {
        const copia = new Map(actual);
        for (const seccion of objetivo) {
          const matriz = this.matrizVacia(p);
          const porDia = resultado.grilla.get(seccion.id);
          const cargas = this.cargasDe(seccion.id);
          porDia?.forEach((periodos, dia) => periodos.forEach((ubicacion, periodo) => {
            const carga = cargas.find(c => c.asignacionId === ubicacion.asignacionId);
            if (carga) {
              const fijada = fijadas.some(f => f.seccionId === seccion.id && f.dia === dia && f.periodo === periodo);
              matriz[dia][periodo] = this.crearClase(carga, ubicacion.aulaId, fijada);
            }
          }));
          copia.set(seccion.id, matriz);
        }
        return copia;
      });
      this.marcarSinGuardar(objetivo.map(s => s.id));
      if (!objetivo.some(s => s.id === this.seccionActiva())) this.seccionActiva.set(objetivo[0].id);
      this.vista.set('seccion');
      this.generando.set(false);

      const sobrecargados = this.docentesSobrecargados(objetivo.map(s => s.id), docentesOcupados);
      if (sobrecargados.length) {
        this.avisar('error', `No caben todos los períodos: ${sobrecargados.join('; ')}. `
          + 'Una persona no puede dictar más horas que las que tiene la jornada. Usa “Repartir un curso entre varios docentes” '
          + '(abajo) o aumenta los días o períodos por día.');
        this.panelCargaAbierto.set(true);
      } else if (resultado.totalFaltantes === 0) {
        this.avisar('ok', `Listo: ${objetivo.length} sección(es) con toda su carga ubicada, sin cruces de docentes ni aulas. Revisa, ajusta a tu gusto y guarda.`);
      } else {
        this.avisar('info', `Quedaron ${resultado.totalFaltantes} período(s) sin lugar. Prueba “Otra variante”, más períodos por día o un máximo diario mayor; también puedes ubicarlos arrastrándolos.`);
      }
    }, 30);
  }

  vaciarSeccion(): void {
    const id = this.seccionActiva();
    if (id == null || this.soloLectura) return;
    if (!confirm('¿Quitar todas las clases de esta sección? (Las fijadas también se quitan.)')) return;
    this.guardarHistorial();
    this.matrices.update(actual => new Map(actual).set(id, this.matrizVacia(this.parametros())));
    this.marcarSinGuardar([id]);
  }

  // ------------------------------------------------------------ edición manual

  alIniciarArrastreCelda(evento: DragEvent, dia: string, periodo: number, clase: Clase): void {
    const id = this.seccionActiva();
    if (this.soloLectura || id == null) return;
    const carga = this.cargasDe(id).find(c => c.asignacionId === clase.asignacionId);
    if (!carga) return;
    evento.dataTransfer?.setData('text/plain', clase.curso);
    if (evento.dataTransfer) evento.dataTransfer.effectAllowed = 'move';
    this.arrastre.set({ origen: 'celda', seccionId: id, dia, periodo, carga });
  }

  alIniciarArrastrePendiente(evento: DragEvent, carga: Carga): void {
    const id = this.seccionActiva();
    if (this.soloLectura || id == null) return;
    evento.dataTransfer?.setData('text/plain', carga.curso);
    if (evento.dataTransfer) evento.dataTransfer.effectAllowed = 'copy';
    this.arrastre.set({ origen: 'pendiente', seccionId: id, carga });
  }

  alTerminarArrastre(): void {
    this.arrastre.set(null);
  }

  permitirSoltar(evento: DragEvent, dia: string, periodo: number): void {
    if (this.arrastre() && !this.docenteOcupado(this.arrastre()!.carga.docenteId, dia, periodo)) evento.preventDefault();
  }

  soltar(evento: DragEvent, dia: string, periodo: number): void {
    evento.preventDefault();
    const arrastre = this.arrastre();
    this.arrastre.set(null);
    const id = this.seccionActiva();
    const matriz = this.matrizActiva();
    if (!arrastre || id == null || !matriz) return;
    if (arrastre.origen === 'celda' && arrastre.dia === dia && arrastre.periodo === periodo) return;

    const destino = matriz[dia]?.[periodo] ?? null;
    const origen = arrastre.origen === 'celda' ? matriz[arrastre.dia!][arrastre.periodo!] : null;

    // Al intercambiar, el docente que vuelve al origen tampoco puede tener cruce.
    if (destino && arrastre.origen === 'celda'
      && this.docenteOcupado(destino.docenteId, arrastre.dia!, arrastre.periodo!)) {
      this.avisar('error', `No se puede intercambiar: ${destino.docente} ya dicta en otra sección a esa hora.`);
      return;
    }
    if (this.docenteOcupado(arrastre.carga.docenteId, dia, periodo)) {
      this.avisar('error', `${arrastre.carga.docente} ya dicta en otra sección a esa hora.`);
      return;
    }

    this.guardarHistorial();
    this.editarMatriz(id, m => {
      if (arrastre.origen === 'celda') {
        m[dia][periodo] = origen;
        m[arrastre.dia!][arrastre.periodo!] = destino;
      } else {
        const aula = this.aulaLibre(id, dia, periodo, destino?.aulaId);
        if (aula == null) throw new Error('sin-aula');
        m[dia][periodo] = this.crearClase(arrastre.carga, aula);
      }
    });
  }

  abrirCelda(dia: string, periodo: number): void {
    if (this.soloLectura || this.vista() !== 'seccion' || this.arrastre()) return;
    this.celdaEditando.set({ dia, periodo });
  }

  cerrarCelda(): void {
    this.celdaEditando.set(null);
  }

  /** Opciones para la celda abierta, con el motivo si no se puede usar. */
  readonly opcionesCelda = computed(() => {
    const celda = this.celdaEditando();
    const id = this.seccionActiva();
    if (!celda || id == null) return [];
    const actual = this.matrizActiva()?.[celda.dia]?.[celda.periodo] ?? null;
    return this.pendientes().map(carga => {
      const ocupadoEn = this.seccionDondeDicta(carga.docenteId, celda.dia, celda.periodo, id);
      const esActual = actual?.asignacionId === carga.asignacionId;
      const porDia = this.contarDia(id, celda.dia, carga.asignacionId) - (esActual ? 1 : 0);
      return {
        carga,
        faltan: carga.faltan,
        bloqueo: ocupadoEn ? `Dicta en ${ocupadoEn}` : null,
        aviso: !esActual && porDia + 1 > this.parametros().maxMismoCursoPorDia
          ? `Supera ${this.parametros().maxMismoCursoPorDia} por día` : (!esActual && carga.faltan === 0 ? 'Carga completa' : null),
        esActual
      };
    });
  });

  claseCeldaEditando(): Clase | null {
    const celda = this.celdaEditando();
    return celda ? this.matrizActiva()?.[celda.dia]?.[celda.periodo] ?? null : null;
  }

  asignarEnCelda(carga: Carga): void {
    const celda = this.celdaEditando();
    const id = this.seccionActiva();
    if (!celda || id == null) return;
    const actual = this.claseCeldaEditando();
    const aula = this.aulaLibre(id, celda.dia, celda.periodo, actual?.aulaId);
    if (aula == null) {
      this.avisar('error', 'No hay un aula libre a esa hora.');
      return;
    }
    this.guardarHistorial();
    this.editarMatriz(id, m => { m[celda.dia][celda.periodo] = this.crearClase(carga, aula, actual?.fijada); });
    this.cerrarCelda();
  }

  liberarCelda(): void {
    const celda = this.celdaEditando();
    const id = this.seccionActiva();
    if (!celda || id == null) return;
    this.guardarHistorial();
    this.editarMatriz(id, m => { m[celda.dia][celda.periodo] = null; });
    this.cerrarCelda();
  }

  alternarFijada(dia: string, periodo: number, evento?: Event): void {
    evento?.stopPropagation();
    const id = this.seccionActiva();
    if (id == null || this.soloLectura) return;
    this.editarMatriz(id, m => {
      const clase = m[dia][periodo];
      if (clase) m[dia][periodo] = { ...clase, fijada: !clase.fijada };
    });
  }

  deshacer(): void {
    const anterior = this.historial.pop();
    if (!anterior) return;
    this.matrices.set(anterior);
    this.puedeDeshacer.set(this.historial.length > 0);
    this.cambiosSinGuardar.set(new Set([...this.cambiosSinGuardar(), ...anterior.keys()]));
  }

  /** Para pintar en verde/rojo las celdas mientras se arrastra. */
  estadoDestino(dia: string, periodo: number): 'libre' | 'ocupado' | null {
    const arrastre = this.arrastre();
    if (!arrastre) return null;
    return this.docenteOcupado(arrastre.carga.docenteId, dia, periodo) ? 'ocupado' : 'libre';
  }

  // ------------------------------------------------------------ guardar

  guardarActiva(): void {
    const id = this.seccionActiva();
    if (id != null) this.guardar([id]);
  }

  guardarTurno(): void {
    const ids = this.resumenSecciones().filter(r => r.sinGuardar).map(r => r.seccion.id);
    if (!ids.length) {
      this.avisar('info', 'No hay cambios sin guardar en este turno.');
      return;
    }
    this.guardar(ids);
  }

  private guardar(ids: number[]): void {
    if (this.soloLectura) return;
    this.guardando.set(true);
    // Todas las secciones en una sola operación: al publicar un turno regenerado no hay
    // choques con los horarios antiguos de las demás secciones, y si algo falla no se guarda nada.
    this.horarioService.reemplazarVarias(ids.map(id => ({ seccionId: id, bloques: this.payload(id) }))).subscribe({
      next: guardados => {
        this.guardando.set(false);
        const nuevos = Object.values(guardados).flat();
        this.horariosGuardados = [...this.horariosGuardados.filter(h => !ids.includes(h.seccionId)), ...nuevos];
        this.cambiosSinGuardar.update(set => new Set([...set].filter(id => !ids.includes(id))));
        this.mensaje.set(null);
        this.toasts.exito('Horario publicado', `${ids.length} sección(es) actualizadas; docentes y estudiantes ya lo ven.`);
      },
      error: error => {
        this.guardando.set(false);
        this.avisar('error', `No se guardó ningún cambio: ${this.textoError(error, 'error al guardar')}.`);
      }
    });
  }

  private payload(seccionId: number): HorarioDTO[] {
    const matriz = this.matrices().get(seccionId);
    if (!matriz) return [];
    const p = this.parametros();
    const bloques = this.bloques().filter(b => !b.esReceso);
    const lista: HorarioDTO[] = [];
    for (const dia of p.dias) {
      (matriz[dia] ?? []).forEach((clase, periodo) => {
        const bloque = bloques[periodo];
        if (!clase || !bloque) return;
        lista.push({
          diaSemana: dia.toUpperCase(),
          horaInicio: `${bloque.inicio}:00`,
          horaFin: `${bloque.fin}:00`,
          turno: this.turno(),
          aulaId: clase.aulaId,
          seccionId,
          cursoId: clase.cursoId,
          docenteId: clase.docenteId
        });
      });
    }
    return lista;
  }

  // ------------------------------------------------------------ exportar

  exportarCsv(): void {
    const filas: string[][] = [['Hora', ...this.parametros().dias.map(d => this.etiquetaDia(d))]];
    for (const bloque of this.bloques()) {
      if (bloque.esReceso) {
        filas.push([`${bloque.inicio} - ${bloque.fin}`, ...this.parametros().dias.map(() => 'RECREO')]);
        continue;
      }
      filas.push([`${bloque.inicio} - ${bloque.fin}`, ...this.parametros().dias.map(dia => {
        if (this.vista() === 'docente') {
          const item = this.matrizDocente()[dia]?.[bloque.periodo];
          return item ? `${item.clase.curso} · ${item.seccion} · ${item.clase.aula}` : '';
        }
        const clase = this.matrizActiva()?.[dia]?.[bloque.periodo];
        return clase ? `${clase.curso} · ${clase.docente} · ${clase.aula}` : '';
      })]);
    }
    const csv = filas.map(f => f.map(v => `"${v.replaceAll('"', '""')}"`).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `horario-${this.vista() === 'docente' ? 'docente' : (this.seccionActivaInfo()?.gradoNombre ?? '') + '-' + (this.seccionActivaInfo()?.nombre ?? '')}.csv`.replace(/\s+/g, '_');
    enlace.click();
    URL.revokeObjectURL(url);
  }

  imprimir(): void {
    window.print();
  }

  // ------------------------------------------------------------ carga académica

  guardarCarga(): void {
    const { cursoId, docenteId, periodos } = this.nuevaCarga;
    const seccionId = this.nuevaCargaSeccion();
    if (!seccionId || !cursoId || !docenteId || periodos < 1) {
      this.avisar('error', 'Completa sección, curso, docente y períodos por semana.');
      return;
    }
    this.guardandoCarga.set(true);
    this.horarioService.guardarAsignacion({
      seccionId: Number(seccionId), cursoId: Number(cursoId), docenteId: Number(docenteId),
      horasSemanales: Number(periodos), anioEscolar: this.anioEscolar, activa: true
    }).subscribe({
      next: asignacion => {
        this.asignaciones.update(lista => [...lista, asignacion]);
        this.guardandoCarga.set(false);
        this.toasts.exito('Carga agregada', 'Ya puedes ubicarla en el horario.');
      },
      error: error => {
        this.guardandoCarga.set(false);
        this.avisar('error', this.textoError(error, 'No se pudo guardar la carga.'));
      }
    });
  }

  eliminarCarga(asignacion: AsignacionAcademica): void {
    if (!asignacion.id || !confirm(`¿Quitar ${asignacion.cursoNombre} de ${asignacion.seccionNombre}?`)) return;
    this.horarioService.eliminarAsignacion(asignacion.id).subscribe({
      next: () => this.asignaciones.update(lista => lista.filter(a => a.id !== asignacion.id)),
      error: error => this.avisar('error', this.textoError(error, 'No se pudo quitar la carga.'))
    });
  }

  cargasDeSeccionActiva(): AsignacionAcademica[] {
    const id = this.seccionActiva();
    return this.asignaciones().filter(a => a.seccionId === id);
  }

  elegirCursoReparto(nombre: string): void {
    this.repartoCurso.set(nombre);
    this.repartoResultado.set(null);
    this.repartoSecciones.set(new Set(this.seccionesReparto().map(item => item.seccion.id)));
    const actuales = new Set(this.seccionesReparto().flatMap(item =>
      this.asignaciones().filter(a => a.seccionId === item.seccion.id && a.cursoId === item.cursoId).map(a => a.docenteId)));
    this.repartoDocentes.set(actuales);
    const horas = this.seccionesReparto().find(item => item.horas > 0)?.horas ?? null;
    this.repartoHoras.set(horas);
  }

  alternarSeccionReparto(id: number): void {
    this.repartoSecciones.update(set => { const c = new Set(set); c.has(id) ? c.delete(id) : c.add(id); return c; });
    this.repartoResultado.set(null);
  }

  alternarDocenteReparto(id: number): void {
    this.repartoDocentes.update(set => { const c = new Set(set); c.has(id) ? c.delete(id) : c.add(id); return c; });
    this.repartoResultado.set(null);
  }

  repartir(simular: boolean): void {
    const items = this.seccionesReparto()
      .filter(item => this.repartoSecciones().has(item.seccion.id))
      .map(item => ({ seccionId: item.seccion.id, cursoId: item.cursoId, horasSemanales: Number(this.repartoHoras()) || item.horas }));
    if (!items.length || !this.repartoDocentes().size) {
      this.avisar('error', 'Elige el curso, al menos una sección y los docentes que lo dictarán.');
      return;
    }
    if (!simular && this.cambiosSinGuardar().size
      && !confirm('Tienes cambios de horario sin guardar. El reparto recarga los datos y se perderán. ¿Continuar?')) return;
    this.repartiendo.set(true);
    this.horarioService.repartir({
      anioEscolar: this.anioEscolar,
      docenteIds: [...this.repartoDocentes()],
      items,
      simular
    }).subscribe({
      next: resultado => {
        this.repartiendo.set(false);
        this.repartoResultado.set(resultado);
        if (resultado.aplicado) {
          const extra = resultado.horariosLiberados
            ? ` ${resultado.horariosLiberados} bloque(s) de horario quedaron libres para reubicar.` : '';
          this.toasts.exito('Carga repartida', `${resultado.plan.length} sección(es) actualizadas.${extra}`);
          this.cargarCatalogos();
        }
      },
      error: error => {
        this.repartiendo.set(false);
        this.avisar('error', this.textoError(error, 'No se pudo repartir la carga.'));
      }
    });
  }

  // ------------------------------------------------------------ carga de datos

  refrescarEnVivo(): void {
    this.cargarCatalogos();
  }

  private cargarCatalogos(): void {
    this.cargando.set(true);
    forkJoin({
      docentes: this.horarioService.listarDocentes(),
      cursos: this.horarioService.listarCursos(),
      aulas: this.horarioService.listarAulas(),
      secciones: this.horarioService.listarSecciones(),
      asignaciones: this.horarioService.listarAsignaciones(this.anioEscolar)
    }).subscribe({
      next: data => {
        this.docentes.set(data.docentes.filter(d => d.estado === 'ACTIVO'));
        this.aulas.set(data.aulas.filter(a => a.activa !== false));
        const secciones = data.secciones.filter(s => s.activa !== false && s.nivelNombre === 'SECUNDARIA');
        this.secciones.set(secciones);
        const grados = new Set(secciones.map(s => s.gradoId));
        const ids = new Set(secciones.map(s => s.id));
        this.cursos.set(data.cursos.filter(c => c.estado === 'ACTIVO' && grados.has(c.gradoId)));
        this.asignaciones.set(data.asignaciones.filter(a => a.activa !== false && ids.has(a.seccionId)));
        if (this.nuevaCargaSeccion() == null) this.nuevaCargaSeccion.set(secciones[0]?.id ?? null);
        this.cargarGuardados();
      },
      error: () => {
        this.cargando.set(false);
        this.avisar('error', 'No se pudieron cargar cursos, docentes, aulas y secciones. Intenta de nuevo.');
      }
    });
  }

  private cargarGuardados(): void {
    const secciones = this.secciones();
    if (!secciones.length) {
      this.cargando.set(false);
      return;
    }
    forkJoin(secciones.map(s => this.horarioService.listarPorSeccion(s.id).pipe(catchError(() => of([] as HorarioDTO[])))))
      .subscribe(resultados => {
        this.horariosGuardados = resultados.flat();
        this.cargando.set(false);
        this.reconstruirDesdeGuardado();
      });
  }

  private reconstruirDesdeGuardado(): void {
    const p = this.parametros();
    const bloques = construirBloques(p).filter(b => !b.esReceso);
    const nuevas = new Map<number, Matriz>();
    let sinLugar = 0;
    for (const seccion of this.seccionesTurno()) {
      const matriz = this.matrizVacia(p);
      const cargas = this.cargasDe(seccion.id);
      this.horariosGuardados
        .filter(h => h.seccionId === seccion.id && this.normalizarTurno(h.turno) === this.turno())
        .forEach(h => {
          const dia = h.diaSemana.toLowerCase();
          const periodo = bloques.findIndex(b => b.inicio === h.horaInicio.slice(0, 5));
          const carga = cargas.find(c => c.cursoId === h.cursoId && c.docenteId === h.docenteId);
          if (periodo < 0 || !matriz[dia] || !carga) {
            sinLugar++;
            return;
          }
          matriz[dia][periodo] = this.crearClase(carga, h.aulaId);
        });
      nuevas.set(seccion.id, matriz);
    }
    this.matrices.set(nuevas);
    this.historial.length = 0;
    this.puedeDeshacer.set(false);
    this.cambiosSinGuardar.set(new Set());
    this.noCoinciden.set(sinLugar);
    const activa = this.seccionActiva();
    if (activa == null || !nuevas.has(activa)) this.seccionActiva.set(this.seccionesTurno()[0]?.id ?? null);
    if (this.docenteActivo() == null) this.docenteActivo.set(this.docentesConCarga()[0]?.id ?? null);
  }

  // ------------------------------------------------------------ utilidades

  esAlcance(valor: Alcance): boolean {
    return this.alcance() === valor;
  }

  private seccionesObjetivo(): SeccionHorario[] {
    const secciones = this.seccionesTurno();
    if (this.alcance() === 'GRADO') return secciones.filter(s => s.gradoId === Number(this.gradoAlcance()));
    if (this.alcance() === 'SECCION') return secciones.filter(s => s.id === Number(this.seccionAlcance() ?? this.seccionActiva()));
    return secciones;
  }

  /** Docentes cuya carga en el turno supera las horas que tiene la jornada. */
  private docentesSobrecargados(seccionIds: number[], ocupadosFuera: Set<string>): string[] {
    const p = this.parametros();
    const capacidad = p.dias.length * p.periodosPorDia;
    const demanda = new Map<number, { nombre: string; periodos: number }>();
    for (const id of seccionIds) {
      for (const carga of this.cargasDe(id)) {
        const actual = demanda.get(carga.docenteId) ?? { nombre: carga.docente, periodos: 0 };
        actual.periodos += carga.periodos;
        demanda.set(carga.docenteId, actual);
      }
    }
    for (const clave of ocupadosFuera) {
      const docenteId = Number(clave.split('|')[0]);
      const actual = demanda.get(docenteId);
      if (actual) actual.periodos++;
    }
    return [...demanda.values()]
      .filter(d => d.periodos > capacidad)
      .sort((a, b) => b.periodos - a.periodos)
      .map(d => `${d.nombre} tiene ${d.periodos} períodos y la jornada solo tiene ${capacidad}`);
  }

  private cargasDe(seccionId: number): Carga[] {
    return this.asignaciones()
      .filter(a => a.seccionId === seccionId && a.activa !== false && a.id != null)
      .flatMap(a => {
        const curso = this.cursos().find(c => c.id === a.cursoId);
        const docente = this.docentes().find(d => d.id === a.docenteId);
        if (!curso || !docente) return [];
        return [{
          asignacionId: a.id!,
          cursoId: curso.id,
          curso: curso.nombre,
          docenteId: docente.id,
          docente: `${docente.nombre} ${docente.apellido}`,
          periodos: a.horasSemanales
        }];
      });
  }

  private crearClase(carga: Carga, aulaId: number, fijada = false): Clase {
    const aula = this.aulas().find(a => a.id === aulaId);
    return {
      asignacionId: carga.asignacionId,
      cursoId: carga.cursoId,
      curso: carga.curso,
      docenteId: carga.docenteId,
      docente: carga.docente,
      aulaId,
      aula: aula?.nombre ?? `Aula ${aulaId}`,
      fijada
    };
  }

  private matrizVacia(p: ParametrosHorario): Matriz {
    const matriz: Matriz = {};
    for (const dia of p.dias) matriz[dia] = Array(p.periodosPorDia).fill(null);
    return matriz;
  }

  private redimensionar(matriz: Matriz, p: ParametrosHorario): Matriz {
    const nueva = this.matrizVacia(p);
    for (const dia of p.dias) {
      (matriz[dia] ?? []).slice(0, p.periodosPorDia).forEach((clase, i) => { nueva[dia][i] = clase; });
    }
    return nueva;
  }

  private editarMatriz(seccionId: number, cambio: (m: Matriz) => void): void {
    const actual = this.matrices().get(seccionId);
    if (!actual) return;
    const copia: Matriz = {};
    for (const dia of Object.keys(actual)) copia[dia] = [...actual[dia]];
    try {
      cambio(copia);
    } catch {
      this.avisar('error', 'No hay un aula libre a esa hora.');
      return;
    }
    this.matrices.update(m => new Map(m).set(seccionId, copia));
    this.marcarSinGuardar([seccionId]);
  }

  private guardarHistorial(): void {
    const copia = new Map<number, Matriz>();
    for (const [id, matriz] of this.matrices()) {
      const m: Matriz = {};
      for (const dia of Object.keys(matriz)) m[dia] = [...matriz[dia]];
      copia.set(id, m);
    }
    this.historial.push(copia);
    if (this.historial.length > 40) this.historial.shift();
    this.puedeDeshacer.set(true);
  }

  private marcarSinGuardar(ids: number[]): void {
    this.cambiosSinGuardar.update(set => new Set([...set, ...ids]));
  }

  /** ¿El docente dicta en OTRA sección a esa hora? (Dentro de la misma sección se reemplaza.) */
  private docenteOcupado(docenteId: number, dia: string, periodo: number): boolean {
    return this.seccionDondeDicta(docenteId, dia, periodo, this.seccionActiva()) != null;
  }

  private seccionDondeDicta(docenteId: number, dia: string, periodo: number, excluir: number | null): string | null {
    for (const [seccionId, matriz] of this.matrices()) {
      if (seccionId === excluir) continue;
      if (matriz[dia]?.[periodo]?.docenteId === docenteId) {
        const s = this.secciones().find(x => x.id === seccionId);
        return `${s?.gradoNombre ?? ''} ${s?.nombre ?? ''}`.trim();
      }
    }
    return null;
  }

  private aulaLibre(seccionId: number, dia: string, periodo: number, preferida?: number): number | null {
    const ocupadas = new Set<number>();
    for (const [id, matriz] of this.matrices()) {
      if (id === seccionId) continue;
      const clase = matriz[dia]?.[periodo];
      if (clase) ocupadas.add(clase.aulaId);
    }
    // Primero el aula que ya usa la sección (su "aula de casa").
    const habitual = this.aulaHabitual(seccionId);
    const candidatas = [preferida, habitual, ...this.aulasTurno().map(a => a.id)].filter((a): a is number => a != null);
    return candidatas.find(a => !ocupadas.has(a)) ?? null;
  }

  private aulaHabitual(seccionId: number): number | undefined {
    const conteo = new Map<number, number>();
    const matriz = this.matrices().get(seccionId);
    if (!matriz) return undefined;
    Object.values(matriz).flat().forEach(c => { if (c) conteo.set(c.aulaId, (conteo.get(c.aulaId) ?? 0) + 1); });
    return [...conteo].sort((a, b) => b[1] - a[1])[0]?.[0];
  }

  private contar(matriz: Matriz, criterio: (c: Clase) => boolean): number {
    return Object.values(matriz).flat().filter((c): c is Clase => !!c && criterio(c)).length;
  }

  private contarDia(seccionId: number, dia: string, asignacionId: number): number {
    return (this.matrices().get(seccionId)?.[dia] ?? []).filter(c => c?.asignacionId === asignacionId).length;
  }

  private leerParametros(turno: string): ParametrosHorario {
    try {
      const guardado = localStorage.getItem(`pad_horario_parametros_${turno}`);
      if (guardado) return sanearParametros({ ...parametrosPorDefecto(turno), ...JSON.parse(guardado) });
    } catch {
      /* valores por defecto */
    }
    return parametrosPorDefecto(turno);
  }

  private normalizarTurno(turno: string | null | undefined): string {
    return (turno ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase();
  }

  private avisar(tipo: 'ok' | 'error' | 'info', texto: string): void {
    this.mensaje.set({ tipo, texto });
  }

  private textoError(error: any, predeterminado = 'Error desconocido'): string {
    return typeof error?.error === 'string' ? error.error : error?.error?.message || predeterminado;
  }
}
