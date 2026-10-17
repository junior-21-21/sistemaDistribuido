import { Component, OnInit, inject, ChangeDetectorRef, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SeccionService } from '../../core/services/seccion.service';
import { Seccion } from '../../core/models/seccion.model';
import { GradoService } from '../../core/services/grado.service';
import { Grado } from '../../core/models/grado.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Docente, DocenteService } from '../../services/docente.service';

@Component({
  selector: 'app-secciones',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Default,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './secciones.html',
  styleUrl: './secciones.css'
})
export class Secciones implements OnInit {
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  secciones: Seccion[] = [];
  grados: Grado[] = [];
  loadError: string | null = null;
  isLoading = false;

  gradoSeleccionado: Grado | null = null;

  seccionForm!: FormGroup;
  showModal = false;
  editingSeccionId: number | null = null;

  searchTerm: string = '';
  filterGrado: string = '';

  private seccionService = inject(SeccionService);
  private gradoService = inject(GradoService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private docenteService = inject(DocenteService);
  private toast = inject(ToastService);

  /** Dirección y Subdirección designan al tutor de cada aula (TOE). */
  readonly puedeAsignarTutor = inject(AuthService).hasAnyRole('DIRECTOR', 'SUBDIRECTOR');
  docentes: Docente[] = [];

  ngOnInit() {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('secciones').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.loadSecciones(); });
    this.initForm();
    this.loadSecciones();
    this.loadGrados();
    if (this.puedeAsignarTutor) {
      this.docenteService.listarTodos().subscribe({
        next: lista => {
          this.docentes = lista.filter(d => d.estado === 'ACTIVO')
            .sort((a, b) => `${a.apellido} ${a.nombre}`.localeCompare(`${b.apellido} ${b.nombre}`, 'es'));
          this.cdr.detectChanges();
        },
        error: () => undefined
      });
    }
  }

  cambiarTutor(seccion: Seccion, docenteId: number | null): void {
    if (!seccion.id) return;
    const anterior = seccion.docenteTutorId;
    seccion.docenteTutorId = docenteId ?? undefined;
    this.seccionService.asignarTutor(seccion.id, docenteId).subscribe({
      next: actualizada => {
        Object.assign(seccion, actualizada);
        this.toast.exito('Tutor actualizado', `Sección ${seccion.nombre}: ${actualizada.docenteTutorNombre ?? 'sin tutor'}`);
        this.cdr.detectChanges();
      },
      error: err => {
        seccion.docenteTutorId = anterior;
        this.toast.error('No se pudo asignar el tutor', err?.error?.message);
        this.cdr.detectChanges();
      }
    });
  }

  initForm() {
    this.seccionForm = this.fb.group({
      nombre: ['', Validators.required],
      gradoId: ['', Validators.required],
      turno: ['MANANA', Validators.required],
      capacidadMaxima: [35, [Validators.required, Validators.min(1)]],
      anioEscolar: [new Date().getFullYear(), Validators.required],
      activa: [true]
    });
  }

  loadSecciones() {
    this.isLoading = true;
    this.loadError = null;
    this.seccionService.getSecciones().subscribe({
      next: (data) => {
        this.secciones = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError = `HTTP ${err.status}: ${err.message}`;
        this.cdr.detectChanges();
      }
    });
  }

  loadGrados() {
    this.gradoService.getGrados().subscribe({
      next: (data) => {
        this.grados = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al cargar grados:', err)
    });
  }

  seleccionarGrado(grado: Grado): void {
    this.gradoSeleccionado = grado;
  }

  volverAGrados(): void {
    this.gradoSeleccionado = null;
  }

  getSeccionesDelGrado(): Seccion[] {
    if (!this.gradoSeleccionado) return [];
    return this.secciones.filter(s => s.gradoId?.toString() === this.gradoSeleccionado!.id?.toString());
  }

  getSeccionesManana(): Seccion[] {
    return this.getSeccionesDelGrado().filter(s => s.turno === 'MANANA');
  }

  getSeccionesTarde(): Seccion[] {
    return this.getSeccionesDelGrado().filter(s => s.turno === 'TARDE');
  }

  contarSeccionesDelGrado(gradoId: number): number {
    return this.secciones.filter(s => s.gradoId?.toString() === gradoId.toString()).length;
  }

  contarSeccionesTurno(gradoId: number, turno: string): number {
    return this.secciones.filter(s => s.gradoId?.toString() === gradoId.toString() && s.turno === turno).length;
  }

  openModal(seccion?: Seccion) {
    if (seccion) {
      this.editingSeccionId = seccion.id!;
      this.seccionForm.patchValue({
        nombre: seccion.nombre,
        gradoId: seccion.gradoId,
        turno: seccion.turno,
        capacidadMaxima: seccion.capacidadMaxima,
        anioEscolar: seccion.anioEscolar,
        activa: seccion.activa
      });
    } else {
      this.editingSeccionId = null;
      this.seccionForm.reset({
        turno: 'MANANA',
        capacidadMaxima: 35,
        anioEscolar: new Date().getFullYear(),
        activa: true
      });
      if (this.gradoSeleccionado) {
        this.seccionForm.patchValue({ gradoId: this.gradoSeleccionado.id });
      }
    }
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingSeccionId = null;
  }

  onSubmit() {
    if (this.seccionForm.invalid) {
      Object.keys(this.seccionForm.controls).forEach(key => {
        this.seccionForm.get(key)?.markAsTouched();
      });
      return;
    }

    const formVal = this.seccionForm.value;

    // Validar duplicado: mismo nombre + gradoId + turno + anioEscolar
    const duplicado = this.secciones.find(s =>
      s.nombre.toLowerCase() === (formVal.nombre || '').toLowerCase() &&
      s.gradoId?.toString() === formVal.gradoId?.toString() &&
      s.turno === formVal.turno &&
      s.anioEscolar?.toString() === formVal.anioEscolar?.toString() &&
      s.id !== this.editingSeccionId
    );

    if (duplicado) {
      alert(`Ya existe la seccion "${formVal.nombre}" en ese grado, turno y anio escolar.`);
      return;
    }

    const seccionData: Seccion = formVal;

    if (this.editingSeccionId) {
      this.seccionService.updateSeccion(this.editingSeccionId, seccionData).subscribe({
        next: () => {
          this.loadSecciones();
          this.closeModal();
        },
        error: (err) => console.error('Error updating seccion', err)
      });
    } else {
      this.seccionService.createSeccion(seccionData).subscribe({
        next: () => {
          this.loadSecciones();
          this.closeModal();
        },
        error: (err) => console.error('Error creating seccion', err)
      });
    }
  }

  deleteSeccion(id: number) {
    if (confirm('Estas seguro de desactivar/eliminar esta seccion?')) {
      this.seccionService.deleteSeccion(id).subscribe({
        next: () => this.loadSecciones(),
        error: (err) => console.error('Error deleting seccion', err)
      });
    }
  }
}
