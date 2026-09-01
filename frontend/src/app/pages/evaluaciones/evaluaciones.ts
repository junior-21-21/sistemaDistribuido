import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotaService, Nota } from '../../services/nota.service';
import { AuthService } from '../../core/services/auth.service';

interface EstudianteRow {
  id: number;
  nombre: string;
  apellido: string;
  notas: Record<string, string>; // competencia -> escalaLiteral
}

@Component({
  selector: 'app-evaluaciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './evaluaciones.html',
  styleUrl: './evaluaciones.css'
})
export class Evaluaciones implements OnInit {
  private notaService = inject(NotaService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  currentSeccionId = 1;
  currentGrado = '3ro "A"';
  currentCurso = 'Comunicación';
  currentBimestre = 'II BIMESTRE';
  currentPeriodo = 'II Bimestre 2026';

  saving = false;
  loading = false;
  savedMessage = '';
  errorMessage = '';

  escalas = ['AD', 'A', 'B', 'C'];
  competencias = ['Comunicación', 'Matemática', 'Ciencia y Tecnología', 'Personal Social'];

  // Estudiantes con sus notas por competencia
  estudiantes: EstudianteRow[] = [
    { id: 1, nombre: 'Rodrigo', apellido: 'Quispe Sullca', notas: { 'Comunicación': 'A', 'Matemática': 'B', 'Ciencia y Tecnología': 'A', 'Personal Social': 'AD' } },
    { id: 2, nombre: 'Valentina', apellido: 'Huaman Flores', notas: { 'Comunicación': 'AD', 'Matemática': 'A', 'Ciencia y Tecnología': 'AD', 'Personal Social': 'A' } },
    { id: 3, nombre: 'Sebastian', apellido: 'Ccahuana Puma', notas: { 'Comunicación': 'B', 'Matemática': 'C', 'Ciencia y Tecnología': 'B', 'Personal Social': 'B' } },
    { id: 4, nombre: 'Mateo', apellido: 'Huanca Ccopa', notas: { 'Comunicación': 'A', 'Matemática': 'A', 'Ciencia y Tecnología': 'A', 'Personal Social': 'A' } },
    { id: 5, nombre: 'Isabella', apellido: 'Sulca Vargas', notas: { 'Comunicación': 'AD', 'Matemática': 'AD', 'Ciencia y Tecnología': 'A', 'Personal Social': 'AD' } },
    { id: 6, nombre: 'Alejandro', apellido: 'Mamani Castro', notas: { 'Comunicación': 'B', 'Matemática': 'A', 'Ciencia y Tecnología': 'B', 'Personal Social': 'A' } },
    { id: 7, nombre: 'Emilio', apellido: 'Huaman Torres', notas: { 'Comunicación': 'C', 'Matemática': 'B', 'Ciencia y Tecnología': 'C', 'Personal Social': 'B' } },
  ];

  ngOnInit() {
    // Future: load real students from backend by seccionId
  }

  escalaBadgeClass(escala: string): string {
    const map: Record<string, string> = {
      'AD': 'badge-ad', 'A': 'badge-a', 'B': 'badge-b', 'C': 'badge-c'
    };
    return map[escala] ?? '';
  }

  guardarNotas() {
    this.saving = true;
    this.errorMessage = '';

    // Build array of Nota objects from the grid
    const notas: Nota[] = [];
    for (const est of this.estudiantes) {
      for (const competencia of this.competencias) {
        const escala = est.notas[competencia];
        if (escala) {
          notas.push({
            estudianteId: est.id,
            cursoId: 1,       // TODO: bind to actual course selection
            docenteId: 1,     // TODO: get from AuthService
            seccionId: this.currentSeccionId,
            escalaLiteral: escala,
            competencia,
            bimestre: this.currentBimestre
          });
        }
      }
    }

    this.notaService.guardarNotasMasivas(notas).subscribe({
      next: () => {
        this.saving = false;
        this.savedMessage = `✓ ${notas.length} notas guardadas correctamente`;
        this.cdr.detectChanges();
        setTimeout(() => { this.savedMessage = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: () => {
        this.saving = false;
        alert('Error al guardar las notas. Verifique la conexión con el servidor.');
        this.cdr.detectChanges();
      }
    });
  }

  trackById(_: number, item: EstudianteRow): number {
    return item.id;
  }
}
