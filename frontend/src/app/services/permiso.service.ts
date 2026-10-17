import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoPermiso = 'JUSTIFICACION' | 'PAPELETA_SALIDA' | 'PERMISO';
export type EstadoPermiso = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

export interface Permiso {
  id: number;
  estudianteId: number;
  estudianteNombre: string;
  estudianteDni: string;
  seccionId: number | null;
  tipo: TipoPermiso;
  fechaDesde: string;
  fechaHasta: string;
  horaSalida: string | null;
  motivo: string;
  personaRecoge: string | null;
  dniPersonaRecoge: string | null;
  origen: 'APODERADO' | 'PERSONAL';
  solicitadoPor: string;
  estado: EstadoPermiso;
  revisadoPor: string | null;
  fechaRevision: string | null;
  observacionRevision: string | null;
  asistenciasJustificadas: number;
  salidaRegistradaPor: string | null;
  fechaSalida: string | null;
  creadoEn: string;
}

export interface NuevoPermiso {
  estudianteId: number;
  tipo: TipoPermiso;
  fechaDesde: string;
  fechaHasta?: string | null;
  horaSalida?: string | null;
  motivo: string;
  personaRecoge?: string | null;
  dniPersonaRecoge?: string | null;
  aprobarAhora?: boolean;
}

export const TIPO_PERMISO: Record<TipoPermiso, string> = {
  JUSTIFICACION: 'Justificación de inasistencia',
  PAPELETA_SALIDA: 'Papeleta de salida',
  PERMISO: 'Permiso de varios días'
};

@Injectable({ providedIn: 'root' })
export class PermisoService {
  private readonly http = inject(HttpClient);

  listar(estado = '', desde = '', hasta = ''): Observable<Permiso[]> {
    const params: Record<string, string> = {};
    if (estado) params['estado'] = estado;
    if (desde) params['desde'] = desde;
    if (hasta) params['hasta'] = hasta;
    return this.http.get<Permiso[]>('/api/permisos', { params });
  }

  salidasDeHoy(): Observable<Permiso[]> {
    return this.http.get<Permiso[]>('/api/permisos/salidas-hoy');
  }

  registrar(permiso: NuevoPermiso): Observable<Permiso> {
    return this.http.post<Permiso>('/api/permisos', permiso);
  }

  revisar(id: number, aprobar: boolean, observacion: string): Observable<Permiso> {
    return this.http.patch<Permiso>(`/api/permisos/${id}/revision`, { aprobar, observacion });
  }

  registrarSalida(id: number): Observable<Permiso> {
    return this.http.patch<Permiso>(`/api/permisos/${id}/salida`, {});
  }

  /** Portal del apoderado: solo ve y solicita para sus hijos. */
  delApoderado(): Observable<Permiso[]> {
    return this.http.get<Permiso[]>('/api/portal-apoderado/permisos');
  }

  solicitarComoApoderado(permiso: NuevoPermiso): Observable<Permiso> {
    return this.http.post<Permiso>('/api/portal-apoderado/permisos', permiso);
  }
}
