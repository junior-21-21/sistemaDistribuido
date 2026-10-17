import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { finalize } from 'rxjs';
import { PortalApoderadoService, PortalEstudiante, PortalResumen } from '../../services/portal-apoderado.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { NuevoPermiso, Permiso, PermisoService, TIPO_PERMISO } from '../../services/permiso.service';

@Component({
  selector: 'app-portal-apoderado',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portal-apoderado.html',
  styleUrl: './portal-apoderado.css'
})
export class PortalApoderado implements OnInit {
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly service = inject(PortalApoderadoService);
  estudiantes: PortalEstudiante[] = [];
  seleccionado: number | null = null;
  resumen: PortalResumen | null = null;
  cargando = false;
  error = '';
  pestana: 'notas' | 'asistencia' | 'permisos' | 'documentos' = 'notas';

  private readonly permisoService = inject(PermisoService);
  readonly tipos = TIPO_PERMISO;
  permisos: Permiso[] = [];
  nuevo: NuevoPermiso = this.permisoVacio();
  enviandoPermiso = false;
  mensajePermiso = '';
  errorPermiso = false;

  ngOnInit(): void {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('notas', 'asistencias').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.seleccionado != null && this.seleccionar(this.seleccionado); });
    this.realtime.alCambiar('permisos').pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargarPermisos());
    this.cargarPermisos();
    this.cargando = true;
    this.service.estudiantes().pipe(finalize(() => this.cargando = false)).subscribe({
      next: items => {
        this.estudiantes = items;
        if (items.length) this.seleccionar(items[0].id);
      },
      error: () => this.error = 'No se pudo cargar la información vinculada.'
    });
  }

  seleccionar(id: number): void {
    this.seleccionado = id;
    this.cargando = true;
    this.error = '';
    this.service.resumen(id).pipe(finalize(() => this.cargando = false)).subscribe({
      next: resumen => this.resumen = resumen,
      error: () => this.error = 'No tienes acceso a este estudiante o la información no está disponible.'
    });
  }

  permisosDelHijo(): Permiso[] {
    return this.permisos.filter(p => p.estudianteId === this.seleccionado);
  }

  solicitarPermiso(): void {
    if (this.seleccionado == null) return;
    const papeleta = this.nuevo.tipo === 'PAPELETA_SALIDA';
    const datos: NuevoPermiso = {
      ...this.nuevo,
      estudianteId: this.seleccionado,
      fechaHasta: papeleta ? this.nuevo.fechaDesde : (this.nuevo.fechaHasta || this.nuevo.fechaDesde),
      horaSalida: papeleta ? this.nuevo.horaSalida : null,
      personaRecoge: papeleta ? this.nuevo.personaRecoge : null,
      dniPersonaRecoge: papeleta ? this.nuevo.dniPersonaRecoge : null,
      motivo: this.nuevo.motivo.trim()
    };
    this.enviandoPermiso = true;
    this.mensajePermiso = '';
    this.permisoService.solicitarComoApoderado(datos).pipe(finalize(() => this.enviandoPermiso = false)).subscribe({
      next: p => {
        this.permisos = [p, ...this.permisos];
        this.nuevo = this.permisoVacio();
        this.errorPermiso = false;
        this.mensajePermiso = 'Solicitud enviada. Te avisaremos cuando el colegio la revise.';
      },
      error: (e: HttpErrorResponse) => {
        this.errorPermiso = true;
        this.mensajePermiso = e.error?.message || 'No se pudo enviar la solicitud.';
      }
    });
  }

  private cargarPermisos(): void {
    this.permisoService.delApoderado().subscribe({ next: lista => this.permisos = lista, error: () => undefined });
  }

  private permisoVacio(): NuevoPermiso {
    const d = new Date();
    const hoy = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return { estudianteId: 0, tipo: 'JUSTIFICACION', fechaDesde: hoy, fechaHasta: hoy, horaSalida: '', motivo: '',
      personaRecoge: '', dniPersonaRecoge: '' };
  }

  descargar(documentoId: number, nombre: string): void {
    if (this.seleccionado == null) return;
    this.service.descargar(this.seleccionado, documentoId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = nombre;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error = 'No se pudo descargar el documento.'
    });
  }

  getLiteralValue(nota: number | undefined): string {
    if (nota === undefined || nota === null) return '-';
    if (nota >= 18) return 'AD';
    if (nota >= 14) return 'A';
    if (nota >= 11) return 'B';
    return 'C';
  }

  getLiteralClass(nota: number | undefined): string {
    if (nota === undefined || nota === null) return 'bg-secondary';
    if (nota >= 18) return 'bg-success';
    if (nota >= 14) return 'bg-primary';
    if (nota >= 11) return 'bg-warning text-dark';
    return 'bg-danger';
  }
}
