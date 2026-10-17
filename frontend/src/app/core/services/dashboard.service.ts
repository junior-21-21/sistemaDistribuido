import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardStats {
  totalEstudiantes: number;
  docentesActivos: number;
  matriculasPendientes: number;
  alertasDesercion: number;
  mensaje: string;
}

export interface Actividad {
  id: number;
  texto: string;
  tipo: string;
  fecha: string;
  origen: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = '/api/estadisticas/dashboard';

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.apiUrl);
  }

  getActividadesRecientes(): Observable<Actividad[]> {
    return this.http.get<Actividad[]>('/api/actividades/recientes');
  }
}
