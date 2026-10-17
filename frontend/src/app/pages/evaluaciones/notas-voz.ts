/**
 * Interpreta frases dictadas por el docente para registrar calificaciones.
 * Ejemplos: "Juan Quispe A", "Quispe logro destacado", "Mamani, B", "número 5 C", "Ana Flores 15".
 *
 * La escala oficial del CNEB es literal (AD, A, B, C). Si se dicta un número (escala vigesimal
 * usada por costumbre), se convierte con la tabla habitual de equivalencias, que el colegio
 * puede ajustar en EQUIVALENCIA_VIGESIMAL.
 */

export interface AlumnoVoz {
  id: number;
  nombre: string;
  apellido: string;
}

export type Calificacion = 'AD' | 'A' | 'B' | 'C';

export type ResultadoVoz =
  | { tipo: 'ok'; alumno: AlumnoVoz; nota: Calificacion; textoNota: string }
  | { tipo: 'ambiguo'; candidatos: AlumnoVoz[]; nota: Calificacion }
  | { tipo: 'sin-alumno'; nota: Calificacion }
  | { tipo: 'sin-nota' };

/** Rango mínimo de cada nivel en escala vigesimal (18–20 AD, 14–17 A, 11–13 B, 0–10 C). */
export const EQUIVALENCIA_VIGESIMAL: { desde: number; nota: Calificacion }[] = [
  { desde: 18, nota: 'AD' },
  { desde: 14, nota: 'A' },
  { desde: 11, nota: 'B' },
  { desde: 0, nota: 'C' },
];

const NUMEROS: Record<string, number> = {
  cero: 0, uno: 1, una: 1, dos: 2, tres: 3, cuatro: 4, cinco: 5, seis: 6, siete: 7, ocho: 8, nueve: 9, diez: 10,
  once: 11, doce: 12, trece: 13, catorce: 14, quince: 15, dieciseis: 16, diecisiete: 17, dieciocho: 18,
  diecinueve: 19, veinte: 20, veintiuno: 21, veintidos: 22, veintitres: 23, veinticuatro: 24, veinticinco: 25,
  veintiseis: 26, veintisiete: 27, veintiocho: 28, veintinueve: 29, treinta: 30, cuarenta: 40,
};

/** Frases de la escala literal, de la más larga a la más corta. */
const FRASES_NOTA: { patron: RegExp; nota: Calificacion }[] = [
  { patron: /\blogro destacado\b/, nota: 'AD' },
  { patron: /\blogro (esperado|previsto)\b/, nota: 'A' },
  { patron: /\ben proceso\b/, nota: 'B' },
  { patron: /\ben inicio\b/, nota: 'C' },
  { patron: /\b(a d|ad|a de|ade|hade)\b/, nota: 'AD' },
  { patron: /\b(be|b|ve|v)\b/, nota: 'B' },
  { patron: /\b(ce|c|se)\b/, nota: 'C' },
  { patron: /\b(a|ha)\b/, nota: 'A' },
];

