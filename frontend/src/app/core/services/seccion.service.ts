import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { Seccion } from '../models/seccion.model';

@Injectable({
  providedIn: 'root'
})
export class SeccionService {
  private apiUrl = `${environment.apiUrl}/secciones`;

  constructor(private http: HttpClient) { }

  getSecciones(): Observable<Seccion[]> {
    return this.http.get<Seccion[]>(this.apiUrl);
  }

  getSeccion(id: number): Observable<Seccion> {
    return this.http.get<Seccion>(`${this.apiUrl}/${id}`);
  }

  createSeccion(seccion: Seccion): Observable<Seccion> {
    return this.http.post<Seccion>(this.apiUrl, seccion);
  }

  updateSeccion(id: number, seccion: Seccion): Observable<Seccion> {
    return this.http.put<Seccion>(`${this.apiUrl}/${id}`, seccion);
  }

  /** Secciones donde el docente que inició sesión es tutor. */
  getMisTutorias(): Observable<Seccion[]> {
    return this.http.get<Seccion[]>(`${this.apiUrl}/tutoria/mias`);
  }

  asignarTutor(id: number, docenteId: number | null): Observable<Seccion> {
    return this.http.put<Seccion>(`${this.apiUrl}/${id}/tutor`, { docenteId });
  }

  deleteSeccion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
