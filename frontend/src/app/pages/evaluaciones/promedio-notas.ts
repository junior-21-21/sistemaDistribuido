/**
 * Promedio ponderado del bimestre con la escala literal del CNEB.
 * Equivalencias: AD = 4, A = 3, B = 2, C = 1. Resultado: AD ≥ 3,5 · A ≥ 2,5 · B ≥ 1,5 · C < 1,5.
 */
export type Literal = 'AD' | 'A' | 'B' | 'C';

export const VALOR_LITERAL: Record<Literal, number> = { AD: 4, A: 3, B: 2, C: 1 };

export interface ResultadoPromedio {
  /** Valor ponderado en escala 1–4 (null si no hay notas). */
  valor: number | null;
  literal: Literal | null;
  /** Falta alguna nota de una columna con peso. */
  parcial: boolean;
}

export function literalDesdeValor(valor: number): Literal {
  if (valor >= 3.5) return 'AD';
  if (valor >= 2.5) return 'A';
  if (valor >= 1.5) return 'B';
  return 'C';
}

/**
 * Calcula el promedio con los pesos (%) de cada columna. Si ninguna columna tiene peso,
 * todas pesan igual. Las columnas sin nota no cuentan y el resultado se marca como parcial.
 */
export function promedioPonderado(
  notas: Record<string, string | undefined>,
  columnas: string[],
  pesos: Record<string, number | null | undefined>
): ResultadoPromedio {
  const conPeso = columnas.some(columna => (pesos[columna] ?? 0) > 0);
  let suma = 0;
  let pesoUsado = 0;
  let parcial = false;
  for (const columna of columnas) {
    const peso = conPeso ? (pesos[columna] ?? 0) : 1;
    if (peso <= 0) continue;
    const literal = notas[columna] as Literal | undefined;
    if (!literal || !(literal in VALOR_LITERAL)) {
      parcial = true;
      continue;
    }
    suma += VALOR_LITERAL[literal] * peso;
    pesoUsado += peso;
  }
  if (!pesoUsado) return { valor: null, literal: null, parcial };
  const valor = Math.round((suma / pesoUsado) * 100) / 100;
  return { valor, literal: literalDesdeValor(valor), parcial };
}

/** Reparte 100 % en partes iguales; el residuo de redondeo va a la última columna. */
export function repartirIgual(columnas: string[]): Record<string, number> {
  if (!columnas.length) return {};
  const base = Math.floor((100 / columnas.length) * 100) / 100;
  const pesos: Record<string, number> = {};
  columnas.forEach((columna, i) => {
    pesos[columna] = i === columnas.length - 1
      ? Math.round((100 - base * (columnas.length - 1)) * 100) / 100
      : base;
  });
  return pesos;
}
