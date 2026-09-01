import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HorarioDTO {
  id?: number;
  diaSemana: string;
  horaInicio: string; // HH:mm:ss
  horaFin: string; // HH:mm:ss
  turno: string;
  aulaId: number;
  seccionId: number;
  cursoId: number;
  docenteId: number;
  cursoNombre?: string;
  docenteNombre?: string;
}

@Injectable({
  providedIn: 'root'
})
export class HorarioService {
  private apiUrl = '/api/horarios'; // Debe estar mapeado en proxy.conf.json a servicio-cursos (8081)

  constructor(private http: HttpClient) {}

  listarPorSeccion(seccionId: number): Observable<HorarioDTO[]> {
    return this.http.get<HorarioDTO[]>(`${this.apiUrl}/seccion/${seccionId}`);
  }

  listarPorDocente(docenteId: number): Observable<HorarioDTO[]> {
    return this.http.get<HorarioDTO[]>(`${this.apiUrl}/docente/${docenteId}`);
  }

  guardar(horario: HorarioDTO): Observable<HorarioDTO> {
    return this.http.post<HorarioDTO>(this.apiUrl, horario);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
