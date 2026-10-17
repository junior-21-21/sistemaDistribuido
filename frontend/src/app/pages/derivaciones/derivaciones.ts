import { ChangeDetectorRef, Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { DerivacionPsicologica, DerivacionService } from '../../services/derivacion.service';
import { Estudiante, EstudianteService } from '../../services/estudiante.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';

@Component({
  selector: 'app-derivaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './derivaciones.html',
  styleUrl: './derivaciones.css'
})
export class Derivaciones implements OnInit {
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly derivacionService = inject(DerivacionService);
  private readonly estudianteService = inject(EstudianteService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  derivaciones: DerivacionPsicologica[] = [];
  estudiantes: Estudiante[] = [];
  loading = true;
  error = '';
  puedeCrear = !this.authService.hasRole('PSICOLOGO');
  puedeVerNotas = this.authService.hasAnyRole('DIRECTOR', 'PSICOLOGO');
  showModal = false;
  submitting = false;
  nuevaDerivacion = this.formularioVacio();

  ngOnInit(): void {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('derivaciones').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.cargarDatos(); });
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      derivaciones: this.derivacionService.getDerivaciones(),
      estudiantes: this.estudianteService.getEstudiantes()
    }).subscribe({
      next: data => {
        this.derivaciones = data.derivaciones;
        this.estudiantes = data.estudiantes;
        this.nuevaDerivacion.estudianteId = this.estudiantes[0]?.id ?? 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar las derivaciones y estudiantes.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirModal(): void {
    this.nuevaDerivacion = {
      ...this.formularioVacio(),
      estudianteId: this.estudiantes[0]?.id ?? 0
    };
    this.error = '';
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
  }

  guardarDerivacion(): void {
    if (!this.nuevaDerivacion.estudianteId || !this.nuevaDerivacion.descripcion.trim()) return;
    this.submitting = true;
    this.error = '';
    this.derivacionService.crearDerivacion(this.nuevaDerivacion).subscribe({
      next: derivacion => {
        this.derivaciones = [derivacion, ...this.derivaciones];
        this.submitting = false;
        this.cerrarModal();
        this.cdr.detectChanges();
      },
      error: error => {
        this.submitting = false;
        this.error = error.error?.message || 'No se pudo crear la derivación.';
        this.cdr.detectChanges();
      }
    });
  }

  getEstudianteNombre(id: number): string {
    const estudiante = this.estudiantes.find(item => item.id === id);
    return estudiante ? `${estudiante.nombre} ${estudiante.apellido}` : `Estudiante ${id}`;
  }

  getBadgeClass(prioridad: string): string {
    return prioridad === 'URGENTE' ? 'bg-danger' : 'bg-info text-dark';
  }

  private formularioVacio(): DerivacionPsicologica {
    return {
      estudianteId: 0,
      motivo: 'RENDIMIENTO',
      prioridad: 'NORMAL',
      descripcion: ''
    };
  }
}
