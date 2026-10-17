import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AsignacionAcademicaService, AsignacionAcademica } from '../../services/asignacion-academica.service';
import { SeccionService } from '../../core/services/seccion.service';
import { DocenteService, Docente } from '../../services/docente.service';
import { CursoService, Curso } from '../../services/curso.service';
import { Seccion } from '../../core/models/seccion.model';
import { HorarioService, HorarioDTO, AulaHorario } from '../../services/horario.service';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { LiveBanner } from '../../shared/live-banner/live-banner';

@Component({
  selector: 'app-gestor-aula',
  standalone: true,
  imports: [LiveBanner, CommonModule, FormsModule],
  templateUrl: './gestor-aula.html',
  styleUrl: './gestor-aula.css'
})
export class GestorAula implements OnInit {
  route = inject(ActivatedRoute);
  http = inject(HttpClient);
  asignacionService = inject(AsignacionAcademicaService);
  seccionService = inject(SeccionService);
  docenteService = inject(DocenteService);
  cursoService = inject(CursoService);
  horarioService = inject(HorarioService);

  seccionId: number | null = null;
  seccion: Seccion | null = null;
  asignaciones: AsignacionAcademica[] = [];
  
  docentes: Docente[] = [];
  cursos: Curso[] = [];
  
  horarios: HorarioDTO[] = [];
  aulas: AulaHorario[] = [];
  diasSemana = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
  bloquesHora = [
    { inicio: '08:00:00', fin: '08:45:00', etiqueta: '08:00 - 08:45' },
    { inicio: '08:45:00', fin: '09:30:00', etiqueta: '08:45 - 09:30' },
    { inicio: '09:30:00', fin: '10:15:00', etiqueta: '09:30 - 10:15' },
    { inicio: '10:15:00', fin: '10:45:00', etiqueta: 'RECREO' },
    { inicio: '10:45:00', fin: '11:30:00', etiqueta: '10:45 - 11:30' },
    { inicio: '11:30:00', fin: '12:15:00', etiqueta: '11:30 - 12:15' },
    { inicio: '12:15:00', fin: '13:00:00', etiqueta: '12:15 - 13:00' },
    { inicio: '13:00:00', fin: '13:45:00', etiqueta: '13:00 - 13:45' }
  ];
  
  nuevaAsignacion: { cursoId: number | null; docenteId: number | null; horasSemanales: number } =
    { cursoId: null, docenteId: null, horasSemanales: 2 };
  nuevoHorario = { asignacionId: null }; // ID de la asignacion academica
  cargando = true;

  // Nav
  vistaActual: 'grados' | 'secciones' | 'gestor' = 'grados';
  seccionesBase: any[] = [];
  gradoNavegacion: any = null;

  ngOnInit() {
    this.cargarSeccionesBase();
    this.route.paramMap.subscribe(params => {
      const id = params.get('seccionId');
      if (id) {
        this.seccionId = +id;
        this.vistaActual = 'gestor';
        this.cargarDatos();
      }
    });
  }

  cargarSeccionesBase(): void {
    this.http.get<any[]>('/api/secciones').subscribe({
      next: (data) => {
        this.seccionesBase = data;
        this.cargando = false;
      },
      error: () => this.cargando = false
    });
  }

  get gradosDisponibles(): any[] {
    const secs = this.seccionesBase.filter(s => s.nivelNombre === 'SECUNDARIA');
    const uniqueMap = new Map();
    secs.forEach(s => {
      if (s.gradoId && !uniqueMap.has(s.gradoId)) {
        uniqueMap.set(s.gradoId, { id: s.gradoId, nombre: s.gradoNombre });
      }
    });
    return Array.from(uniqueMap.values()).sort((a,b) => a.id - b.id);
  }

  navegarAGrado(grado: any): void {
    this.gradoNavegacion = grado;
    this.vistaActual = 'secciones';
  }

