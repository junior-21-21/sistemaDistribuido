import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

interface Derivacion {
  id: number;
  estudianteId: number;
  docenteDerivadorId: number;
  motivo: string;
  descripcion: string;
  prioridad: string;
  estado: string;
  fechaDerivacion: string;
  fechaAtencion?: string;
  notasPsicologo?: string;
}

@Component({
  selector: 'app-derivaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './derivaciones.html',
  styleUrl: './derivaciones.css'
})
export class Derivaciones implements OnInit {
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  derivaciones: Derivacion[] = [];
  loading = true;
  error = '';
  isDirector = false;
  
  showModal = false;
  submitting = false;
  nuevaDerivacion = {
    estudianteId: 1,
    motivo: 'RENDIMIENTO',
    prioridad: 'NORMAL',
    descripcion: ''
  };

  estudiantesMock = [
    { id: 1, nombre: 'Andres Suni Mamani' },
    { id: 2, nombre: 'Natalia Ayala Rojas' },
    { id: 3, nombre: 'Emilio Huaman Torres' },
    { id: 4, nombre: 'Daniela Palacios Quispe' }
  ];

  constructor(private authService: AuthService) {
    this.isDirector = this.authService.hasRole('DIRECTOR');
  }

  ngOnInit() {
    this.cargarDerivaciones();
  }

  cargarDerivaciones() {
    this.loading = true;
    this.http.get<Derivacion[]>('/api/derivaciones').subscribe({
      next: (data) => {
        this.derivaciones = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo conectar al servidor de tutoría.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirModal() {
    this.showModal = true;
    this.nuevaDerivacion.descripcion = '';
  }

  cerrarModal() {
    this.showModal = false;
  }

  guardarDerivacion() {
    if (!this.nuevaDerivacion.descripcion.trim()) return;
    this.submitting = true;
    
    const payload = {
      ...this.nuevaDerivacion,
      docenteDerivadorId: 2
    };

    this.http.post<Derivacion>('/api/derivaciones', payload).subscribe({
      next: (derivacion) => {
        this.derivaciones.unshift(derivacion);
        this.submitting = false;
        this.cerrarModal();
        this.cdr.detectChanges();
      },
      error: () => {
        this.submitting = false;
        alert('Error al crear la derivación.');
        this.cdr.detectChanges();
      }
    });
  }

  getEstudianteNombre(id: number) {
    const est = this.estudiantesMock.find((e: any) => e.id == id);
    return est ? est.nombre : `Estudiante ${id}`;
  }

  getBadgeClass(prioridad: string) {
    if (prioridad === 'URGENTE') return 'bg-danger';
    return 'bg-info text-dark';
  }
}
