import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Estudiante {
  id?: number;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  telefono: string;
  gradoId: number;
  seccionId: number;
  estado?: string;
}

export interface MatriculaRequest {
  estudiante: Estudiante;
  apoderadoId: number;
  parentesco: string;
  permisoRetiro: boolean;
  tipoMatricula?: 'NUEVO_INGRESO' | 'CONTINUIDAD' | 'TRASLADO_ENTRANTE';
  ieProcedencia?: string | null;
  codigoModularProcedencia?: string | null;
  consentimientos: Array<{
    finalidad: 'DATOS_ACADEMICOS' | 'SALUD' | 'COMUNICACIONES';
    accion: 'OTORGADO' | 'DENEGADO';
    evidencia?: string;
  }>;
}

export interface MatriculaAnual {
  id: number;
  anioEscolar: number;
  tipo: string;
  gradoId?: number;
  seccionId?: number;
  ieProcedencia?: string;
  situacionFinal?: string;
  ieDestino?: string;
  fechaTraslado?: string;
  motivoTraslado?: string;
}

export interface AreaPendiente {
  id: number;
  estudianteId: number;
  estudiante: string;
  seccionId?: number;
  cursoNombre: string;
  gradoOrigen: string;
  anioOrigen: number;
  estado: 'PENDIENTE' | 'SUBSANADA' | 'NO_SUBSANADA';
  calificacion?: string;
  observacion?: string;
  fechaResultado?: string;
  /** RECUPERACION: acompañamiento al terminar el año. SUBSANACION: área que lleva hasta diciembre. */
  tipo: 'RECUPERACION' | 'SUBSANACION';
  gradoId?: number;
  /** Situación final del año de origen (RECUPERACION mientras el caso siga abierto). */
  situacionOrigen?: string;
}

/** Situación final sugerida por las reglas del MINEDU a partir de las notas (servicio de evaluaciones). */
export interface SituacionSugerida {
  estudianteId: number;
  situacion: 'PROMOVIDO' | 'RECUPERACION' | 'PERMANECE' | 'SIN_NOTAS';
  motivo: string;
  areasConC: number[];
  areas: { cursoId: number; competencias: { competencia: string; nivel: string }[]; enC: number }[];
}

export interface CierreRecuperacion {
  situacion: 'PROMOVIDO' | 'PERMANECE' | 'EGRESADO';
  areaPorSubsanar?: string | null;
  detalle: string;
}

export interface Trayectoria {
  matriculas: MatriculaAnual[];
  areasPendientes: AreaPendiente[];
}

export interface Promocion {
  anioOrigen: number;
  gradoOrigen: string;
  decisiones: {
    estudianteId: number;
    situacion: 'PROMOVIDO' | 'PERMANECE' | 'RECUPERACION' | 'EGRESADO';
    gradoDestinoId?: number | null;
    seccionDestinoId?: number | null;
    areasPendientes: string[];
    areasRecuperacion?: string[];
  }[];
}

@Injectable({
  providedIn: 'root'
})
export class EstudianteService {
  private apiUrl = '/api/estudiantes';

  constructor(private http: HttpClient) { }

  getEstudiantes(): Observable<Estudiante[]> {
    return this.http.get<Estudiante[]>(this.apiUrl);
  }

  getEstudiantesPage(page = 0, size = 20, q = '', gradoId?: number,
                     seccionIds: number[] = []): Observable<PageResponse<Estudiante>> {
    const params: Record<string, string | number | readonly (string | number)[]> = { page, size, q };
    if (gradoId) params['gradoId'] = gradoId;
    if (seccionIds.length) params['seccionIds'] = seccionIds;
    return this.http.get<PageResponse<Estudiante>>(`${this.apiUrl}/page`, { params });
  }

  createEstudiante(estudiante: Estudiante): Observable<Estudiante> {
    return this.http.post<Estudiante>(this.apiUrl, estudiante);
  }

  /** Cambio de sección dentro del mismo grado (traslado interno). */
  cambiarSeccion(id: number, seccionDestinoId: number, motivo: string): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/${id}/cambio-seccion`, { seccionDestinoId, motivo });
  }

  trasladarSalida(id: number, datos: { ieDestino: string; codigoModular?: string; fecha?: string; motivo?: string }): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/${id}/traslado-salida`, datos);
  }

  trayectoria(id: number): Observable<Trayectoria> {
    return this.http.get<Trayectoria>(`${this.apiUrl}/${id}/trayectoria`);
  }

  situacionSugerida(seccionId: number, grado: number, anio: number): Observable<SituacionSugerida[]> {
    return this.http.get<SituacionSugerida[]>('/api/notas/situacion-final', { params: { seccionId, grado, anio } });
  }

  cerrarRecuperacion(estudianteId: number, datos: { anioOrigen: number; gradoDestinoId?: number | null;
      seccionDestinoId?: number | null; seccionPermanenciaId?: number | null; ultimoGrado: boolean }): Observable<CierreRecuperacion> {
    return this.http.post<CierreRecuperacion>(`/api/recuperacion/estudiante/${estudianteId}/cerrar`, datos);
  }

  promover(promocion: Promocion): Observable<{ promovidos: number; permanecen: number; enRecuperacion: number; areasRegistradas: number }> {
    return this.http.post<{ promovidos: number; permanecen: number; enRecuperacion: number; areasRegistradas: number }>(`${this.apiUrl}/promocion`, promocion);
  }

  matricular(request: MatriculaRequest): Observable<Estudiante> {
    return this.http.post<Estudiante>(`${this.apiUrl}/matricula`, request);
  }
}
