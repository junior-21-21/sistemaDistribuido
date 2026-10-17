import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HorarioDTO {
  id?: number;
  diaSemana: string;
  horaInicio: string; // HH:mm:ss
  horaFin: string; // HH:mm:ss
  turno: string;
  aulaId: number;
  seccionId: number;
  cursoId: number;
  docenteId: number;
  cursoNombre?: string;
  docenteNombre?: string;
}

export interface CursoHorario {
  id: number;
  nombre: string;
  gradoId: number;
  estado: string;
}

export interface DocenteHorario {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  especialidad: string;
  estado: string;
  horasSemanalesContratadas?: number;
  username?: string;
}

export interface AsignacionAcademica {
  id?: number;
  cursoId: number;
  cursoNombre?: string;
  docenteId: number;
  docenteNombre?: string;
  seccionId: number;
  seccionNombre?: string;
  horasSemanales: number;
  anioEscolar: number;
  activa?: boolean;
  horasContratadasDocente?: number;
  horasAsignadasDocente?: number;
  horasDisponiblesDocente?: number;
}

export interface AulaHorario {
  id: number;
  nombre: string;
  activa: boolean;
  turnoDisponible: string;
}

export interface SeccionHorario {
  id: number;
  nombre: string;
  gradoId: number;
  gradoNombre: string;
  nivelNombre: string;
  turno: string;
  activa: boolean;
}

export interface RepartoLinea {
  seccionId: number;
  seccion: string;
  cursoId: number;
  curso: string;
  horas: number;
  docenteId: number;
  docente: string;
}

export interface RepartoResultado {
  aplicado: boolean;
  plan: RepartoLinea[];
  cargas: { docenteId: number; docente: string; antes: number; despues: number; contratadas: number }[];
  sinAsignar: string[];
  horariosActualizados: number;
  horariosLiberados: number;
}

@Injectable({
  providedIn: 'root'
})
export class HorarioService {
  private apiUrl = '/api/horarios';

  constructor(private http: HttpClient) {}

  listarPorSeccion(seccionId: number): Observable<HorarioDTO[]> {
    return this.http.get<HorarioDTO[]>(`${this.apiUrl}/seccion/${seccionId}`);
  }

  listarPorDocente(docenteId: number): Observable<HorarioDTO[]> {
    return this.http.get<HorarioDTO[]>(`${this.apiUrl}/docente/${docenteId}`);
  }

  guardar(horario: HorarioDTO): Observable<HorarioDTO> {
    return this.http.post<HorarioDTO>(this.apiUrl, horario);
  }

  listarMisHorarios(): Observable<HorarioDTO[]> {
    return this.http.get<HorarioDTO[]>(`${this.apiUrl}/mios`);
  }

  reemplazarSeccion(seccionId: number, horarios: HorarioDTO[]): Observable<HorarioDTO[]> {
    return this.http.put<HorarioDTO[]>(`${this.apiUrl}/seccion/${seccionId}`, horarios);
  }

  /** Guarda varias secciones en una sola operación (todo o nada). */
  reemplazarVarias(secciones: { seccionId: number; bloques: HorarioDTO[] }[]): Observable<Record<string, HorarioDTO[]>> {
    return this.http.put<Record<string, HorarioDTO[]>>(`${this.apiUrl}/lote`, secciones);
  }

  /** Reparte un curso entre varios docentes (simular = solo propuesta). */
  repartir(solicitud: {
    anioEscolar: number;
    docenteIds: number[];
    items: { seccionId: number; cursoId: number; horasSemanales: number }[];
    simular: boolean;
  }): Observable<RepartoResultado> {
    return this.http.post<RepartoResultado>('/api/asignaciones-academicas/reparto', solicitud);
  }

  listarCursos(): Observable<CursoHorario[]> {
    return this.http.get<CursoHorario[]>('/api/cursos');
  }

  listarDocentes(): Observable<DocenteHorario[]> {
    return this.http.get<DocenteHorario[]>('/api/docentes');
  }

  listarAulas(): Observable<AulaHorario[]> {
    return this.http.get<AulaHorario[]>('/api/aulas');
  }

  listarSecciones(): Observable<SeccionHorario[]> {
    return this.http.get<SeccionHorario[]>('/api/secciones');
  }

  listarAsignaciones(anioEscolar: number): Observable<AsignacionAcademica[]> {
    return this.http.get<AsignacionAcademica[]>('/api/asignaciones-academicas', {
      params: { anioEscolar }
    });
  }

  listarMisAsignaciones(anioEscolar: number): Observable<AsignacionAcademica[]> {
    return this.http.get<AsignacionAcademica[]>('/api/asignaciones-academicas/mias', {
      params: { anioEscolar }
    });
  }

  guardarAsignacion(asignacion: AsignacionAcademica): Observable<AsignacionAcademica> {
    return asignacion.id
      ? this.http.put<AsignacionAcademica>(`/api/asignaciones-academicas/${asignacion.id}`, asignacion)
      : this.http.post<AsignacionAcademica>('/api/asignaciones-academicas', asignacion);
  }

  eliminarAsignacion(id: number): Observable<void> {
    return this.http.delete<void>(`/api/asignaciones-academicas/${id}`);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- ENDPOINTS MOTOR DE IA (TIMEFOLD) ---

  guardarParametros(parametros: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/parametros`, parametros, { responseType: 'text' });
  }

  generarHorarioIA(): Observable<any> {
    return this.http.post(`${this.apiUrl}/generar`, {}, { responseType: 'text' });
  }

  consultarEstadoIA(): Observable<any> {
    return this.http.get(`${this.apiUrl}/estado`, { responseType: 'text' });
  }

  obtenerResultadoIA(): Observable<any> {
    return this.http.get(`${this.apiUrl}/resultado`);
  }
}

