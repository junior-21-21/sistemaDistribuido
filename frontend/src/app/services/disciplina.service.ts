import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReporteDisciplinario {
  id?: number;
  estudianteId: number;
  docenteId?: number;
  seccionId: number;
  tipo: string;
  descripcion: string;
  estado?: string;
  apoderadoNotificado?: boolean;
  fechaReporte?: string;
  observaciones?: string;
  nombreEstudiante?: string;
  registradoPor?: string;
  codigoSiseve?: string | null;
  fechaSiseve?: string | null;
  citacionApoderado?: string | null;
  medidasProteccion?: string | null;
  derivacionExterna?: string | null;
  fechaDerivacion?: string | null;
  protocoloActualizadoPor?: string | null;
}

/** Protocolo de atención de violencia escolar: registro en SíseVe, citación, medidas y derivación. */
export interface ProtocoloViolencia {
  codigoSiseve?: string | null;
  fechaSiseve?: string | null;
  citacionApoderado?: string | null;
  medidasProteccion?: string | null;
  derivacionExterna?: string | null;
  fechaDerivacion?: string | null;
}

@Injectable({ providedIn: 'root' })
export class DisciplinaService {
  private http = inject(HttpClient);

  getReportesPorSeccion(seccionId: number): Observable<ReporteDisciplinario[]> {
    return this.http.get<ReporteDisciplinario[]>(`/api/reportes-disciplinarios?seccionId=${seccionId}`);
  }

  getReportesPorEstudiante(estudianteId: number): Observable<ReporteDisciplinario[]> {
    return this.http.get<ReporteDisciplinario[]>(`/api/reportes-disciplinarios/estudiante/${estudianteId}`);
  }

  crearReporte(reporte: ReporteDisciplinario): Observable<ReporteDisciplinario> {
    return this.http.post<ReporteDisciplinario>('/api/reportes-disciplinarios', reporte);
  }

  actualizarProtocolo(id: number, datos: ProtocoloViolencia): Observable<ReporteDisciplinario> {
    return this.http.patch<ReporteDisciplinario>(`/api/reportes-disciplinarios/${id}/protocolo`, datos);
  }

  actualizarEstado(id: number, estado: string): Observable<ReporteDisciplinario> {
    return this.http.patch<ReporteDisciplinario>(`/api/reportes-disciplinarios/${id}/estado`, { estado });
  }
}
