import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FichaMedica {
  id?: number;
  estudianteId: number;
  tipoSangre: string;
  alergias?: string;
  condicionesCronicas?: string;
  medicamentosHabituales?: string;
  discapacidades?: string;
  observacionesGenerales?: string;
  tipoSeguro?: string;
  nroSeguro?: string;
  contactoEmergenciaNombre?: string;
  contactoEmergenciaTelefono?: string;
  contactoEmergenciaParentesco?: string;
  fechaCreacion?: string;
  fechaActualizacion?: string;
  ultimaActualizacionPor?: string;
}

@Injectable({ providedIn: 'root' })
export class TopicoService {
  private http = inject(HttpClient);

  getFichaPorEstudiante(estudianteId: number): Observable<FichaMedica> {
    return this.http.get<FichaMedica>(`/api/fichas-medicas/estudiante/${estudianteId}`);
  }

  crearFicha(ficha: FichaMedica): Observable<FichaMedica> {
    return this.http.post<FichaMedica>('/api/fichas-medicas', ficha);
  }

  actualizarFicha(id: number, ficha: FichaMedica): Observable<FichaMedica> {
    return this.http.put<FichaMedica>(`/api/fichas-medicas/${id}`, ficha);
  }

  listarFichas(): Observable<FichaMedica[]> {
    return this.http.get<FichaMedica[]>('/api/fichas-medicas');
  }
}