  navegarASeccion(seccion: any): void {
    this.seccionId = seccion.id;
    this.vistaActual = 'gestor';
    this.cargarDatos();
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

  getSeccionesDelGradoNav(): any[] {
    if (!this.gradoNavegacion) return [];
    return this.seccionesBase.filter(s => s.gradoId?.toString() === this.gradoNavegacion.id?.toString());
  }

  getSeccionesMananaNav(): any[] {
    return this.getSeccionesDelGradoNav().filter(s => s.turno === 'MANANA');
  }

  getSeccionesTardeNav(): any[] {
    return this.getSeccionesDelGradoNav().filter(s => s.turno === 'TARDE');
  }

  cargarDatos() {
    if (!this.seccionId) return;
    this.cargando = true;
    forkJoin({
      seccion: this.seccionService.getSeccion(this.seccionId),
      asignaciones: this.asignacionService.listar(this.seccionId),
      docentes: this.docenteService.listarTodos(),
      cursos: this.cursoService.listarTodos(),
      horarios: this.horarioService.listarPorSeccion(this.seccionId),
      aulas: this.horarioService.listarAulas()
    }).subscribe({
      next: (res) => {
        this.seccion = res.seccion;
        this.asignaciones = res.asignaciones;
        this.docentes = res.docentes;
        this.cursos = res.cursos;
        this.horarios = res.horarios;
        this.aulas = res.aulas;
        this.cargando = false;
      },
      error: (err) => {
        console.error(err);
        this.cargando = false;
      }
    });
  }

  getNombreDocente(id: number): string {
    const doc = this.docentes.find(d => d.id === id);
    return doc ? `${doc.nombre} ${doc.apellido}` : 'Desconocido';
  }
  
  getNombreCurso(id: number): string {
    const cur = this.cursos.find(c => c.id === id);
    return cur ? cur.nombre : 'Desconocido';
  }

  asignar() {
    if (!this.nuevaAsignacion.cursoId || !this.nuevaAsignacion.docenteId || !this.seccionId) return;
    
    const dto: AsignacionAcademica = {
      cursoId: this.nuevaAsignacion.cursoId,
      docenteId: this.nuevaAsignacion.docenteId,
      seccionId: this.seccionId,
      horasSemanales: this.nuevaAsignacion.horasSemanales,
      anioEscolar: new Date().getFullYear()
    };
    
    this.asignacionService.crear(dto).subscribe({
      next: (creado) => {
        this.asignaciones.push(creado);
        this.nuevaAsignacion = { cursoId: null, docenteId: null, horasSemanales: 2 };
      },
      error: (err) => alert(err?.error?.message || 'No se pudo asignar. Quizás el curso ya está asignado en esta sección.')
    });
  }

  eliminar(id: number) {
    if (confirm('¿Eliminar asignación?')) {
      this.asignacionService.eliminar(id).subscribe({
        next: () => this.asignaciones = this.asignaciones.filter(a => a.id !== id),
        error: () => alert('No se pudo eliminar la asignación.')
      });
    }
  }

  getHorarioBloque(dia: string, horaInicio: string): HorarioDTO | undefined {
    return this.horarios.find(h => h.diaSemana === dia && h.horaInicio === horaInicio);
  }

  asignarHorarioCargando = false;
  asignarHorario(dia: string, horaInicio: string, horaFin: string, evt: any) {
    const asignacionId = evt.target.value;
    if (!asignacionId || !this.seccionId) return;
    
    const asignacion = this.asignaciones.find(a => a.id == asignacionId);
    if (!asignacion) return;

    const turno = this.seccion?.turno || 'MANANA';
    const aula = this.aulas.find(a => a.activa && (a.turnoDisponible === turno || a.turnoDisponible === 'AMBOS'));
    if (!aula) {
      alert('No hay un aula activa disponible para el turno de esta sección.');
      evt.target.value = '';
      return;
    }

    const dto: HorarioDTO = {
      diaSemana: dia,
      horaInicio: horaInicio,
      horaFin: horaFin,
      turno: this.seccion?.turno || 'MANANA',
      aulaId: aula.id,
      seccionId: this.seccionId,
      cursoId: asignacion.cursoId,
      docenteId: asignacion.docenteId
    };

    evt.target.disabled = true;
    this.horarioService.guardar(dto).subscribe({
      next: (h) => {
        this.horarios.push(h);
        evt.target.disabled = false;
        evt.target.value = '';
      },
      error: () => {
        alert('Error al guardar horario. Es posible que el docente o el curso ya tengan clases a esta hora.');
        evt.target.disabled = false;
        evt.target.value = '';
      }
    });
  }

  eliminarHorario(id: number) {
    if (confirm('¿Liberar este bloque de horario?')) {
      this.horarioService.eliminar(id).subscribe({
        next: () => this.horarios = this.horarios.filter(h => h.id !== id),
        error: () => alert('No se pudo liberar el bloque de horario.')
      });
    }
  }
}
