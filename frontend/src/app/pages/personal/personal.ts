import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface PersonalData {
  id: number;
  nombre: string;
  apellido: string;
  rol: string;
  email: string;
  estado: 'ACTIVO' | 'PAUSADO' | 'BAJA';
  ultimoAcceso: string;
}

@Component({
  selector: 'app-personal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personal.html',
  styleUrl: './personal.css'
})
export class Personal implements OnInit {
  personalList: PersonalData[] = [
    { id: 1, nombre: 'Carlos', apellido: 'Quispe Mendoza', rol: 'DIRECTOR', email: 'carlos.quispe@iep.edu.pe', estado: 'ACTIVO', ultimoAcceso: 'Hace 5 min' },
    { id: 2, nombre: 'Maria', apellido: 'Llanos Perez', rol: 'SECRETARIA', email: 'maria.llanos@iep.edu.pe', estado: 'ACTIVO', ultimoAcceso: 'Ayer' },
    { id: 3, nombre: 'Juan', apellido: 'Rojas', rol: 'DOCENTE', email: 'juan.rojas@iep.edu.pe', estado: 'ACTIVO', ultimoAcceso: 'Hace 2 horas' },
    { id: 4, nombre: 'Elena', apellido: 'Torres', rol: 'DOCENTE', email: 'elena.torres@iep.edu.pe', estado: 'PAUSADO', ultimoAcceso: 'Hace 15 dias' },
    { id: 5, nombre: 'Jorge', apellido: 'Paucar', rol: 'PSICOLOGO', email: 'jorge.paucar@iep.edu.pe', estado: 'ACTIVO', ultimoAcceso: 'Hoy' },
    { id: 6, nombre: 'Ana', apellido: 'Guzman', rol: 'DOCENTE', email: 'ana.guzman@iep.edu.pe', estado: 'BAJA', ultimoAcceso: 'Hace 3 meses' }
  ];

  filteredList: PersonalData[] = [];
  searchTerm = '';
  filtroRol = '';

  ngOnInit() {
    this.filteredList = [...this.personalList];
  }

  filtrar() {
    const t = this.searchTerm.toLowerCase();
    this.filteredList = this.personalList.filter(p => {
      const matchText = p.nombre.toLowerCase().includes(t) || 
                        p.apellido.toLowerCase().includes(t) || 
                        p.email.toLowerCase().includes(t);
      if (!matchText) return false;
      if (this.filtroRol && p.rol !== this.filtroRol) return false;
      return true;
    });
  }

  toggleEstado(p: PersonalData) {
    if (p.estado === 'ACTIVO') {
      p.estado = 'PAUSADO';
    } else if (p.estado === 'PAUSADO') {
      p.estado = 'ACTIVO';
    }
  }

  resetClave(p: PersonalData) {
    alert(`Contraseña de ${p.nombre} restablecida a "123456". En el sistema real esto enviaría un correo o guardaría el cambio en BD.`);
  }

  darDeBaja(p: PersonalData) {
    if (confirm(`¿Estás seguro de dar de baja a ${p.nombre}? Su acceso será revocado permanentemente.`)) {
      p.estado = 'BAJA';
    }
  }
}
