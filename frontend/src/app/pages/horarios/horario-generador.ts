/**
 * Motor de generación de horarios escolares (sin dependencias de Angular, probado aparte).
 *
 * Recibe los parámetros que elige Coordinación Académica, la carga curso-docente de cada
 * sección, las aulas y lo que ya está ocupado fuera de las secciones a generar. Coloca los
 * períodos respetando:
 *  - sin cruces de docente, aula ni sección;
 *  - máximo de períodos del mismo curso por día;
 *  - máximo de períodos seguidos de un docente;
 *  - bloques dobles (opcional) sin partirlos con un recreo;
 *  - las clases que el usuario fijó a mano.
 * Prueba varias variantes y devuelve la que deja menos períodos sin ubicar.
 */

export interface Recreo {
  /** Se toma después de este número de período (1 = después del primero). */
  despuesDe: number;
  minutos: number;
}

export interface ParametrosHorario {
  horaInicio: string;
  minutosPeriodo: number;
  periodosPorDia: number;
  recreos: Recreo[];
  dias: string[];
  maxMismoCursoPorDia: number;
  preferirBloquesDobles: boolean;
  /** 0 = sin límite. */
  maxSeguidosDocente: number;
  aulaFijaPorSeccion: boolean;
}

export interface BloqueTiempo {
  inicio: string;
  fin: string;
  esReceso: boolean;
  /** Índice del período (0..n-1); -1 en recreos. */
  periodo: number;
}

export interface CargaGeneracion {
  asignacionId: number;
  cursoId: number;
  docenteId: number;
  periodos: number;
}

export interface Ubicacion {
  asignacionId: number;
  aulaId: number;
}

/** seccionId -> dia -> periodo -> ubicación */
export type Grilla = Map<number, Map<string, Map<number, Ubicacion>>>;

export interface EntradaGeneracion {
  parametros: ParametrosHorario;
  secciones: { id: number; cargas: CargaGeneracion[] }[];
  aulas: number[];
  /** Claves "docenteId|dia|periodo" ocupadas por secciones que no se regeneran. */
  docentesOcupados: Set<string>;
  /** Claves "aulaId|dia|periodo" ocupadas por secciones que no se regeneran. */
  aulasOcupadas: Set<string>;
  /** Clases fijadas a mano que deben conservarse. */
  fijadas: { seccionId: number; dia: string; periodo: number; asignacionId: number; aulaId: number }[];
  intentos?: number;
  semilla?: number;
}

export interface ResultadoGeneracion {
  grilla: Grilla;
  /** asignacionId -> períodos que no se pudieron ubicar */
  faltantes: Map<number, number>;
  totalFaltantes: number;
}

export const DIAS_SEMANA = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado'];

export const DIA_ETIQUETA: Record<string, string> = {
  lunes: 'Lunes', martes: 'Martes', miercoles: 'Miércoles', jueves: 'Jueves', viernes: 'Viernes', sabado: 'Sábado'
};

export function parametrosPorDefecto(turno: string): ParametrosHorario {
  return {
    horaInicio: turno === 'TARDE' ? '13:30' : '07:30',
    minutosPeriodo: 45,
    periodosPorDia: 7,
    recreos: [{ despuesDe: 3, minutos: 15 }, { despuesDe: 6, minutos: 15 }],
    dias: ['lunes', 'martes', 'miercoles', 'jueves', 'viernes'],
    maxMismoCursoPorDia: 2,
    preferirBloquesDobles: true,
    maxSeguidosDocente: 4,
    aulaFijaPorSeccion: true
  };
}

/** Normaliza y acota los valores para que nunca produzcan un horario imposible de dibujar. */
export function sanearParametros(p: ParametrosHorario): ParametrosHorario {
  const periodos = acotar(Math.round(p.periodosPorDia), 1, 12);
  const dias = DIAS_SEMANA.filter(dia => p.dias.includes(dia));
  return {
    horaInicio: /^\d{2}:\d{2}$/.test(p.horaInicio) ? p.horaInicio : '07:30',
    minutosPeriodo: acotar(Math.round(p.minutosPeriodo), 30, 90),
    periodosPorDia: periodos,
    recreos: p.recreos
      .map(r => ({ despuesDe: acotar(Math.round(r.despuesDe), 1, periodos - 1), minutos: acotar(Math.round(r.minutos), 5, 60) }))
      .filter((r, i, lista) => periodos > 1 && lista.findIndex(x => x.despuesDe === r.despuesDe) === i)
      .sort((a, b) => a.despuesDe - b.despuesDe),
    dias: dias.length ? dias : ['lunes'],
    maxMismoCursoPorDia: acotar(Math.round(p.maxMismoCursoPorDia), 1, periodos),
    preferirBloquesDobles: !!p.preferirBloquesDobles,
    maxSeguidosDocente: acotar(Math.round(p.maxSeguidosDocente), 0, periodos),
    aulaFijaPorSeccion: p.aulaFijaPorSeccion !== false
  };
}

