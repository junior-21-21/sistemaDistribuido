import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TicketMantenimiento {
  id?: number;
  aulaId: number;
  docenteId?: number;
  categoria: 'MOBILIARIO' | 'ELECTRICO' | 'PLOMERIA' | 'TECNOLOGIA' | 'OTROS';
  descripcion: string;
  urgencia: 'BAJA' | 'MEDIA' | 'ALTA' | 'EMERGENCIA';
  estado?: 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO';
  fechaReporte?: string;
  fechaResolucion?: string;
  notasResolucion?: string;
}

@Injectable({ providedIn: 'root' })
export class TicketService {
  private http = inject(HttpClient);

  getTickets(): Observable<TicketMantenimiento[]> {
    return this.http.get<TicketMantenimiento[]>('/api/tickets');
  }

  crearTicket(ticket: TicketMantenimiento): Observable<TicketMantenimiento> {
    return this.http.post<TicketMantenimiento>('/api/tickets', ticket);
  }

  actualizarEstado(id: number, estado: string, notasResolucion?: string): Observable<TicketMantenimiento> {
    return this.http.put<TicketMantenimiento>(`/api/tickets/${id}/estado`, { estado, notasResolucion });
  }
}
