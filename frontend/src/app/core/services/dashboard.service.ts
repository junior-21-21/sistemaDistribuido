import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

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

  /** Construye actividad reciente desde endpoints reales existentes */
  getActividadesRecientes(): Observable<Actividad[]> {
    const disciplina$ = this.http.get<any[]>('/api/reportes-disciplinarios').pipe(
      catchError(() => of([]))
    );
    const estudiantes$ = this.http.get<any[]>('/api/estudiantes').pipe(
      catchError(() => of([]))
    );

    return forkJoin([disciplina$, estudiantes$]).pipe(
      map(([disciplina, estudiantes]) => {
        const actividades: Actividad[] = [];
        let idCounter = 1;

        // Últimos 3 reportes disciplinarios
        const recientes = (disciplina as any[]).slice(0, 3);
        for (const rep of recientes) {
          actividades.push({
            id: idCounter++,
            texto: `Reporte disciplinario: ${rep.descripcion || rep.tipo || 'Incidencia registrada'}`,
            tipo: 'warning',
            fecha: rep.fecha || new Date().toISOString(),
            origen: 'Disciplina'
          });
        }

        // Matriculas recientes
        const nuevos = (estudiantes as any[]).filter(e => e.estado === 'PENDIENTE').slice(0, 2);
        for (const est of nuevos) {
          actividades.push({
            id: idCounter++,
            texto: `Matrícula pendiente: ${est.nombre || est.nombreCompleto || 'Estudiante'} ${est.apellido || ''}`.trim(),
            tipo: 'info',
            fecha: est.fechaMatricula || new Date().toISOString(),
            origen: 'Matrículas'
          });
        }

        return actividades;
      }),
      catchError(() => of([]))
    );
  }
}
