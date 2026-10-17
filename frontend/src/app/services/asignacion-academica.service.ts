import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AsignacionAcademica {
  id?: number;
  cursoId: number;
  docenteId: number;
  seccionId: number;
  horasSemanales?: number;
  anioEscolar: number;
  estado?: string;
  cursoNombre?: string;
  docenteNombre?: string;
  seccionNombre?: string;
  gradoId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AsignacionAcademicaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/asignaciones-academicas`;

  listar(seccionId?: number, anioEscolar?: number): Observable<AsignacionAcademica[]> {
    let params = new HttpParams();
    if (seccionId) params = params.set('seccionId', seccionId.toString());
    if (anioEscolar) params = params.set('anioEscolar', anioEscolar.toString());
    
    return this.http.get<AsignacionAcademica[]>(this.apiUrl, { params });
  }

  crear(asignacion: AsignacionAcademica): Observable<AsignacionAcademica> {
    return this.http.post<AsignacionAcademica>(this.apiUrl, asignacion);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
