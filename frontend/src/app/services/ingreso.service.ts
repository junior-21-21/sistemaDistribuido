import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Ingreso {
  id: number;
  estudianteId: number;
  nombre: string;
  apellido: string;
  dniFinal: string;
  gradoId: number;
  seccionId: number;
  fecha: string;
  hora: string;
  turno: 'MANANA' | 'TARDE';
  estado: 'PUNTUAL' | 'TARDANZA';
  minutosTarde: number;
  metodo: 'ESCANER' | 'CAMARA' | 'MANUAL';
  yaRegistrado: boolean;
}

export interface ResumenIngresos {
  fecha: string;
  estudiantesActivos: number;
  ingresaron: number;
  puntuales: number;
  tardanzas: number;
  recientes: Ingreso[];
}

@Injectable({ providedIn: 'root' })
export class IngresoService {
  private readonly http = inject(HttpClient);

  registrar(codigo: string, metodo: 'ESCANER' | 'CAMARA' | 'MANUAL'): Observable<Ingreso> {
    return this.http.post<Ingreso>('/api/ingresos', { codigo, metodo });
  }

  resumenHoy(): Observable<ResumenIngresos> {
    return this.http.get<ResumenIngresos>('/api/ingresos/hoy');
  }
}
