import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TicketMantenimiento {
  id?: number;
  aulaId?: number;
  docenteId?: number;
  titulo: string;
  descripcion: string;
  tipo: string;
  prioridad?: string;
  estado?: string;
  fechaCreacion?: string;
  fechaResolucion?: string;
  creadoPor?: string;
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

  actualizarEstado(id: number, estado: string): Observable<TicketMantenimiento> {
    return this.http.patch<TicketMantenimiento>(`/api/tickets/${id}/estado`, { estado });
  }
}
