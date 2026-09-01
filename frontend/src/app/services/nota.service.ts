import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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

@Injectable({ providedIn: 'root' })
export class NotaService {
  private http = inject(HttpClient);

  getNotasPorSeccion(seccionId: number, bimestre?: string): Observable<Nota[]> {
    let params = new HttpParams().set('seccionId', seccionId);
    if (bimestre) params = params.set('bimestre', bimestre);
    return this.http.get<Nota[]>('/api/notas', { params });
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

  registrarAsistencia(asistencia: Asistencia): Observable<Asistencia> {
    return this.http.post<Asistencia>('/api/asistencias', asistencia);
  }

  registrarAsistenciaMasiva(asistencias: Asistencia[]): Observable<Asistencia[]> {
    return this.http.post<Asistencia[]>('/api/asistencias/bulk', asistencias);
  }
}