export function construirBloques(parametros: ParametrosHorario): BloqueTiempo[] {
  const p = sanearParametros(parametros);
  const bloques: BloqueTiempo[] = [];
  let minuto = aMinutos(p.horaInicio);
  for (let i = 0; i < p.periodosPorDia; i++) {
    bloques.push({ inicio: aHora(minuto), fin: aHora(minuto + p.minutosPeriodo), esReceso: false, periodo: i });
    minuto += p.minutosPeriodo;
    const recreo = p.recreos.find(r => r.despuesDe === i + 1);
    if (recreo && i < p.periodosPorDia - 1) {
      bloques.push({ inicio: aHora(minuto), fin: aHora(minuto + recreo.minutos), esReceso: true, periodo: -1 });
      minuto += recreo.minutos;
    }
  }
  return bloques;
}

/** Hora de término de la jornada con estos parámetros (para mostrarla al usuario). */
export function horaFin(parametros: ParametrosHorario): string {
  const bloques = construirBloques(parametros);
  return bloques[bloques.length - 1]?.fin ?? parametros.horaInicio;
}

export function generarHorario(entrada: EntradaGeneracion): ResultadoGeneracion {
  const intentos = Math.max(1, entrada.intentos ?? 40);
  let mejor: ResultadoGeneracion | null = null;
  for (let i = 0; i < intentos; i++) {
    const resultado = intentar(entrada, (entrada.semilla ?? 1) * 7919 + i * 104729);
    if (!mejor || resultado.totalFaltantes < mejor.totalFaltantes) mejor = resultado;
    if (mejor.totalFaltantes === 0) break;
  }
  return mejor!;
}

// ------------------------------------------------------------------ internos

interface Unidad {
  seccionId: number;
  carga: CargaGeneracion;
  tamano: 1 | 2;
}

