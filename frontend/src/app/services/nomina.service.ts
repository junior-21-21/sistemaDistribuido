import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EstadoNomina {
  seccionId: number;
  anioEscolar: number;
  estudiantes: number;
  aprobada: boolean;
  desactualizada: boolean;
  totalAprobado: number | null;
  aprobadaPor: string | null;
  fechaAprobacion: string | null;
  observacion: string | null;
}

export interface FilaNomina {
  numero: number;
  estudianteId: number;
  dni: string;
  apellidos: string;
  nombres: string;
}

@Injectable({ providedIn: 'root' })
export class NominaService {
  private readonly http = inject(HttpClient);

  estado(anio: number): Observable<EstadoNomina[]> {
    return this.http.get<EstadoNomina[]>('/api/nominas', { params: { anio } });
  }

  detalle(seccionId: number): Observable<FilaNomina[]> {
    return this.http.get<FilaNomina[]>(`/api/nominas/${seccionId}/estudiantes`);
  }

  aprobar(seccionId: number, anio: number, observacion: string): Observable<EstadoNomina> {
    return this.http.post<EstadoNomina>(`/api/nominas/${seccionId}/aprobar`, { observacion }, { params: { anio } });
  }
}
