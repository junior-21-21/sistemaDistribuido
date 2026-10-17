import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { Curso, CursoService } from '../../services/curso.service';
import { GradoService } from '../../core/services/grado.service';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';

@Component({
  selector: 'app-cursos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cursos.html',
  styleUrl: './cursos.css'
})
export class Cursos implements OnInit {
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly cursoService = inject(CursoService);
  private readonly gradoService = inject(GradoService);

  cursos: Curso[] = [];
  grados: any[] = [];
  
  gradoSeleccionado: any = null;

  cargando = false;
  guardando = false;
  mostrarFormulario = false;
  mensaje = '';
  tipoMensaje: 'success' | 'error' = 'success';
  accionEnCurso: number | null = null;

  nuevoCurso: Curso = this.formularioVacio();

  ngOnInit(): void {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('cursos').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.cargarCursos(); });
    this.cargarCursos();
    this.cargarGrados();
  }

  cargarCursos(): void {
    this.cargando = true;
    this.cursoService.listarTodos()
      .pipe(finalize(() => this.cargando = false))
      .subscribe({
        next: items => this.cursos = items,
        error: error => this.mostrarError(error, 'No se pudieron cargar los cursos.')
      });
  }

  cargarGrados(): void {
    this.gradoService.getGrados().subscribe({
      next: (items: any) => this.grados = items,
      error: () => console.error('Error al cargar grados')
    });
  }

  seleccionarGrado(grado: any): void {
    this.gradoSeleccionado = grado;
    this.mostrarFormulario = false;
    this.limpiarMensaje();
  }

  volverAGrados(): void {
    this.gradoSeleccionado = null;
    this.mostrarFormulario = false;
    this.limpiarMensaje();
  }

  getCursosDelGradoSeleccionado(): Curso[] {
    if (!this.gradoSeleccionado) return [];
    return this.cursos.filter(c => c.gradoId === this.gradoSeleccionado.id);
  }

  abrirFormulario(): void {
    this.mostrarFormulario = true;
    this.limpiarMensaje();
    this.nuevoCurso = this.formularioVacio();
    if (this.gradoSeleccionado) {
      this.nuevoCurso.gradoId = this.gradoSeleccionado.id;
    }
  }

  cancelarFormulario(): void {
    this.mostrarFormulario = false;
    this.nuevoCurso = this.formularioVacio();
  }

  guardarCurso(): void {
    if (this.guardando) return;
    this.guardando = true;
    this.limpiarMensaje();
    
    // Ensure the course gets attached to the currently selected grade
    if (this.gradoSeleccionado) {
      this.nuevoCurso.gradoId = this.gradoSeleccionado.id;
    }

    if (this.nuevoCurso.id) {
      this.cursoService.actualizar(this.nuevoCurso.id, this.nuevoCurso)
        .pipe(finalize(() => this.guardando = false))
        .subscribe({
          next: actualizado => {
            const index = this.cursos.findIndex(c => c.id === actualizado.id);
            if (index !== -1) {
              this.cursos[index] = actualizado;
            }
            this.cancelarFormulario();
            this.mostrarExito(`El curso ${actualizado.nombre} fue actualizado con éxito.`);
          },
          error: error => this.mostrarError(error, 'No se pudo actualizar el curso.')
        });
    } else {
      this.cursoService.crear(this.nuevoCurso)
        .pipe(finalize(() => this.guardando = false))
        .subscribe({
          next: creado => {
            this.cursos = [...this.cursos, creado];
            this.cancelarFormulario();
            this.mostrarExito(`El curso ${creado.nombre} fue creado con éxito.`);
          },
          error: error => this.mostrarError(error, 'No se pudo crear el curso.')
        });
    }
  }

  editarCurso(curso: Curso): void {
    this.nuevoCurso = { ...curso };
    this.mostrarFormulario = true;
    this.limpiarMensaje();
    // Scroll to top where form is
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  eliminarCurso(curso: Curso): void {
    if (!curso.id) return;
    if (!confirm(`¿Estás seguro de eliminar el curso ${curso.nombre}?`)) return;
    
    this.accionEnCurso = curso.id;
    this.limpiarMensaje();
    
    this.cursoService.eliminar(curso.id)
      .pipe(finalize(() => this.accionEnCurso = null))
      .subscribe({
        next: () => {
          this.cursos = this.cursos.filter(c => c.id !== curso.id);
          this.mostrarExito(`El curso ${curso.nombre} fue eliminado.`);
        },
        error: error => this.mostrarError(error, 'No se pudo eliminar el curso.')
      });
  }

  getNombreGrado(gradoId: number): string {
    const grado = this.grados.find(g => g.id === gradoId);
    if (!grado) return 'Error: Sin Grado';
    return grado.nivel ? `${grado.nombre} - ${grado.nivel.nombre}` : grado.nombre;
  }

  private formularioVacio(): Curso {
    return { codigo: '', nombre: '', tipo: 'OBLIGATORIO', gradoId: null as any, estado: 'ACTIVO' };
  }

  private mostrarExito(mensaje: string): void {
    this.tipoMensaje = 'success';
    this.mensaje = mensaje;
  }

  private mostrarError(error: HttpErrorResponse, predeterminado: string): void {
    this.tipoMensaje = 'error';
    this.mensaje = error.error?.message || predeterminado;
  }

  limpiarMensaje(): void {
    this.mensaje = '';
  }
}
