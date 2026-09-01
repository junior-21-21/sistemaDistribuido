import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface Ticket {
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
  imports: [CommonModule, FormsModule],
  templateUrl: './tickets.html',
  styleUrl: './tickets.css'
})
export class Tickets implements OnInit {
  private cdr = inject(ChangeDetectorRef);

  tickets: Ticket[] = [];
  loading = true;
  error = '';
  
  showModal = false;
  submitting = false;
  nuevoTicket = {
    aulaId: 101,
    categoria: 'MOBILIARIO',
    urgencia: 'MEDIA',
    descripcion: ''
  };

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.cargarTickets();
  }

  cargarTickets() {
    this.loading = true;
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: (data) => {
        this.tickets = data;
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
    this.showModal = true;
    this.nuevoTicket.descripcion = '';
  }

  cerrarModal() {
    this.showModal = false;
  }

  guardarTicket() {
    if (!this.nuevoTicket.descripcion.trim()) return;
    this.submitting = true;
    
    const payload = {
      ...this.nuevoTicket,
      docenteId: 2 // mock docente ID para la prueba
    };

    this.http.post<Ticket>('/api/tickets', payload).subscribe({
      next: (ticket) => {
        this.tickets.unshift(ticket);
        this.submitting = false;
        this.cerrarModal();
        this.cdr.detectChanges();
      },
      error: () => {
        this.submitting = false;
        alert('Error al crear el ticket de mantenimiento.');
        this.cdr.detectChanges();
      }
    });
  }

  getBadgeClass(urgencia: string) {
    if (urgencia === 'EMERGENCIA' || urgencia === 'ALTA') return 'bg-danger';
    if (urgencia === 'MEDIA') return 'bg-warning text-dark';
    return 'bg-info text-dark';
  }
}
