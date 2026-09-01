import { Component, OnInit, inject, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SeccionService } from '../../core/services/seccion.service';
import { Seccion } from '../../core/models/seccion.model';
import { GradoService } from '../../core/services/grado.service';
import { Grado } from '../../core/models/grado.model';

@Component({
  selector: 'app-secciones',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Default,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './secciones.html',
  styleUrl: './secciones.css'
})
export class Secciones implements OnInit {
  secciones: Seccion[] = [];
  grados: Grado[] = [];
  loadError: string | null = null;
  isLoading = false;
  
  seccionForm!: FormGroup;
  showModal = false;
  editingSeccionId: number | null = null;
  
  searchTerm: string = '';
  filterGrado: string = '';

  private seccionService = inject(SeccionService);
  private gradoService = inject(GradoService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit() {
    this.initForm();
    this.loadSecciones();
    this.loadGrados();
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
        console.log('Secciones cargadas:', data.length);
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError = `HTTP ${err.status}: ${err.message}`;
        this.cdr.detectChanges();
        console.error('Error al cargar secciones:', err);
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

  get filteredSecciones(): Seccion[] {
    const search = (this.searchTerm || '').toLowerCase();
    return this.secciones.filter(seccion => {
      const matchName = (seccion.nombre || '').toLowerCase().includes(search) ||
                        (seccion.gradoNombre || '').toLowerCase().includes(search) ||
                        (seccion.nivelNombre || '').toLowerCase().includes(search);
      const matchGrado = this.filterGrado ? seccion.gradoId?.toString() === this.filterGrado : true;
      return matchName && matchGrado;
    });
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

    // Validar duplicado: mismo nombre (case-insensitive) + gradoId + turno + anioEscolar
    const duplicado = this.secciones.find(s =>
      s.nombre.toLowerCase() === (formVal.nombre || '').toLowerCase() &&
      s.gradoId?.toString() === formVal.gradoId?.toString() &&
      s.turno === formVal.turno &&
      s.anioEscolar?.toString() === formVal.anioEscolar?.toString() &&
      s.id !== this.editingSeccionId
    );

    if (duplicado) {
      alert(`Ya existe la sección "${formVal.nombre}" en ese grado, turno y año escolar.`);
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
    if (confirm('¿Estás seguro de desactivar/eliminar esta sección?')) {
      this.seccionService.deleteSeccion(id).subscribe({
        next: () => this.loadSecciones(),
        error: (err) => console.error('Error deleting seccion', err)
      });
    }
  }
}
