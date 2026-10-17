import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { AulaHorario, HorarioService } from '../../services/horario.service';
import { LiveBanner } from '../../shared/live-banner/live-banner';

export interface Ticket {
  id: number;
  docenteId: number;
  aulaId: number;
  categoria: string;
  descripcion: string;
  urgencia: string;
  estado: string;
  fechaReporte: string;
  fechaResolucion?: string;
  notasResolucion?: string;
}

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [LiveBanner, CommonModule, FormsModule],
  templateUrl: './tickets.html',
  styleUrl: './tickets.css'
})
export class Tickets implements OnInit {
  private cdr = inject(ChangeDetectorRef);

  tickets: Ticket[] = [];
  loading = true;
  error = '';
  aulas: AulaHorario[] = [];
  puedeCrear = false;
  puedeGestionar = false;
  actualizandoId: number | null = null;
  ediciones: Record<number, { estado: string; notasResolucion: string }> = {};
  
  showModal = false;
  submitting = false;
  nuevoTicket = {
    aulaId: 0,
    categoria: 'MOBILIARIO',
    urgencia: 'MEDIA',
    descripcion: ''
  };

  constructor(
    private http: HttpClient,
    private horarioService: HorarioService,
    private authService: AuthService
  ) {
    this.puedeCrear = this.authService.hasRole('DOCENTE');
    this.puedeGestionar = this.authService.hasRole('MANTENIMIENTO');
  }

  ngOnInit() {
    this.cargarTickets();
  }

  cargarTickets() {
    this.loading = true;
    forkJoin({
      tickets: this.http.get<Ticket[]>('/api/tickets'),
      aulas: this.horarioService.listarAulas()
    }).subscribe({
      next: (data) => {
        this.tickets = data.tickets;
        this.prepararEdiciones(data.tickets);
        this.aulas = data.aulas;
        this.nuevoTicket.aulaId = this.aulas[0]?.id ?? 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo conectar al servidor de mantenimiento.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirModal() {
    this.error = '';
    this.showModal = true;
    this.nuevoTicket.descripcion = '';
  }

  cerrarModal() {
    this.showModal = false;
  }

  guardarTicket() {
    if (!this.nuevoTicket.descripcion.trim()) return;
    this.submitting = true;
    
    this.http.post<Ticket>('/api/tickets', this.nuevoTicket).subscribe({
      next: (ticket) => {
        this.tickets.unshift(ticket);
        this.prepararEdiciones([ticket]);
        this.submitting = false;
        this.cerrarModal();
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.submitting = false;
        this.error = error.error?.message || 'No se pudo crear el ticket de mantenimiento.';
        this.cdr.detectChanges();
      }
    });
  }

  actualizarTicket(ticket: Ticket) {
    const edicion = this.ediciones[ticket.id];
    if (!this.puedeGestionar || !edicion || this.actualizandoId !== null) return;
    if (edicion.estado === 'RESUELTO' && !edicion.notasResolucion.trim()) {
      this.error = 'Escribe las notas de resolución antes de cerrar el ticket.';
      return;
    }
    this.actualizandoId = ticket.id;
    this.error = '';
    this.http.put<Ticket>(`/api/tickets/${ticket.id}/estado`, {
      estado: edicion.estado,
      notasResolucion: edicion.notasResolucion.trim() || null
    }).subscribe({
      next: actualizado => {
        this.tickets = this.tickets.map(item => item.id === actualizado.id ? actualizado : item);
        this.prepararEdiciones([actualizado]);
        this.actualizandoId = null;
        this.cdr.detectChanges();
      },
      error: error => {
        this.actualizandoId = null;
        this.error = error.error?.message || 'No se pudo actualizar el estado del ticket.';
        this.cdr.detectChanges();
      }
    });
  }

  getBadgeClass(urgencia: string) {
    if (urgencia === 'EMERGENCIA' || urgencia === 'ALTA') return 'bg-danger';
    if (urgencia === 'MEDIA') return 'bg-warning text-dark';
    return 'bg-info text-dark';
  }

  getAulaNombre(aulaId: number): string {
    return this.aulas.find(aula => aula.id === aulaId)?.nombre ?? `Aula ${aulaId}`;
  }

  private prepararEdiciones(tickets: Ticket[]): void {
    for (const ticket of tickets) {
      this.ediciones[ticket.id] = {
        estado: ticket.estado,
        notasResolucion: ticket.notasResolucion ?? ''
      };
    }
  }
}
