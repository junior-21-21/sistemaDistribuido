import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Estudiante {
  id?: number;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  telefono: string;
  gradoId: number;
  seccionId: number;
  estado?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EstudianteService {
  private apiUrl = '/api/estudiantes';

  constructor(private http: HttpClient) { }

  getEstudiantes(): Observable<Estudiante[]> {
    return this.http.get<Estudiante[]>(this.apiUrl);
  }

  createEstudiante(estudiante: Estudiante): Observable<Estudiante> {
    return this.http.post<Estudiante>(this.apiUrl, estudiante);
  }
}
