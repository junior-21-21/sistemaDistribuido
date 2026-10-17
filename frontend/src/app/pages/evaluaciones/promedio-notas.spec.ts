import { literalDesdeValor, promedioPonderado, repartirIgual } from './promedio-notas';

describe('promedio-notas', () => {
  it('pondera cada columna según su porcentaje', () => {
    // 30 % A (3) + 70 % AD (4) = 3,7 → AD
    const r = promedioPonderado({ Tarea: 'A', Examen: 'AD' }, ['Tarea', 'Examen'], { Tarea: 30, Examen: 70 });
    expect(r.valor).toBe(3.7);
    expect(r.literal).toBe('AD');
    expect(r.parcial).toBe(false);
  });

  it('sin pesos todas las columnas valen igual', () => {
    const r = promedioPonderado({ T1: 'B', T2: 'A' }, ['T1', 'T2'], {});
    expect(r.valor).toBe(2.5);
    expect(r.literal).toBe('A');
  });

  it('marca parcial si falta una nota y promedia solo lo registrado', () => {
    const r = promedioPonderado({ T1: 'C' }, ['T1', 'T2'], { T1: 50, T2: 50 });
    expect(r.valor).toBe(1);
    expect(r.literal).toBe('C');
    expect(r.parcial).toBe(true);
  });

  it('devuelve vacío cuando no hay notas', () => {
    expect(promedioPonderado({}, ['T1'], { T1: 100 }).literal).toBeNull();
  });

  it('convierte el valor a la escala literal', () => {
    expect(literalDesdeValor(3.5)).toBe('AD');
    expect(literalDesdeValor(3.49)).toBe('A');
    expect(literalDesdeValor(1.5)).toBe('B');
    expect(literalDesdeValor(1.49)).toBe('C');
  });

  it('reparte 100 % en partes iguales', () => {
    const pesos = repartirIgual(['a', 'b', 'c']);
    expect(pesos['a']).toBe(33.33);
    expect(Object.values(pesos).reduce((t, v) => t + v, 0)).toBeCloseTo(100, 5);
  });
});
