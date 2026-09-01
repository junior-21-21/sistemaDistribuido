import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Apoderado {
  id?: number;
  nombre: string;
  apellido: string;
  dni: string;
  telefono: string;
  email?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApoderadoService {
  private http = inject(HttpClient);
  private apiUrl = '/api/apoderados';

  listarTodos(): Observable<Apoderado[]> {
    return this.http.get<Apoderado[]>(this.apiUrl);
  }

  buscarPorDni(dni: string): Observable<Apoderado> {
    return this.http.get<Apoderado>(`${this.apiUrl}/dni/${dni}`);
  }

  guardar(apoderado: Apoderado): Observable<Apoderado> {
    return apoderado.id 
      ? this.http.put<Apoderado>(`${this.apiUrl}/${apoderado.id}`, apoderado)
      : this.http.post<Apoderado>(this.apiUrl, apoderado);
  }
}
