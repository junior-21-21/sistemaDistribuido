import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from './estudiante.service';

export interface RevisionAuditoria {
  revision: number; fecha: string; accion: string; entidad: string; entidadId: number;
  usuario: string | null; correlationId: string | null; valores: Record<string, unknown>;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);
  buscar(filtros: Record<string, string | number>, page = 0): Observable<PageResponse<RevisionAuditoria>> {
    let params = new HttpParams().set('page', page).set('size', 25);
    Object.entries(filtros).forEach(([key, value]) => { if (value !== '') params = params.set(key, value); });
    return this.http.get<PageResponse<RevisionAuditoria>>('/api/auditoria', { params });
  }
}