export function normalizar(texto: string): string {
  return texto
    .toLowerCase()
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9ñ\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export function aLiteral(valor: number): Calificacion | null {
  if (!Number.isFinite(valor) || valor < 0 || valor > 20) return null;
  return EQUIVALENCIA_VIGESIMAL.find(e => valor >= e.desde)!.nota;
}

/** Separa la frase en "alumno" y "nota" buscando la nota al final de lo dictado. */
export function interpretar(frase: string, alumnos: AlumnoVoz[]): ResultadoVoz {
  const texto = normalizar(frase);
  if (!texto) return { tipo: 'sin-nota' };

  const extraida = extraerNota(texto);
  if (!extraida) return { tipo: 'sin-nota' };
  const resto = extraida.resto;

  // "número 5" / "alumno 5" / "el 5": por orden en la lista.
  const porOrden = resto.match(/\b(numero|nro|alumno|alumna|el|la)\s+(\d{1,2}|[a-z]+)\b/);
  if (porOrden) {
    const n = /^\d+$/.test(porOrden[2]) ? Number(porOrden[2]) : NUMEROS[porOrden[2]];
    if (n && n >= 1 && n <= alumnos.length) {
      return { tipo: 'ok', alumno: alumnos[n - 1], nota: extraida.nota, textoNota: extraida.texto };
    }
  }

  const palabras = resto.split(' ').filter(p => p.length >= 2 && !['el', 'la', 'de', 'del', 'y', 'nota', 'alumno', 'alumna', 'estudiante'].includes(p));
  if (!palabras.length) return { tipo: 'sin-alumno', nota: extraida.nota };

  const puntuados = alumnos
    .map(alumno => ({ alumno, puntos: puntaje(palabras, normalizar(`${alumno.nombre} ${alumno.apellido}`).split(' ')) }))
    .filter(x => x.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos);

  if (!puntuados.length) return { tipo: 'sin-alumno', nota: extraida.nota };
  const mejores = puntuados.filter(x => x.puntos === puntuados[0].puntos);
  if (mejores.length > 1) return { tipo: 'ambiguo', candidatos: mejores.map(x => x.alumno), nota: extraida.nota };
  return { tipo: 'ok', alumno: mejores[0].alumno, nota: extraida.nota, textoNota: extraida.texto };
}

function extraerNota(texto: string): { nota: Calificacion; texto: string; resto: string } | null {
  // 1. Número al final (dígitos o palabras): "juan quispe 15", "juan quispe quince".
  const digitos = texto.match(/(\d{1,2})\s*$/);
  if (digitos) {
    const nota = aLiteral(Number(digitos[1]));
    if (nota) return { nota, texto: digitos[1], resto: texto.slice(0, digitos.index).trim() };
  }
  const palabras = texto.split(' ');
  for (let largo = 3; largo >= 1; largo--) {
    if (palabras.length <= largo) continue;
    const cola = palabras.slice(-largo).join(' ');
    const valor = numeroEnPalabras(cola);
    if (valor != null) {
      const nota = aLiteral(valor);
      if (nota) return { nota, texto: String(valor), resto: palabras.slice(0, -largo).join(' ') };
    }
  }
  // 2. Escala literal, buscando la última aparición.
  for (const { patron, nota } of FRASES_NOTA) {
    const global = new RegExp(patron.source, 'g');
    let ultima: RegExpExecArray | null = null;
    let m: RegExpExecArray | null;
    while ((m = global.exec(texto))) ultima = m;
    if (ultima && ultima.index > 0) {
      const resto = (texto.slice(0, ultima.index) + ' ' + texto.slice(ultima.index + ultima[0].length)).trim();
      return { nota, texto: nota, resto };
    }
  }
  return null;
}

function numeroEnPalabras(texto: string): number | null {
  if (texto in NUMEROS) return NUMEROS[texto];
  const partes = texto.split(' ');
  if (partes.length === 3 && partes[1] === 'y' && partes[0] in NUMEROS && partes[2] in NUMEROS) {
    return NUMEROS[partes[0]] + NUMEROS[partes[2]];
  }
  return null;
}

function puntaje(dichas: string[], delAlumno: string[]): number {
  let total = 0;
  for (const dicha of dichas) {
    let mejor = 0;
    for (const propia of delAlumno) {
      if (dicha === propia) mejor = Math.max(mejor, 3);
      else if (dicha.length >= 4 && propia.length >= 4 && distancia(dicha, propia) <= 1) mejor = Math.max(mejor, 2);
      else if (dicha.length >= 4 && propia.startsWith(dicha)) mejor = Math.max(mejor, 1);
    }
    total += mejor;
  }
  return total;
}

/** Distancia de edición (Levenshtein) para tolerar errores del reconocimiento de voz. */
function distancia(a: string, b: string): number {
  const fila = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    let previo = fila[0];
    fila[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const temp = fila[j];
      fila[j] = Math.min(fila[j] + 1, fila[j - 1] + 1, previo + (a[i - 1] === b[j - 1] ? 0 : 1));
      previo = temp;
    }
  }
  return fila[b.length];
}
