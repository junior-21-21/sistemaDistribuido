import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs';
import { PageResponse } from './estudiante.service';

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

export interface AtencionMedica {
  id: number;
  fichaMedicaId: number;
  estudianteId: number;
  motivo: string;
  diagnostico: string;
  tratamiento: string;
  fechaAtencion: string;
  registradoPor: string;
  apoderadoAvisado?: boolean;
  destino?: DestinoAtencion;
  establecimientoSalud?: string | null;
  cubiertoSis?: boolean | null;
}

export type DestinoAtencion = 'RETORNA_AULA' | 'RETIRADO_APODERADO' | 'TRASLADO_SALUD';

export interface CrearAtencionMedica {
  fichaMedicaId: number;
  motivo: string;
  diagnostico: string;
  tratamiento: string;
  apoderadoAvisado?: boolean;
  destino?: DestinoAtencion;
  establecimientoSalud?: string | null;
  cubiertoSis?: boolean | null;
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

  listarAtenciones(fichaMedicaId: number): Observable<AtencionMedica[]> {
    return this.http.get<PageResponse<AtencionMedica>>(`/api/atenciones-medicas/ficha/${fichaMedicaId}/page`)
      .pipe(map(page => page.content));
  }

  registrarAtencion(atencion: CrearAtencionMedica): Observable<AtencionMedica> {
    return this.http.post<AtencionMedica>('/api/atenciones-medicas', atencion);
  }
}
