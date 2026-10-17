import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Curso {
  id?: number;
  codigo: string;
  nombre: string;
  creditos?: number;
  tipo: string;
  gradoId: number;
  estado: string;
}

@Injectable({
  providedIn: 'root'
})
export class CursoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/cursos`;

  listarTodos(): Observable<Curso[]> {
    return this.http.get<Curso[]>(this.apiUrl);
  }

  crear(curso: Curso): Observable<Curso> {
    return this.http.post<Curso>(this.apiUrl, curso);
  }

  actualizar(id: number, curso: Curso): Observable<Curso> {
    return this.http.put<Curso>(`${this.apiUrl}/${id}`, curso);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
