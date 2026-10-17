import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { EstudianteService } from '../../services/estudiante.service';
import { AtencionMedica, DestinoAtencion, FichaMedica, TopicoService } from '../../services/topico.service';
import { LiveBanner } from '../../shared/live-banner/live-banner';

interface FichaVista extends FichaMedica {
  id: number;
  nombre: string;
  apellido: string;
  grado: string;
}

@Component({
  selector: 'app-topico',
  standalone: true,
  imports: [LiveBanner, CommonModule, FormsModule],
  templateUrl: './topico.html',
  styleUrl: './topico.css'
})
export class Topico implements OnInit {
  private readonly topicoService = inject(TopicoService);
  private readonly estudianteService = inject(EstudianteService);
  private readonly cdr = inject(ChangeDetectorRef);

  searchTerm = '';
  fichas: FichaVista[] = [];
  selectedFicha: FichaVista | null = null;
  atenciones: AtencionMedica[] = [];
  loading = true;
  atencionesLoading = false;
  submitting = false;
  error = '';
  showNuevaAtencion = false;
  nuevaAtencion = { motivo: '', diagnostico: '', tratamiento: '', destino: 'RETORNA_AULA' as DestinoAtencion, establecimientoSalud: '', cubiertoSis: false, apoderadoAvisado: false };
  savedMessage = '';

  ngOnInit(): void {
    this.cargarFichas();
  }

  get fichasFiltradas(): FichaVista[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('es');
    if (!term) return this.fichas;
    return this.fichas.filter(ficha =>
      [ficha.nombre, ficha.apellido, ficha.grado]
        .some(valor => valor.toLocaleLowerCase('es').includes(term))
    );
  }

  seleccionar(ficha: FichaVista): void {
    this.selectedFicha = ficha;
    this.showNuevaAtencion = false;
    this.error = '';
    this.atencionesLoading = true;
    this.topicoService.listarAtenciones(ficha.id).subscribe({
      next: atenciones => {
        this.atenciones = atenciones;
        this.atencionesLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.atenciones = [];
        this.atencionesLoading = false;
        this.error = 'No se pudo cargar el historial de atenciones.';
        this.cdr.detectChanges();
      }
    });
  }

  registrarAtencion(): void {
    if (!this.selectedFicha || !this.formularioValido() || this.submitting) return;
    this.submitting = true;
    this.error = '';
    this.topicoService.registrarAtencion({
      fichaMedicaId: this.selectedFicha.id,
      ...this.nuevaAtencion
    }).subscribe({
      next: atencion => {
        this.atenciones = [atencion, ...this.atenciones];
        this.submitting = false;
        this.savedMessage = 'Atención registrada correctamente.';
        this.showNuevaAtencion = false;
        this.nuevaAtencion = { motivo: '', diagnostico: '', tratamiento: '', destino: 'RETORNA_AULA' as DestinoAtencion, establecimientoSalud: '', cubiertoSis: false, apoderadoAvisado: false };
        this.cdr.detectChanges();
      },
      error: error => {
        this.submitting = false;
        this.error = error.error?.message || 'No se pudo registrar la atención.';
        this.cdr.detectChanges();
      }
    });
  }

  formularioValido(): boolean {
    return Boolean(
      this.nuevaAtencion.motivo.trim()
      && this.nuevaAtencion.diagnostico.trim()
      && this.nuevaAtencion.tratamiento.trim()
      && (this.nuevaAtencion.destino !== 'TRASLADO_SALUD' || this.nuevaAtencion.establecimientoSalud.trim())
    );
  }

  private cargarFichas(): void {
    forkJoin({
      fichas: this.topicoService.listarFichas(),
      estudiantes: this.estudianteService.getEstudiantes()
    }).subscribe({
      next: data => {
        this.fichas = data.fichas.flatMap(ficha => {
          if (ficha.id == null) return [];
          const estudiante = data.estudiantes.find(item => item.id === ficha.estudianteId);
          if (!estudiante) return [];
          return [{
            ...ficha,
            id: ficha.id,
            nombre: estudiante.nombre,
            apellido: estudiante.apellido,
            grado: `Grado ${estudiante.gradoId} · Sección ${estudiante.seccionId}`
          }];
        });
        this.loading = false;
        if (this.fichas.length) this.seleccionar(this.fichas[0]);
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudieron cargar las fichas médicas.';
        this.cdr.detectChanges();
      }
    });
  }

  /** Recarga pedida desde el aviso de cambios en vivo. */
  refrescarEnVivo(): void {
    this.cargarFichas();
  }
}
