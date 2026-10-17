import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from './estudiante.service';

export type EstadoPersonal = 'ACTIVO' | 'PAUSADO' | 'BAJA';

export interface PersonalData {
  id: number;
  username: string;
  nombre: string;
  apellido: string;
  email: string;
  rol: string;
  roles: string[];
  apoderadoId?: number | null;
  estado: EstadoPersonal;
  ultimoAcceso: string | null;
  debeCambiarClave: boolean;
}

export interface CrearPersonalRequest {
  username: string;
  clave: string;
  nombre: string;
  apellido: string;
  email: string;
  rol: string;
  roles: string[];
  apoderadoId?: number | null;
}

interface RestablecerClaveResponse {
  claveTemporal: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class PersonalService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/usuarios';

  listar(): Observable<PersonalData[]> {
    return this.http.get<PersonalData[]>(this.apiUrl);
  }

  listarPagina(page = 0, size = 20, q = '', rol = ''): Observable<PageResponse<PersonalData>> {
    return this.http.get<PageResponse<PersonalData>>(`${this.apiUrl}/page`, { params: { page, size, q, rol } });
  }

  crear(request: CrearPersonalRequest): Observable<PersonalData> {
    return this.http.post<PersonalData>(this.apiUrl, request);
  }

  cambiarEstado(id: number, estado: EstadoPersonal): Observable<PersonalData> {
    return this.http.patch<PersonalData>(`${this.apiUrl}/${id}/estado`, { estado });
  }

  restablecerClave(id: number): Observable<RestablecerClaveResponse> {
    return this.http.post<RestablecerClaveResponse>(`${this.apiUrl}/${id}/restablecer-clave`, {});
  }

  /** Cambia el puesto y los encargos de la cuenta (solo Dirección). */
  actualizarRoles(id: number, roles: string[]): Observable<PersonalData> {
    return this.http.put<PersonalData>(`${this.apiUrl}/${id}/roles`, { roles });
  }
}
