import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface FichaMedica {
  id: number;
  nombre: string;
  apellido: string;
  grado: string;
  tipoSangre: string;
  alergias: string;
  condiciones: string;
  ultimaAtencion: string;
}

@Component({
  selector: 'app-topico',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './topico.html',
  styleUrl: './topico.css'
})
export class Topico {
  searchTerm = '';
  selectedFicha: FichaMedica | null = null;
  showNuevaAtencion = false;
  nuevaAtencion = { motivo: '', diagnostico: '', tratamiento: '' };
  savedMessage = '';

  fichas: FichaMedica[] = [
    { id: 1, nombre: 'Rodrigo', apellido: 'Quispe Sullca', grado: '1ro A', tipoSangre: 'O+', alergias: 'Ninguna', condiciones: 'Ninguna', ultimaAtencion: '15/08/2026' },
    { id: 2, nombre: 'Valentina', apellido: 'Huaman Flores', grado: '1ro A', tipoSangre: 'A+', alergias: 'Polen, polvo', condiciones: 'Asma leve', ultimaAtencion: '20/08/2026' },
    { id: 3, nombre: 'Sebastian', apellido: 'Ccahuana Puma', grado: '1ro B', tipoSangre: 'B+', alergias: 'Ninguna', condiciones: 'Ninguna', ultimaAtencion: '10/07/2026' },
    { id: 4, nombre: 'Camila', apellido: 'Rojas Suni', grado: '4to A', tipoSangre: 'AB+', alergias: 'Penicilina', condiciones: 'Rinitis alérgica', ultimaAtencion: '22/08/2026' },
    { id: 5, nombre: 'Daniel', apellido: 'Torres Huanca', grado: '4to A', tipoSangre: 'O-', alergias: 'Mariscos', condiciones: 'Ninguna', ultimaAtencion: '01/08/2026' },
  ];

  get fichasFiltradas() {
    if (!this.searchTerm) return this.fichas;
    const term = this.searchTerm.toLowerCase();
    return this.fichas.filter(f =>
      f.nombre.toLowerCase().includes(term) ||
      f.apellido.toLowerCase().includes(term) ||
      f.grado.toLowerCase().includes(term)
    );
  }

  seleccionar(ficha: FichaMedica) {
    this.selectedFicha = ficha;
    this.showNuevaAtencion = false;
  }

  registrarAtencion() {
    setTimeout(() => {
      this.savedMessage = 'Atención registrada correctamente ✓';
      this.showNuevaAtencion = false;
      this.nuevaAtencion = { motivo: '', diagnostico: '', tratamiento: '' };
      setTimeout(() => this.savedMessage = '', 3000);
    }, 1000);
  }
}
