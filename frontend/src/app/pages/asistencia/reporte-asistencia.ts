import { Asistencia } from '../../services/nota.service';
import { CeldaXlsx, EstiloXlsx, HojaXlsx } from '../../shared/excel/xlsx';
import { CeldaPdf, TablaPdf } from '../../shared/pdf/pdf-tabla';

export type PeriodoReporte = 'DIA' | 'SEMANA' | 'MES';

export interface ContextoReporte {
  periodo: PeriodoReporte;
  desde: string;
  hasta: string;
  curso: string;
  seccion: string;
  turno?: string;
  docente?: string;
  generadoPor: string;
}

export interface AlumnoReporte { id: number; nombre: string; apellido: string; dni?: string; }

interface Sesion { clave: string; fecha: string; horarioId: number; hora: string; }

const LETRA: Record<string, string> = { PRESENTE: 'P', TARDE: 'T', FALTA: 'F', JUSTIFICADO: 'J' };
const COLOR_PDF: Record<string, { fondo: string; color: string }> = {
  P: { fondo: '#d6f0de', color: '#17663a' }, T: { fondo: '#fdecc8', color: '#8a5a0b' },
  F: { fondo: '#fbd9d5', color: '#a3271c' }, J: { fondo: '#d8e7f6', color: '#1f5585' }
};
const ESTILO_XLSX: Record<string, EstiloXlsx> = { P: EstiloXlsx.NotaAD, T: EstiloXlsx.NotaB, F: EstiloXlsx.NotaC, J: EstiloXlsx.NotaA };
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'setiembre', 'octubre', 'noviembre', 'diciembre'];
const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

function aFecha(iso: string): Date { return new Date(`${iso}T12:00:00`); }
function iso(fecha: Date): string {
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}
function corta(isoFecha: string): string { const [, m, d] = isoFecha.split('-'); return `${d}/${m}`; }

/** Rango de fechas del periodo que contiene a la fecha de referencia (la semana va de lunes a viernes). */
export function rangoDelPeriodo(periodo: PeriodoReporte, referencia: string): { desde: string; hasta: string } {
  const fecha = aFecha(referencia);
  if (periodo === 'DIA') return { desde: referencia, hasta: referencia };
  if (periodo === 'SEMANA') {
    const lunes = new Date(fecha);
    lunes.setDate(fecha.getDate() - ((fecha.getDay() + 6) % 7));
    const viernes = new Date(lunes);
    viernes.setDate(lunes.getDate() + 4);
    return { desde: iso(lunes), hasta: iso(viernes) };
  }
  return { desde: iso(new Date(fecha.getFullYear(), fecha.getMonth(), 1)), hasta: iso(new Date(fecha.getFullYear(), fecha.getMonth() + 1, 0)) };
}

/** Descripción legible del periodo: «miércoles 14 de octubre», «semana del 12 al 16 de octubre», «octubre de 2026». */
export function describirPeriodo(periodo: PeriodoReporte, desde: string, hasta: string): string {
  const a = aFecha(desde);
  const b = aFecha(hasta);
  if (periodo === 'DIA') return `${DIAS[a.getDay()]} ${a.getDate()} de ${MESES[a.getMonth()]} de ${a.getFullYear()}`;
  if (periodo === 'MES') return `${MESES[a.getMonth()]} de ${a.getFullYear()}`;
  const inicio = a.getMonth() === b.getMonth() ? `${a.getDate()}` : `${a.getDate()} de ${MESES[a.getMonth()]}`;
  return `semana del ${inicio} al ${b.getDate()} de ${MESES[b.getMonth()]}`;
}

export interface MatrizAsistencia {
  sesiones: Sesion[];
  filas: { alumno: AlumnoReporte; marcas: string[]; P: number; T: number; F: number; J: number; porcentaje: number | null }[];
  totales: { P: number; T: number; F: number; J: number; porcentaje: number | null };
}

