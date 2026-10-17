import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-matricula',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './matricula.html',
  styleUrl: './matricula.css'
})
export class Matricula implements OnInit {
  private router = inject(Router);

  currentStep = 1;
  isSubmitting = false;

  // Step 1: Estudiante
  estudiante = {
    nombres: '',
    apellidos: '',
    dni: '',
    fechaNacimiento: ''
  };

  // Step 2: Apoderado
  apoderado = {
    dni: '',
    nombres: '',
    parentesco: '',
    telefono: '',
    email: ''
  };

  // Step 3: Académico
  academico = {
    gradoId: '',
    seccionId: ''
  };

  // Step 4: Pago
  pago = {
    monto: 350.00,
    metodo: 'TRANSFERENCIA',
    nroOperacion: ''
  };

  grados = [
    { id: '1', nombre: '1° de Secundaria' },
    { id: '2', nombre: '2° de Secundaria' },
    { id: '3', nombre: '3° de Secundaria' },
    { id: '4', nombre: '4° de Secundaria' },
    { id: '5', nombre: '5° de Secundaria' }
  ];

  secciones = [
    { id: '1', nombre: 'Sección A (Mañana)' },
    { id: '2', nombre: 'Sección B (Mañana)' },
    { id: '3', nombre: 'Sección C (Tarde)' }
  ];

  ngOnInit(): void {
    // Inicialización si se necesita cargar datos previos
  }

  nextStep(): void {
    if (this.currentStep < 4) {
      this.currentStep++;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  isStep1Valid(): boolean {
    return !!(this.estudiante.nombres && this.estudiante.apellidos && this.estudiante.dni && this.estudiante.fechaNacimiento);
  }

  isStep2Valid(): boolean {
    return !!(this.apoderado.dni && this.apoderado.nombres && this.apoderado.parentesco && this.apoderado.telefono);
  }

  isStep3Valid(): boolean {
    return !!(this.academico.gradoId && this.academico.seccionId);
  }

  isStep4Valid(): boolean {
    if (this.pago.metodo !== 'EFECTIVO' && !this.pago.nroOperacion) return false;
    return this.pago.monto > 0;
  }

  canGoNext(): boolean {
    if (this.currentStep === 1) return this.isStep1Valid();
    if (this.currentStep === 2) return this.isStep2Valid();
    if (this.currentStep === 3) return this.isStep3Valid();
    return false;
  }

  submitMatricula(): void {
    if (!this.isStep4Valid()) return;
    
    this.isSubmitting = true;
    
    // Simulate API call delay
    setTimeout(() => {
      this.isSubmitting = false;
      alert('¡Matrícula registrada exitosamente!');
      this.router.navigate(['/estudiantes']);
    }, 1500);
  }

  buscarApoderadoPorDni(): void {
    // Simula buscar un apoderado si el DNI tiene 8 dígitos
    if (this.apoderado.dni && this.apoderado.dni.length >= 8) {
      // Mock data
      this.apoderado.nombres = 'Carlos ' + this.estudiante.apellidos.split(' ')[0];
      this.apoderado.telefono = '987654321';
      this.apoderado.email = 'carlos@ejemplo.com';
    }
  }
}