function intentar(entrada: EntradaGeneracion, semilla: number): ResultadoGeneracion {
  const p = sanearParametros(entrada.parametros);
  const azar = generadorAleatorio(semilla);
  const docentes = new Set(entrada.docentesOcupados);
  const aulas = new Set(entrada.aulasOcupadas);
  const grilla: Grilla = new Map();
  const usados = new Map<number, number>();
  const porDia = new Map<string, number>(); // "asignacionId|dia" -> períodos

  const cargaPorAsignacion = new Map<number, { seccionId: number; carga: CargaGeneracion }>();
  for (const seccion of entrada.secciones) {
    grilla.set(seccion.id, new Map(p.dias.map(dia => [dia, new Map<number, Ubicacion>()])));
    for (const carga of seccion.cargas) cargaPorAsignacion.set(carga.asignacionId, { seccionId: seccion.id, carga });
  }

  const colocar = (seccionId: number, carga: CargaGeneracion, dia: string, periodo: number, aulaId: number) => {
    grilla.get(seccionId)!.get(dia)!.set(periodo, { asignacionId: carga.asignacionId, aulaId });
    docentes.add(`${carga.docenteId}|${dia}|${periodo}`);
    aulas.add(`${aulaId}|${dia}|${periodo}`);
    usados.set(carga.asignacionId, (usados.get(carga.asignacionId) ?? 0) + 1);
    porDia.set(`${carga.asignacionId}|${dia}`, (porDia.get(`${carga.asignacionId}|${dia}`) ?? 0) + 1);
  };

  // 1. Clases fijadas por el usuario.
  for (const fija of entrada.fijadas) {
    const ref = cargaPorAsignacion.get(fija.asignacionId);
    const dias = grilla.get(fija.seccionId);
    if (!ref || !dias?.has(fija.dia) || fija.periodo >= p.periodosPorDia) continue;
    colocar(fija.seccionId, ref.carga, fija.dia, fija.periodo, fija.aulaId);
  }

  // 2. Aula "de casa" para cada sección (la menos usada por otras secciones).
  const aulaCasa = new Map<number, number>();
  if (p.aulaFijaPorSeccion) {
    const libres = [...entrada.aulas].sort((a, b) => usoAula(a, aulas) - usoAula(b, aulas));
    entrada.secciones.forEach((seccion, i) => {
      const fijadaDeLaSeccion = entrada.fijadas.find(f => f.seccionId === seccion.id)?.aulaId;
      aulaCasa.set(seccion.id, fijadaDeLaSeccion ?? libres[i % Math.max(1, libres.length)]);
    });
  }

  // 3. Unidades por ubicar (dobles primero, luego por docente más cargado).
  const demandaDocente = new Map<number, number>();
  for (const seccion of entrada.secciones) {
    for (const carga of seccion.cargas) {
      demandaDocente.set(carga.docenteId, (demandaDocente.get(carga.docenteId) ?? 0) + carga.periodos);
    }
  }
  const unidades: Unidad[] = [];
  for (const seccion of entrada.secciones) {
    for (const carga of seccion.cargas) {
      let restantes = Math.max(0, carga.periodos - (usados.get(carga.asignacionId) ?? 0));
      if (p.preferirBloquesDobles && p.maxMismoCursoPorDia >= 2) {
        while (restantes >= 2) {
          unidades.push({ seccionId: seccion.id, carga, tamano: 2 });
          restantes -= 2;
        }
      }
      for (let i = 0; i < restantes; i++) unidades.push({ seccionId: seccion.id, carga, tamano: 1 });
    }
  }
  const ruido = new Map(unidades.map(u => [u, azar()]));
  unidades.sort((a, b) => (b.tamano - a.tamano)
    || ((demandaDocente.get(b.carga.docenteId) ?? 0) - (demandaDocente.get(a.carga.docenteId) ?? 0))
    || (ruido.get(a)! - ruido.get(b)!));

  const recreoTras = new Set(p.recreos.map(r => r.despuesDe));
  const periodos = Array.from({ length: p.periodosPorDia }, (_, i) => i);

  const aulaPara = (seccionId: number, dia: string, posiciones: number[]): number | null => {
    const preferida = aulaCasa.get(seccionId);
    const candidatas = preferida != null ? [preferida, ...entrada.aulas.filter(a => a !== preferida)] : barajar(entrada.aulas, azar);
    return candidatas.find(aula => posiciones.every(pos => !aulas.has(`${aula}|${dia}|${pos}`))) ?? null;
  };

  const seguidosDocente = (docenteId: number, dia: string, posiciones: number[]): number => {
    const ocupado = (pos: number) => posiciones.includes(pos) || docentes.has(`${docenteId}|${dia}|${pos}`);
    let maximo = 0;
    let racha = 0;
    for (const pos of periodos) {
      racha = ocupado(pos) ? racha + 1 : 0;
      maximo = Math.max(maximo, racha);
    }
    return maximo;
  };

  const intentarUbicar = (unidad: Unidad): boolean => {
    const { seccionId, carga, tamano } = unidad;
    const filaSeccion = grilla.get(seccionId)!;
    // Días donde el curso aún no aparece primero, para repartirlo en la semana.
    const dias = barajar(p.dias, azar).sort((a, b) =>
      (porDia.get(`${carga.asignacionId}|${a}`) ?? 0) - (porDia.get(`${carga.asignacionId}|${b}`) ?? 0));
    for (const dia of dias) {
      if ((porDia.get(`${carga.asignacionId}|${dia}`) ?? 0) + tamano > p.maxMismoCursoPorDia) continue;
      for (const inicio of barajar(periodos, azar)) {
        const posiciones = tamano === 2 ? [inicio, inicio + 1] : [inicio];
        if (posiciones[posiciones.length - 1] >= p.periodosPorDia) continue;
        if (tamano === 2 && recreoTras.has(inicio + 1)) continue; // no partir un bloque doble con el recreo
        if (posiciones.some(pos => filaSeccion.get(dia)!.has(pos))) continue;
        if (posiciones.some(pos => docentes.has(`${carga.docenteId}|${dia}|${pos}`))) continue;
        if (p.maxSeguidosDocente > 0 && seguidosDocente(carga.docenteId, dia, posiciones) > p.maxSeguidosDocente) continue;
        const aula = aulaPara(seccionId, dia, posiciones);
        if (aula == null) continue;
        posiciones.forEach(pos => colocar(seccionId, carga, dia, pos, aula));
        return true;
      }
    }
    return false;
  };

  for (let i = 0; i < unidades.length; i++) {
    const unidad = unidades[i];
    if (intentarUbicar(unidad)) continue;
    if (unidad.tamano === 2) {
      // Si el bloque doble no entra, se intenta como dos períodos sueltos.
      unidades.splice(i + 1, 0, { ...unidad, tamano: 1 }, { ...unidad, tamano: 1 });
    }
  }

  const faltantes = new Map<number, number>();
  let total = 0;
  for (const seccion of entrada.secciones) {
    for (const carga of seccion.cargas) {
      const falta = Math.max(0, carga.periodos - (usados.get(carga.asignacionId) ?? 0));
      if (falta > 0) faltantes.set(carga.asignacionId, falta);
      total += falta;
    }
  }
  return { grilla, faltantes, totalFaltantes: total };
}

function usoAula(aula: number, ocupadas: Set<string>): number {
  let n = 0;
  for (const clave of ocupadas) if (clave.startsWith(`${aula}|`)) n++;
  return n;
}

function generadorAleatorio(semilla: number): () => number {
  // mulberry32: reproducible para una misma semilla.
  let t = semilla >>> 0;
  return () => {
    t = (t + 0x6d2b79f5) >>> 0;
    let r = Math.imul(t ^ (t >>> 15), 1 | t);
    r = (r + Math.imul(r ^ (r >>> 7), 61 | r)) ^ r;
    return ((r ^ (r >>> 14)) >>> 0) / 4294967296;
  };
}

function barajar<T>(lista: T[], azar: () => number): T[] {
  const copia = [...lista];
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(azar() * (i + 1));
    [copia[i], copia[j]] = [copia[j], copia[i]];
  }
  return copia;
}

function acotar(valor: number, min: number, max: number): number {
  if (!Number.isFinite(valor)) return min;
  return Math.min(max, Math.max(min, valor));
}

export function aMinutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

function aHora(minutos: number): string {
  const h = Math.floor(minutos / 60) % 24;
  const m = minutos % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}
