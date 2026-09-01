export interface Seccion {
  id?: number;
  nombre: string;
  gradoId: number;
  gradoNombre?: string;
  nivelNombre?: string;
  turno: string;
  capacidadMaxima: number;
  activa: boolean;
  anioEscolar: number;
  docenteTutorId?: number;
}
