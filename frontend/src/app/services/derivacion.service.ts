import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DerivacionPsicologica {
  id?: number;
  estudianteId: number;
  docenteId?: number;
  motivo: string;
  descripcion: string;
  estado?: string;
  fechaDerivacion?: string;
  fechaSeguimiento?: string;
  nombreEstudiante?: string;
}

@Injectable({ providedIn: 'root' })
export class DerivacionService {
  private http = inject(HttpClient);

  getDerivaciones(): Observable<DerivacionPsicologica[]> {
    return this.http.get<DerivacionPsicologica[]>('/api/derivaciones');
  }

  getDerivacionesPorEstudiante(estudianteId: number): Observable<DerivacionPsicologica[]> {
    return this.http.get<DerivacionPsicologica[]>(`/api/derivaciones/estudiante/${estudianteId}`);
  }

  crearDerivacion(derivacion: DerivacionPsicologica): Observable<DerivacionPsicologica> {
    return this.http.post<DerivacionPsicologica>('/api/derivaciones', derivacion);
  }

  actualizarEstado(id: number, estado: string): Observable<DerivacionPsicologica> {
    return this.http.patch<DerivacionPsicologica>(`/api/derivaciones/${id}/estado`, { estado });
  }
}
