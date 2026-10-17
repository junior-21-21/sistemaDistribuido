import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { EstudianteService } from '../../services/estudiante.service';
import {
  AsignacionAcademica,
  CursoHorario,
  DocenteHorario,
  HorarioService,
  SeccionHorario
} from '../../services/horario.service';
import { BimestreAuditoria, BimestreEstado, Nota, NotaService } from '../../services/nota.service';
import { LiveBanner } from '../../shared/live-banner/live-banner';
import { interpretar } from './notas-voz';
import { Literal, ResultadoPromedio, literalDesdeValor, promedioPonderado, repartirIgual } from './promedio-notas';
import { CeldaXlsx, EstiloXlsx, descargarXlsx } from '../../shared/excel/xlsx';
import { ContinuarSeccion, SeccionRecordada, UltimaSeccionService } from '../../shared/continuar-seccion/continuar-seccion';

interface EstudianteRow {
  id: number;
  nombre: string;
  apellido: string;
  notas: Record<string, string>;
}

/** Clave para comparar nombres de columnas sin distinguir mayúsculas ni espacios. */
const clave = (texto: string) => texto.trim().toLocaleLowerCase('es');

@Component({
  selector: 'app-evaluaciones',
  standalone: true,
  imports: [ContinuarSeccion, LiveBanner, CommonModule, FormsModule],
  templateUrl: './evaluaciones.html',
  styleUrl: './evaluaciones.css'
})
export class Evaluaciones implements OnInit, OnDestroy {
  private readonly ultimaSeccion = inject(UltimaSeccionService);
  private readonly notaService = inject(NotaService);
  private readonly estudianteService = inject(EstudianteService);
  private readonly horarioService = inject(HorarioService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly zona = inject(NgZone);

  // Dictado de notas por voz (Web Speech API de Chrome/Edge)
  readonly vozDisponible = typeof window !== 'undefined'
    && !!((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition);
  escuchando = false;
  transcripcion = '';
  competenciaVoz = '';
  resaltado = '';
  registrosVoz: { texto: string; ok: boolean }[] = [];
  private reconocimiento: any = null;

  currentSeccionId: number | null = null;
  currentCursoId: number | null = null;
  currentDocenteId: number | null = null;
  
  // Nav
  vistaActual: 'grados' | 'secciones' | 'gestor' = 'grados';
  gradoNavegacion: any = null;
  currentBimestre = 'I BIMESTRE';
  readonly anioEscolar = new Date().getFullYear();
  readonly bimestres = ['I BIMESTRE', 'II BIMESTRE', 'III BIMESTRE', 'IV BIMESTRE'];
  readonly escalas = ['AD', 'A', 'B', 'C'];

  secciones: SeccionHorario[] = [];
  cursos: CursoHorario[] = [];
  docentes: DocenteHorario[] = [];
  asignaciones: AsignacionAcademica[] = [];
  competencias: string[] = [];
  /** Porcentaje que vale cada columna dentro del promedio del bimestre. */
  pesos: Record<string, number | null> = {};
  /** Columnas que ya existen en el servidor (con notas o con peso guardado). */
  private columnasGuardadas = new Set<string>();
  nuevaCompetencia = '';
  estudiantes: EstudianteRow[] = [];

  saving = false;
  loading = true;
  savedMessage = '';
  errorMessage = '';
  estadosBimestre: BimestreEstado[] = [];
  auditoriaBimestre: BimestreAuditoria[] = [];
  motivoCambioEstado = '';
  cambiandoEstado = false;

  get esDirector(): boolean {
    return this.authService.hasAnyRole('DIRECTOR', 'SUBDIRECTOR');
  }

  get esCoordinadorAcademico(): boolean {
    return this.authService.hasAnyRole('DIRECTOR', 'SUBDIRECTOR');
  }

  get bimestreCerrado(): boolean {
    return this.estadosBimestre.find(item => item.bimestre === this.currentBimestre)?.estado === 'CERRADO';
  }

  get soloLectura(): boolean {
    return this.authService.hasAnyRole('DIRECTOR', 'SUBDIRECTOR');
  }

  /** Suma de los porcentajes de todas las columnas. */
  get sumaPesos(): number {
    return Math.round(this.competencias.reduce((total, c) => total + (Number(this.pesos[c]) || 0), 0) * 100) / 100;
  }

  get pesosCompletos(): boolean {
    return this.sumaPesos === 100;
  }

  promedio(estudiante: EstudianteRow): ResultadoPromedio {
    return promedioPonderado(estudiante.notas, this.competencias, this.pesos);
  }

  /** Promedio de la sección y cantidad de estudiantes por nivel de logro. */
  get resumenSeccion(): { valor: number | null; literal: Literal | null; conteo: Record<Literal, number>; aprobados: number } {
    const conteo: Record<Literal, number> = { AD: 0, A: 0, B: 0, C: 0 };
    const valores: number[] = [];
    for (const estudiante of this.estudiantes) {
      const r = this.promedio(estudiante);
      if (r.literal && r.valor !== null) {
        conteo[r.literal]++;
        valores.push(r.valor);
      }
    }
    if (!valores.length) return { valor: null, literal: null, conteo, aprobados: 0 };
    const valor = Math.round(valores.reduce((t, v) => t + v, 0) / valores.length * 100) / 100;
    return { valor, literal: literalDesdeValor(valor), conteo, aprobados: conteo.AD + conteo.A };
  }

  /** Cantidad de cada nota en una columna (para el pie de la planilla). */
  conteoColumna(competencia: string): Record<Literal, number> {
    const conteo: Record<Literal, number> = { AD: 0, A: 0, B: 0, C: 0 };
    for (const estudiante of this.estudiantes) {
      const nota = estudiante.notas[competencia] as Literal | undefined;
      if (nota && nota in conteo) conteo[nota]++;
    }
    return conteo;
  }

  /** Letra de columna al estilo de una hoja de cálculo (A, B, …, Z, AA…). */
  letraColumna(indice: number): string {
    let letra = '';
    for (let n = indice + 1; n > 0; n = Math.floor((n - 1) / 26)) letra = String.fromCharCode(65 + ((n - 1) % 26)) + letra;
    return letra;
  }

  repartirPesos(): void {
    this.pesos = { ...this.pesos, ...repartirIgual(this.competencias) };
  }

  get cursosDisponibles(): CursoHorario[] {
    const ids = new Set(this.asignaciones
      .filter(item => item.seccionId === Number(this.currentSeccionId) && item.activa !== false)
      .map(item => item.cursoId));
    return this.cursos.filter(curso => ids.has(curso.id));
  }

  get seccionActual(): string {
    const seccion = this.secciones.find(item => item.id === Number(this.currentSeccionId));
    return seccion ? `${seccion.gradoNombre || seccion.nivelNombre} · Sección ${seccion.nombre}` : 'Sin sección';
  }

  get cursoActual(): string {
    return this.cursos.find(item => item.id === Number(this.currentCursoId))?.nombre ?? 'Sin área curricular';
  }

  get docenteActual(): string {
    const docente = this.docentes.find(item => item.id === Number(this.currentDocenteId));
    return docente ? `${docente.nombre} ${docente.apellido}` : 'Sin docente asociado';
  }

  ngOnInit(): void {
    this.cargarCatalogos();
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
    this.currentSeccionId = seccion.id;
    this.vistaActual = 'gestor';
    this.cambiarSeccion();
  }

  volverAGrados(): void {
    this.vistaActual = 'grados';
    this.gradoNavegacion = null;
    this.currentSeccionId = null;
  }

  volverASecciones(): void {
    this.vistaActual = 'secciones';
    this.currentSeccionId = null;
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

  cambiarSeccion(): void {
    this.seleccionarCursoCompatible();
    this.actualizarDocenteAsignado();
    this.cargarEvaluaciones();
  }

  cambiarCurso(): void {
    this.actualizarDocenteAsignado();
    this.cargarEvaluaciones();
  }

  cambiarBimestre(): void {
    this.cargarEvaluaciones();
    this.cargarAuditoria();
  }

  cargarEvaluaciones(): void {
    if (!this.currentSeccionId || !this.currentCursoId) {
      this.estudiantes = [];
      this.competencias = [];
      this.loading = false;
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    forkJoin({
      estudiantes: this.estudianteService.getEstudiantes(),
      notas: this.notaService.getNotasPorSeccion(this.currentSeccionId, this.currentBimestre, this.currentCursoId),
      ponderaciones: this.notaService.getPonderaciones(this.currentSeccionId, this.currentCursoId,
        this.currentBimestre, this.anioEscolar)
    }).subscribe({
      next: ({ estudiantes, notas, ponderaciones }) => {
        const notasCurso = notas.filter(nota => nota.cursoId === Number(this.currentCursoId));
        // Primero las columnas en el orden en que se guardaron sus pesos; luego las que solo tienen notas.
        const conPeso = ponderaciones.columnas.map(item => item.competencia);
        const vistas = new Set(conPeso.map(clave));
        this.competencias = [...conPeso,
          ...[...new Set(notasCurso.map(nota => nota.competencia))].filter(c => !vistas.has(clave(c)))];
        this.pesos = Object.fromEntries(this.competencias.map(c =>
          [c, ponderaciones.columnas.find(item => clave(item.competencia) === clave(c))?.peso ?? null]));
        this.columnasGuardadas = new Set(this.competencias.map(clave));
        if (!this.competencias.includes(this.competenciaVoz)) this.competenciaVoz = this.competencias[0] ?? '';
        this.estudiantes = estudiantes
          .filter(estudiante => estudiante.seccionId === Number(this.currentSeccionId) && estudiante.id != null
            && (estudiante.estado ?? 'ACTIVO') === 'ACTIVO')
          .map(estudiante => ({
            id: estudiante.id!,
            nombre: estudiante.nombre,
            apellido: estudiante.apellido,
            notas: Object.fromEntries(
              notasCurso
                .filter(nota => nota.estudianteId === estudiante.id && nota.escalaLiteral)
                .map(nota => [nota.competencia, nota.escalaLiteral!])
            )
          }));
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se pudieron cargar los estudiantes y sus evaluaciones.';
        this.cdr.detectChanges();
      }
    });
  }

  agregarCompetencia(): void {
    const competencia = this.nuevaCompetencia.trim();
    if (!competencia || competencia.length > 100) {
      this.errorMessage = 'La competencia debe tener entre 1 y 100 caracteres.';
      return;
    }
    if (this.competencias.some(item => item.toLocaleLowerCase('es') === competencia.toLocaleLowerCase('es'))) {
      this.errorMessage = 'Esa competencia ya está en la tabla.';
      return;
    }
    this.competencias = [...this.competencias, competencia];
    this.pesos = { ...this.pesos, [competencia]: null };
    if (!this.competenciaVoz) this.competenciaVoz = competencia;
    this.nuevaCompetencia = '';
    this.errorMessage = '';
  }

  /** Quita una columna. Si ya estaba guardada, borra también sus notas en el servidor. */
  eliminarColumna(competencia: string): void {
    if (this.soloLectura || this.bimestreCerrado) return;
    const guardada = this.columnasGuardadas.has(clave(competencia));
    const conNotas = this.estudiantes.filter(e => e.notas[competencia]).length;
    const aviso = guardada && conNotas
      ? `¿Eliminar la columna «${competencia}» y sus ${conNotas} notas? Esta acción no se puede deshacer.`
      : `¿Quitar la columna «${competencia}»?`;
    if (!confirm(aviso)) return;
    const quitarLocal = () => {
      this.competencias = this.competencias.filter(c => c !== competencia);
      const resto = { ...this.pesos };
      delete resto[competencia];
      this.pesos = resto;
      this.estudiantes.forEach(e => delete e.notas[competencia]);
      this.columnasGuardadas.delete(clave(competencia));
      if (this.competenciaVoz === competencia) this.competenciaVoz = this.competencias[0] ?? '';
      this.cdr.detectChanges();
    };
    if (!guardada || !this.currentSeccionId || !this.currentCursoId || !this.currentDocenteId) {
      quitarLocal();
      return;
    }
    this.notaService.eliminarColumna({
      seccionId: this.currentSeccionId, cursoId: this.currentCursoId, docenteId: this.currentDocenteId,
      bimestre: this.currentBimestre, periodoAnio: this.anioEscolar, competencia
    }).subscribe({
      next: r => {
        quitarLocal();
        this.savedMessage = `Columna eliminada (${r.notasEliminadas} notas).`;
        this.cdr.detectChanges();
      },
      error: error => {
        this.errorMessage = error.error?.message || 'No se pudo eliminar la columna.';
        this.cdr.detectChanges();
      }
    });
  }

  /** Descarga la planilla del bimestre como archivo de Excel con colores y promedios. */
  exportarExcel(): void {
    const estiloNota = (nota?: string | null): EstiloXlsx =>
      ({ AD: EstiloXlsx.NotaAD, A: EstiloXlsx.NotaA, B: EstiloXlsx.NotaB, C: EstiloXlsx.NotaC } as Record<string, EstiloXlsx>)[nota ?? '']
      ?? EstiloXlsx.Texto;
    const encabezado: CeldaXlsx[] = [
      { v: 'N.°', s: EstiloXlsx.Encabezado }, { v: 'Apellidos y nombres', s: EstiloXlsx.Encabezado },
      ...this.competencias.map(c => ({ v: c, s: EstiloXlsx.Encabezado })),
      { v: 'Promedio', s: EstiloXlsx.Encabezado }, { v: 'Valor (1-4)', s: EstiloXlsx.Encabezado }
    ];
    const filaPesos: CeldaXlsx[] = [
      { v: '', s: EstiloXlsx.Peso }, { v: 'Peso de cada columna', s: EstiloXlsx.Peso },
      ...this.competencias.map(c => ({ v: this.pesos[c] != null ? `${this.pesos[c]} %` : '—', s: EstiloXlsx.Peso })),
      { v: `${this.sumaPesos} %`, s: EstiloXlsx.Peso }, { v: '', s: EstiloXlsx.Peso }
    ];
    const filas: CeldaXlsx[][] = this.estudiantes.map((e, i) => {
      const r = this.promedio(e);
      return [
        { v: i + 1, s: EstiloXlsx.Texto }, { v: `${e.apellido}, ${e.nombre}`, s: EstiloXlsx.Texto },
        ...this.competencias.map(c => ({ v: e.notas[c] ?? '', s: estiloNota(e.notas[c]) })),
        { v: r.literal ?? '', s: estiloNota(r.literal) }, { v: r.valor, s: EstiloXlsx.Numero }
      ];
    });
    const resumen = this.resumenSeccion;
    const ultima = String.fromCharCode(64 + Math.min(this.competencias.length + 4, 26));
    const nombre = `notas ${this.seccionActual} ${this.cursoActual} ${this.currentBimestre} ${this.anioEscolar}`
      .replace(/[·\\/:*?"<>|]+/g, ' ').trim().replace(/\s+/g, '_');
    descargarXlsx(`${nombre}.xlsx`, {
      nombre: `${this.currentBimestre} ${this.anioEscolar}`,
      filas: [
        [{ v: `Registro de notas — ${this.cursoActual}`, s: EstiloXlsx.Titulo }],
        [{ v: `${this.seccionActual} · ${this.currentBimestre} ${this.anioEscolar} · Docente: ${this.docenteActual}`, s: EstiloXlsx.Subtitulo }],
        [],
        encabezado, filaPesos, ...filas, [],
        [{ v: '' }, { v: 'Promedio de la sección', s: EstiloXlsx.Peso }, { v: resumen.literal ?? '—', s: estiloNota(resumen.literal) }],
        [{ v: '' }, { v: `AD: ${resumen.conteo.AD} · A: ${resumen.conteo.A} · B: ${resumen.conteo.B} · C: ${resumen.conteo.C}`, s: EstiloXlsx.Subtitulo }],
        [{ v: '' }, { v: 'Escala: AD = 4, A = 3, B = 2, C = 1. Promedio: AD desde 3,5; A desde 2,5; B desde 1,5; C menos de 1,5.', s: EstiloXlsx.Subtitulo }]
      ],
      anchos: [6, 36, ...this.competencias.map(() => 16), 12, 12],
      combinar: [`A1:${ultima}1`, `A2:${ultima}2`],
      inmovilizar: { columnas: 2, filas: 5 }
    });
  }

  guardarNotas(): void {
    if (this.soloLectura) {
      this.errorMessage = 'Esta vista es de supervisión. Las notas las registra el docente asignado.';
      return;
    }
    if (this.bimestreCerrado) {
      this.errorMessage = 'El bimestre está cerrado. Dirección debe autorizar una reapertura antes de editar.';
      return;
    }
    if (!this.currentSeccionId || !this.currentCursoId || !this.currentDocenteId) {
      this.errorMessage = 'Selecciona una sección, un curso y un docente válido.';
      return;
    }

    const notas: Nota[] = this.estudiantes.flatMap(estudiante =>
      this.competencias.flatMap(competencia => {
        const escala = estudiante.notas[competencia];
        return escala ? [{
          estudianteId: estudiante.id,
          cursoId: this.currentCursoId!,
          docenteId: this.currentDocenteId!,
          seccionId: this.currentSeccionId!,
          escalaLiteral: escala,
          competencia,
          bimestre: this.currentBimestre,
          periodoAnio: String(this.anioEscolar)
        }] : [];
      })
    );

    if (this.sumaPesos > 100) {
      this.errorMessage = `Los porcentajes suman ${this.sumaPesos} %. Ajusta las columnas para que no pasen de 100 %.`;
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.savedMessage = '';
    this.notaService.guardarPonderaciones({
      seccionId: this.currentSeccionId, cursoId: this.currentCursoId, docenteId: this.currentDocenteId,
      bimestre: this.currentBimestre, periodoAnio: String(this.anioEscolar),
      columnas: this.competencias.map(competencia => ({ competencia, peso: Number(this.pesos[competencia]) || 0 }))
    }).pipe(
      switchMap(() => notas.length ? this.notaService.guardarNotasMasivas(notas) : of([] as Nota[]))
    ).subscribe({
      next: guardadas => {
        this.saving = false;
        this.competencias.forEach(c => this.columnasGuardadas.add(clave(c)));
        this.savedMessage = guardadas.length
          ? `${guardadas.length} calificaciones y los porcentajes guardados.`
          : 'Porcentajes guardados.';
        this.cdr.detectChanges();
      },
      error: error => {
        this.saving = false;
        this.errorMessage = error.error?.message || 'No se pudieron guardar las calificaciones.';
        this.cdr.detectChanges();
      }
    });
  }

  alternarVoz(): void {
    if (this.escuchando) {
      this.detenerVoz();
      return;
    }
    const Reconocimiento = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!Reconocimiento) return;
    if (!this.competencias.includes(this.competenciaVoz)) this.competenciaVoz = this.competencias[0] ?? '';
    const rec = new Reconocimiento();
    rec.lang = 'es-PE';
    rec.continuous = true;
    rec.interimResults = true;
    rec.onresult = (evento: any) => this.zona.run(() => {
      for (let i = evento.resultIndex; i < evento.results.length; i++) {
        const texto: string = evento.results[i][0].transcript;
        if (evento.results[i].isFinal) {
          this.aplicarFraseDictada(texto);
          this.transcripcion = '';
        } else {
          this.transcripcion = texto;
        }
      }
      this.cdr.detectChanges();
    });
    rec.onerror = (evento: any) => this.zona.run(() => {
      if (evento.error === 'not-allowed') this.errorMessage = 'Permite el uso del micrófono para dictar notas.';
      this.detenerVoz();
    });
    // Chrome corta el dictado tras unos segundos de silencio: se reanuda mientras siga activo.
    rec.onend = () => this.zona.run(() => {
      if (this.escuchando) {
        try { rec.start(); } catch { this.detenerVoz(); }
      }
    });
    this.reconocimiento = rec;
    this.escuchando = true;
    rec.start();
  }

  /** Aplica una frase reconocida: "Juan Quispe, A" pone A en la competencia elegida. */
  aplicarFraseDictada(frase: string): void {
    const alumnos = this.estudiantes.map(e => ({ id: e.id, nombre: e.nombre, apellido: e.apellido }));
    const resultado = interpretar(frase, alumnos);
    let registro: { texto: string; ok: boolean };
    if (resultado.tipo === 'ok') {
      const fila = this.estudiantes.find(e => e.id === resultado.alumno.id)!;
      fila.notas[this.competenciaVoz] = resultado.nota;
      this.resaltado = `${fila.id}|${this.competenciaVoz}`;
      registro = { texto: `${fila.apellido}, ${fila.nombre} → ${resultado.nota}`, ok: true };
    } else if (resultado.tipo === 'ambiguo') {
      const nombres = resultado.candidatos.map(c => c.nombre + ' ' + c.apellido.split(' ')[0]).join(', ');
      registro = { texto: `"${frase}": hay varios (${nombres}). Di también el nombre.`, ok: false };
    } else if (resultado.tipo === 'sin-alumno') {
      registro = { texto: `"${frase}": no encontré a ese estudiante`, ok: false };
    } else {
      registro = { texto: `"${frase}": falta la nota (AD, A, B, C o 0 a 20)`, ok: false };
    }
    this.registrosVoz = [registro, ...this.registrosVoz].slice(0, 8);
  }

  private detenerVoz(): void {
    this.escuchando = false;
    this.transcripcion = '';
    try { this.reconocimiento?.stop(); } catch { /* ya detenido */ }
    this.reconocimiento = null;
  }

  ngOnDestroy(): void {
    this.detenerVoz();
  }

  trackById(_: number, item: EstudianteRow): number {
    return item.id;
  }

  cambiarEstadoBimestre(accion: 'CERRAR' | 'REABRIR'): void {
    const motivo = this.motivoCambioEstado.trim();
    if (motivo.length < 10) {
      this.errorMessage = 'Indica un motivo de al menos 10 caracteres para dejar evidencia auditable.';
      return;
    }
    this.cambiandoEstado = true;
    this.errorMessage = '';
    this.savedMessage = '';
    const operacion = accion === 'CERRAR'
      ? this.notaService.cerrarBimestre(this.anioEscolar, this.currentBimestre, motivo)
      : this.notaService.reabrirBimestre(this.anioEscolar, this.currentBimestre, motivo);
    operacion.subscribe({
      next: estado => {
        this.estadosBimestre = this.estadosBimestre.map(item =>
          item.bimestre === estado.bimestre ? estado : item);
        this.cambiandoEstado = false;
        this.motivoCambioEstado = '';
        this.savedMessage = accion === 'CERRAR' ? 'Bimestre cerrado y auditado.' : 'Bimestre reabierto y auditado.';
        this.cargarAuditoria();
        this.cdr.detectChanges();
      },
      error: error => {
        this.cambiandoEstado = false;
        this.errorMessage = error.error?.message || 'No se pudo cambiar el estado del bimestre.';
        this.cdr.detectChanges();
      }
    });
  }

  private cargarCatalogos(): void {
    const supervisor = this.authService.hasAnyRole('DIRECTOR', 'SUBDIRECTOR');
    forkJoin({
      secciones: this.horarioService.listarSecciones(),
      cursos: this.horarioService.listarCursos(),
      docentes: this.horarioService.listarDocentes(),
      asignaciones: supervisor
        ? this.horarioService.listarAsignaciones(this.anioEscolar)
        : this.horarioService.listarMisAsignaciones(this.anioEscolar),
      bimestres: this.notaService.listarBimestres(this.anioEscolar)
    }).subscribe({
      next: data => {
        this.asignaciones = data.asignaciones.filter(item => item.activa !== false);
        const seccionesAsignadas = new Set(this.asignaciones.map(item => item.seccionId));
        const cursosAsignados = new Set(this.asignaciones.map(item => item.cursoId));
        this.secciones = data.secciones.filter(item => item.activa !== false
          && item.nivelNombre === 'SECUNDARIA' && seccionesAsignadas.has(item.id));
        this.cursos = data.cursos.filter(item => item.estado === 'ACTIVO' && cursosAsignados.has(item.id));
        this.docentes = data.docentes.filter(item => item.estado === 'ACTIVO');
        this.estadosBimestre = data.bimestres;
        this.loading = false;
        // The selection and load will be triggered by navigating via Cajones
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se pudieron cargar los catálogos académicos.';
        this.cdr.detectChanges();
      }
    });
  }

  private actualizarDocenteAsignado(): void {
    const asignacion = this.asignaciones.find(item => item.seccionId === Number(this.currentSeccionId)
      && item.cursoId === Number(this.currentCursoId) && item.activa !== false);
    this.currentDocenteId = asignacion?.docenteId ?? null;
  }

  private seleccionarCursoCompatible(): void {
    if (!this.cursosDisponibles.some(item => item.id === Number(this.currentCursoId))) {
      this.currentCursoId = this.cursosDisponibles[0]?.id ?? null;
    }
  }

  private cargarAuditoria(): void {
    if (!this.esDirector && !this.esCoordinadorAcademico) {
      this.auditoriaBimestre = [];
      return;
    }
    this.notaService.auditoriaBimestre(this.anioEscolar, this.currentBimestre).subscribe({
      next: eventos => {
        this.auditoriaBimestre = eventos;
        this.cdr.detectChanges();
      },
      error: () => {
        this.auditoriaBimestre = [];
        this.cdr.detectChanges();
      }
    });
  }
}
