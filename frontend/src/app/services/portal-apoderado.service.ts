import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PortalEstudiante {
  id: number;
  nombre: string;
  apellido: string;
  gradoId: number;
  seccionId: number;
  parentesco: string;
  permisoRetiro: boolean;
}

export interface PortalResumen {
  estudiante: PortalEstudiante;
  documentos: Array<{ id: number; nombreArchivo: string; tipoDocumento: string; fechaSubida: string }>;
  notas: Array<{ cursoId?: number; tipoEvaluacion?: string; evaluacion?: string; calificacion?: number; valor?: number }>;
  asistencias: Array<{ fecha?: string; estado?: string; justificacion?: string }>;
}

@Injectable({ providedIn: 'root' })
export class PortalApoderadoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/portal-apoderado';

  estudiantes(): Observable<PortalEstudiante[]> {
    return this.http.get<PortalEstudiante[]>(`${this.baseUrl}/estudiantes`);
  }

  resumen(estudianteId: number): Observable<PortalResumen> {
    return this.http.get<PortalResumen>(`${this.baseUrl}/estudiantes/${estudianteId}/resumen`);
  }

  descargar(estudianteId: number, documentoId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/estudiantes/${estudianteId}/documentos/${documentoId}/descargar`,
      { responseType: 'blob' });
  }
}
