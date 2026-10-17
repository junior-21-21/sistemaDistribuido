import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Docente {
  id?: number;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  telefono?: string;
  especialidad: string;
  estado: string;
}

@Injectable({
  providedIn: 'root'
})
export class DocenteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/docentes`;

  listarTodos(): Observable<Docente[]> {
    return this.http.get<Docente[]>(this.apiUrl);
  }
}
