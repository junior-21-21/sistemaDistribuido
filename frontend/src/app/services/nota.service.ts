import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs';
import { PageResponse } from './estudiante.service';

export interface Nota {
  id?: number;
  estudianteId: number;
  cursoId: number;
  docenteId: number;
  seccionId: number;
  escalaLiteral?: string;      // AD, A, B, C
  valorNumerico?: number;      // 0-20
  competencia: string;
  bimestre: string;
  periodoAnio?: string;
  observaciones?: string;
  fechaRegistro?: string;
  fechaModificacion?: string;
}

export interface Asistencia {
  id?: number;
  estudianteId: number;
  horarioId: number;
  docenteId: number;
  seccionId: number;
  cursoId: number;
  fecha: string;
  estado: string;   // PRESENTE, FALTA, TARDE, JUSTIFICADO
  turno?: string;
  justificacion?: string;
  registradoPor?: string;
}

export interface SesionAsistencia {
  id?: number;
  horarioId: number;
  seccionId: number;
  cursoId: number;
  docenteId: number;
  fecha: string;
  estado?: 'ABIERTA' | 'CERRADA';
  abiertaPor?: string;
  cerradaPor?: string;
  fechaApertura?: string;
  fechaCierre?: string;
}

export interface BimestreEstado {
  anio: number;
  bimestre: string;
  estado: 'ABIERTO' | 'CERRADO';
  actualizadoPor?: string;
  actualizadoEn?: string;
}

export interface PonderacionColumna {
  competencia: string;
  peso: number;
}

export interface Ponderaciones {
  seccionId: number;
  cursoId: number;
  docenteId?: number;
  bimestre: string;
  periodoAnio?: string;
  columnas: PonderacionColumna[];
}

export interface BimestreAuditoria {
  accion: 'CIERRE' | 'REAPERTURA';
  motivo: string;
  actor: string;
  ocurridoEn: string;
}

@Injectable({ providedIn: 'root' })
export class NotaService {
  private http = inject(HttpClient);

  getNotasPorSeccion(seccionId: number, bimestre?: string, cursoId?: number): Observable<Nota[]> {
    let params = new HttpParams().set('seccionId', seccionId);
    if (bimestre) params = params.set('bimestre', bimestre);
    if (cursoId) params = params.set('cursoId', cursoId);
    params = params.set('size', 2000);
    return this.http.get<PageResponse<Nota>>('/api/notas/page', { params }).pipe(map(page => page.content));
  }

  getNotasPorEstudiante(estudianteId: number): Observable<Nota[]> {
    return this.http.get<Nota[]>(`/api/notas/estudiante/${estudianteId}`);
  }

  guardarNota(nota: Nota): Observable<Nota> {
    return nota.id
      ? this.http.put<Nota>(`/api/notas/${nota.id}`, nota)
      : this.http.post<Nota>('/api/notas', nota);
  }

  guardarNotasMasivas(notas: Nota[]): Observable<Nota[]> {
    return this.http.post<Nota[]>('/api/notas/bulk', notas);
  }

  getPonderaciones(seccionId: number, cursoId: number, bimestre: string, periodoAnio: number): Observable<Ponderaciones> {
    const params = new HttpParams().set('seccionId', seccionId).set('cursoId', cursoId)
      .set('bimestre', bimestre).set('periodoAnio', periodoAnio);
    return this.http.get<Ponderaciones>('/api/notas/ponderaciones', { params });
  }

  guardarPonderaciones(ponderaciones: Ponderaciones): Observable<Ponderaciones> {
    return this.http.put<Ponderaciones>('/api/notas/ponderaciones', ponderaciones);
  }

  eliminarColumna(contexto: { seccionId: number; cursoId: number; docenteId: number; bimestre: string;
                             periodoAnio: number; competencia: string }): Observable<{ notasEliminadas: number }> {
    let params = new HttpParams();
    Object.entries(contexto).forEach(([clave, valor]) => params = params.set(clave, valor));
    return this.http.delete<{ notasEliminadas: number }>('/api/notas/columna', { params });
  }

  listarBimestres(anio: number): Observable<BimestreEstado[]> {
    return this.http.get<BimestreEstado[]>('/api/bimestres', {
      params: new HttpParams().set('anio', anio)
    });
  }

  cerrarBimestre(anio: number, bimestre: string, motivo: string): Observable<BimestreEstado> {
    return this.http.post<BimestreEstado>(
      `/api/bimestres/${anio}/${this.periodoUrl(bimestre)}/cerrar`, { motivo });
  }

  reabrirBimestre(anio: number, bimestre: string, motivo: string): Observable<BimestreEstado> {
    return this.http.post<BimestreEstado>(
      `/api/bimestres/${anio}/${this.periodoUrl(bimestre)}/reabrir`, { motivo });
  }

  auditoriaBimestre(anio: number, bimestre: string): Observable<BimestreAuditoria[]> {
    return this.http.get<BimestreAuditoria[]>(
      `/api/bimestres/${anio}/${this.periodoUrl(bimestre)}/auditoria`);
  }

  getAsistenciaHoy(seccionId: number, cursoId: number): Observable<Asistencia[]> {
    const params = new HttpParams()
      .set('seccionId', seccionId)
      .set('cursoId', cursoId);
    return this.http.get<Asistencia[]>('/api/asistencias/hoy', { params });
  }

  getAsistenciasPorFecha(seccionId: number, fecha: string): Observable<Asistencia[]> {
    const params = new HttpParams().set('seccionId', seccionId).set('fecha', fecha);
    return this.http.get<Asistencia[]>('/api/asistencias', { params });
  }

  /** Asistencia de una sección (y de un curso, si se indica) entre dos fechas: reportes de día, semana o mes. */
  getAsistenciasPorRango(seccionId: number, desde: string, hasta: string, cursoId?: number | null): Observable<Asistencia[]> {
    let params = new HttpParams().set('seccionId', seccionId).set('desde', desde).set('hasta', hasta);
    if (cursoId) params = params.set('cursoId', cursoId);
    return this.http.get<Asistencia[]>('/api/asistencias/rango', { params });
  }

  registrarAsistencia(asistencia: Asistencia): Observable<Asistencia> {
    return this.http.post<Asistencia>('/api/asistencias', asistencia);
  }

  registrarAsistenciaMasiva(asistencias: Asistencia[]): Observable<Asistencia[]> {
    return this.http.post<Asistencia[]>('/api/asistencias/bulk', asistencias);
  }

  obtenerSesion(horarioId: number, fecha: string): Observable<SesionAsistencia> {
    return this.http.get<SesionAsistencia>('/api/asistencias/sesiones', {
      params: new HttpParams().set('horarioId', horarioId).set('fecha', fecha)
    });
  }

  abrirSesion(sesion: SesionAsistencia): Observable<SesionAsistencia> {
    return this.http.post<SesionAsistencia>('/api/asistencias/sesiones', sesion);
  }

  cerrarSesion(id: number): Observable<SesionAsistencia> {
    return this.http.post<SesionAsistencia>(`/api/asistencias/sesiones/${id}/cerrar`, {});
  }

  private periodoUrl(bimestre: string): string {
    return bimestre.trim().split(/\s+/)[0];
  }
}