export function construirMatriz(registros: Asistencia[], alumnos: AlumnoReporte[], horaDe: (horarioId: number) => string): MatrizAsistencia {
  const sesionesMapa = new Map<string, Sesion>();
  const marcas = new Map<string, string>();
  for (const r of registros) {
    const clave = `${r.fecha}|${r.horarioId}`;
    if (!sesionesMapa.has(clave)) sesionesMapa.set(clave, { clave, fecha: r.fecha, horarioId: r.horarioId, hora: horaDe(r.horarioId) });
    marcas.set(`${r.estudianteId}|${clave}`, LETRA[r.estado] ?? '');
  }
  const sesiones = [...sesionesMapa.values()].sort((a, b) => a.fecha.localeCompare(b.fecha) || a.hora.localeCompare(b.hora));
  const lista = [...alumnos].sort((a, b) => `${a.apellido} ${a.nombre}`.localeCompare(`${b.apellido} ${b.nombre}`, 'es'));
  const totales = { P: 0, T: 0, F: 0, J: 0, porcentaje: null as number | null };
  const filas = lista.map(alumno => {
    const fila = { alumno, marcas: sesiones.map(s => marcas.get(`${alumno.id}|${s.clave}`) ?? ''), P: 0, T: 0, F: 0, J: 0, porcentaje: null as number | null };
    for (const m of fila.marcas) if (m) { fila[m as 'P']++; totales[m as 'P']++; }
    const total = fila.P + fila.T + fila.F + fila.J;
    fila.porcentaje = total ? Math.round(((fila.P + fila.T) * 1000) / total) / 10 : null;
    return fila;
  });
  const total = totales.P + totales.T + totales.F + totales.J;
  totales.porcentaje = total ? Math.round(((totales.P + totales.T) * 1000) / total) / 10 : null;
  return { sesiones, filas, totales };
}

function encabezadoSesion(s: Sesion, periodo: PeriodoReporte): { titulo: string; detalle: string } {
  return periodo === 'DIA' ? { titulo: s.hora, detalle: 'Bloque' } : { titulo: corta(s.fecha), detalle: s.hora };
}

export function reportePdf(m: MatrizAsistencia, ctx: ContextoReporte): TablaPdf {
  const enRiesgo = m.filas.filter(f => f.porcentaje !== null && f.porcentaje < 85).length;
  const filas: CeldaPdf[][] = m.filas.map((f, i) => [
    { texto: String(i + 1), color: '#6b7a72' },
    { texto: `${f.alumno.apellido}, ${f.alumno.nombre}`, negrita: true },
    ...f.marcas.map(letra => letra ? { texto: letra, negrita: true, ...COLOR_PDF[letra] } : { texto: '·', color: '#b6c0ba' }),
    { texto: String(f.P), color: '#17663a' }, { texto: String(f.T), color: '#8a5a0b' },
    { texto: String(f.F), color: '#a3271c' }, { texto: String(f.J), color: '#1f5585' },
    f.porcentaje === null ? { texto: '—', color: '#b6c0ba' }
      : { texto: `${f.porcentaje.toFixed(1)} %`, negrita: true, color: f.porcentaje < 85 ? '#a3271c' : '#17663a', fondo: f.porcentaje < 85 ? '#fbd9d5' : undefined }
  ]);
  return {
    titulo: 'Reporte de asistencia',
    subtitulo: `${ctx.curso} · ${ctx.seccion} · ${describirPeriodo(ctx.periodo, ctx.desde, ctx.hasta)}`,
    datos: [['Sección', ctx.seccion + (ctx.turno ? ` (${ctx.turno})` : '')], ['Curso', ctx.curso], ...(ctx.docente ? [['Docente', ctx.docente] as [string, string]] : []),
      ['Periodo', `${corta(ctx.desde)} al ${corta(ctx.hasta)}`], ['Clases', String(m.sesiones.length)]],
    resumen: [
      { etiqueta: 'Asistencia del periodo', valor: m.totales.porcentaje === null ? '—' : `${m.totales.porcentaje.toFixed(1)} %`, color: '#1f4d3a' },
      { etiqueta: 'Presentes', valor: String(m.totales.P), color: '#17663a' },
      { etiqueta: 'Tardanzas', valor: String(m.totales.T), color: '#b0730f' },
      { etiqueta: 'Faltas', valor: String(m.totales.F), color: '#a3271c' },
      { etiqueta: 'Justificadas', valor: String(m.totales.J), color: '#1f5585' },
      { etiqueta: 'Estudiantes bajo 85 %', valor: String(enRiesgo), color: enRiesgo ? '#a3271c' : '#17663a' }
    ],
    columnas: [
      { titulo: 'N°', ancho: 2.2 }, { titulo: 'Estudiante', ancho: 15, alinear: 'izq' },
      ...m.sesiones.map(s => ({ ...encabezadoSesion(s, ctx.periodo), ancho: 2.6 })),
      { titulo: 'P', ancho: 2.4 }, { titulo: 'T', ancho: 2.4 }, { titulo: 'F', ancho: 2.4 }, { titulo: 'J', ancho: 2.4 },
      { titulo: '% Asist.', ancho: 4.6 }
    ],
    filas,
    leyenda: [{ texto: 'P  Presente', color: '#d6f0de' }, { texto: 'T  Tardanza', color: '#fdecc8' }, { texto: 'F  Falta', color: '#fbd9d5' },
      { texto: 'J  Falta justificada', color: '#d8e7f6' }, { texto: '% Asist. = (P + T) / clases registradas', color: '#ffffff' }],
    pie: `I.E. Huamanga · Generado por ${ctx.generadoPor} el ${new Date().toLocaleString('es-PE')}`
  };
}

