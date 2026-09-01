import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisciplinaService, ReporteDisciplinario } from '../../services/disciplina.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-disciplina',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './disciplina.html',
  styleUrl: './disciplina.css'
})
export class Disciplina implements OnInit {
  private disciplinaService = inject(DisciplinaService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  reportes: ReporteDisciplinario[] = [];
  loading = false;
  showModal = false;
  submitting = false;
  successMessage = '';
  errorMessage = '';

  tiposReporte = [
    { value: 'TARDANZA', label: 'Tardanza', color: 'orange', icon: 'bi-clock-history' },
    { value: 'FALTA_LEVE', label: 'Falta Leve', color: 'yellow', icon: 'bi-exclamation-circle' },
    { value: 'FALTA_GRAVE', label: 'Falta Grave', color: 'red', icon: 'bi-shield-fill-exclamation' },
    { value: 'MERITO', label: 'Mérito / Reconocimiento', color: 'green', icon: 'bi-star-fill' },
    { value: 'LLAMADA_ATENCION', label: 'Llamada de Atención', color: 'blue', icon: 'bi-megaphone-fill' },
  ];

  reporteForm: FormGroup = this.fb.group({
    estudianteId: [1, Validators.required],
    tipo: ['TARDANZA', Validators.required],
    descripcion: ['', [Validators.required, Validators.minLength(10)]],
    observaciones: ['']
  });

  // Demo students for offline mode
  demoEstudiantes = [
    { id: 1, nombre: 'Rodrigo Quispe Sullca' },
    { id: 2, nombre: 'Valentina Huaman Flores' },
    { id: 3, nombre: 'Sebastian Ccahuana Puma' },
    { id: 4, nombre: 'Mateo Huanca Ccopa' },
    { id: 5, nombre: 'Isabella Sulca Vargas' },
  ];

  ngOnInit() {
    this.cargarReportes();
  }

  cargarReportes() {
    this.loading = true;
    this.disciplinaService.getReportesPorSeccion(1).subscribe({
      next: (data) => {
        this.reportes = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirModal() { this.showModal = true; }
  cerrarModal() { this.showModal = false; this.reporteForm.reset({ tipo: 'TARDANZA' }); }

  guardarReporte() {
    if (this.reporteForm.invalid) {
      this.reporteForm.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const reporte: ReporteDisciplinario = {
      ...this.reporteForm.value,
      docenteId: 1,
      seccionId: 1
    };
    this.disciplinaService.crearReporte(reporte).subscribe({
      next: (data) => {
        this.reportes.unshift(data);
        this.submitting = false;
        this.showModal = false;
        this.reporteForm.reset({ tipo: 'TARDANZA' });
        this.successMessage = 'Papeleta digital emitida correctamente ✓';
        this.cdr.detectChanges();
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: () => {
        this.submitting = false;
        alert('Error al emitir la papeleta digital.');
        this.cdr.detectChanges();
      }
    });
  }

  tipoBadgeClass(tipo: string): string {
    const map: Record<string, string> = {
      'TARDANZA': 'badge-tardanza',
      'FALTA_LEVE': 'badge-leve',
      'FALTA_GRAVE': 'badge-grave',
      'MERITO': 'badge-merito',
      'LLAMADA_ATENCION': 'badge-atencion'
    };
    return map[tipo] ?? '';
  }

  tipoLabel(tipo: string): string {
    return this.tiposReporte.find(t => t.value === tipo)?.label ?? tipo;
  }

  getTardanzas() { return this.reportes.filter(r => r.tipo === 'TARDANZA').length; }
  getFaltasGraves() { return this.reportes.filter(r => r.tipo === 'FALTA_GRAVE').length; }
  getMeritos() { return this.reportes.filter(r => r.tipo === 'MERITO').length; }
}