export function reporteXlsx(m: MatrizAsistencia, ctx: ContextoReporte): HojaXlsx {
  const ultimaColumna = 2 + m.sesiones.length + 5;
  const letra = (n: number) => { let s = ''; for (let k = n + 1; k > 0; k = Math.floor((k - 1) / 26)) s = String.fromCharCode(65 + ((k - 1) % 26)) + s; return s; };
  const vacias = (n: number): CeldaXlsx[] => Array.from({ length: n }, () => ({ v: null }));
  const filas: CeldaXlsx[][] = [
    [{ v: `Reporte de asistencia · ${ctx.curso}`, s: EstiloXlsx.Titulo }],
    [{ v: `${ctx.seccion}${ctx.turno ? ` (${ctx.turno})` : ''} · ${describirPeriodo(ctx.periodo, ctx.desde, ctx.hasta)}${ctx.docente ? ` · Docente: ${ctx.docente}` : ''}`, s: EstiloXlsx.Subtitulo }],
    [],
    [{ v: 'N°', s: EstiloXlsx.Encabezado }, { v: 'Estudiante', s: EstiloXlsx.Encabezado },
      ...m.sesiones.map(s => ({ v: ctx.periodo === 'DIA' ? s.hora : `${corta(s.fecha)} ${s.hora}`, s: EstiloXlsx.Encabezado })),
      ...['P', 'T', 'F', 'J', '% Asist.'].map(v => ({ v, s: EstiloXlsx.Encabezado }))]
  ];
  m.filas.forEach((f, i) => filas.push([
    { v: i + 1, s: EstiloXlsx.Texto }, { v: `${f.alumno.apellido}, ${f.alumno.nombre}`, s: EstiloXlsx.Texto },
    ...f.marcas.map(x => ({ v: x || null, s: x ? ESTILO_XLSX[x] : EstiloXlsx.Texto })),
    { v: f.P, s: EstiloXlsx.Peso }, { v: f.T, s: EstiloXlsx.Peso }, { v: f.F, s: EstiloXlsx.Peso }, { v: f.J, s: EstiloXlsx.Peso },
    { v: f.porcentaje, s: f.porcentaje !== null && f.porcentaje < 85 ? EstiloXlsx.NotaC : EstiloXlsx.Numero }
  ]));
  filas.push([{ v: null }, { v: 'Total del aula', s: EstiloXlsx.Peso }, ...vacias(m.sesiones.length).map(() => ({ v: null, s: EstiloXlsx.Texto })),
    { v: m.totales.P, s: EstiloXlsx.Peso }, { v: m.totales.T, s: EstiloXlsx.Peso }, { v: m.totales.F, s: EstiloXlsx.Peso },
    { v: m.totales.J, s: EstiloXlsx.Peso }, { v: m.totales.porcentaje, s: EstiloXlsx.Numero }]);
  filas.push([]);
  filas.push([{ v: 'P = Presente · T = Tardanza · F = Falta · J = Falta justificada · % Asist. = (P + T) / clases registradas', s: EstiloXlsx.Subtitulo }]);
  return {
    nombre: 'Asistencia',
    filas,
    anchos: [5, 34, ...m.sesiones.map(() => ctx.periodo === 'DIA' ? 9 : 11), 6, 6, 6, 6, 10],
    combinar: [`A1:${letra(ultimaColumna)}1`, `A2:${letra(ultimaColumna)}2`],
    inmovilizar: { columnas: 2, filas: 4 }
  };
}
