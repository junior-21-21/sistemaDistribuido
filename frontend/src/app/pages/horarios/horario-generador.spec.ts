import {
  EntradaGeneracion,
  Grilla,
  construirBloques,
  generarHorario,
  horaFin,
  parametrosPorDefecto,
  sanearParametros
} from './horario-generador';

function entradaBase(): EntradaGeneracion {
  const parametros = parametrosPorDefecto('MANANA');
  // Dos secciones que comparten docentes: obliga a evitar cruces.
  const cargas = (base: number) => [
    { asignacionId: base + 1, cursoId: 1, docenteId: 10, periodos: 5 },
    { asignacionId: base + 2, cursoId: 2, docenteId: 11, periodos: 5 },
    { asignacionId: base + 3, cursoId: 3, docenteId: 12, periodos: 4 },
    { asignacionId: base + 4, cursoId: 4, docenteId: 13, periodos: 3 },
  ];
  return {
    parametros,
    secciones: [{ id: 1, cargas: cargas(100) }, { id: 2, cargas: cargas(200) }],
    aulas: [501, 502, 503],
    docentesOcupados: new Set(),
    aulasOcupadas: new Set(),
    fijadas: [],
    semilla: 3
  };
}

function ocupacion(grilla: Grilla, entrada: EntradaGeneracion) {
  const docentePorAsignacion = new Map(entrada.secciones.flatMap(s => s.cargas.map(c => [c.asignacionId, c.docenteId] as const)));
  const docentes = new Map<string, number>();
  const aulas = new Map<string, number>();
  const porCursoDia = new Map<string, number>();
  for (const [, dias] of grilla) {
    for (const [dia, periodos] of dias) {
      for (const [periodo, ubicacion] of periodos) {
        const d = `${docentePorAsignacion.get(ubicacion.asignacionId)}|${dia}|${periodo}`;
        docentes.set(d, (docentes.get(d) ?? 0) + 1);
        const a = `${ubicacion.aulaId}|${dia}|${periodo}`;
        aulas.set(a, (aulas.get(a) ?? 0) + 1);
        const c = `${ubicacion.asignacionId}|${dia}`;
        porCursoDia.set(c, (porCursoDia.get(c) ?? 0) + 1);
      }
    }
  }
  return { docentes, aulas, porCursoDia };
}

describe('generador de horarios', () => {
  it('construye la jornada según los parámetros, con recreos', () => {
    const bloques = construirBloques(parametrosPorDefecto('MANANA'));
    expect(bloques[0]).toEqual({ inicio: '07:30', fin: '08:15', esReceso: false, periodo: 0 });
    expect(bloques.filter(b => b.esReceso).length).toBe(2);
    expect(bloques[3]).toMatchObject({ inicio: '09:45', fin: '10:00', esReceso: true });
    expect(horaFin(parametrosPorDefecto('MANANA'))).toBe('13:15');

    const cortos = construirBloques({ ...parametrosPorDefecto('TARDE'), minutosPeriodo: 40, periodosPorDia: 6, recreos: [{ despuesDe: 3, minutos: 20 }] });
    expect(cortos[0].inicio).toBe('13:30');
    expect(cortos.at(-1)?.fin).toBe('17:50');
  });

  it('acota parámetros absurdos para no romper la pantalla', () => {
    const p = sanearParametros({ ...parametrosPorDefecto('MANANA'), periodosPorDia: 99, minutosPeriodo: 5, dias: [], recreos: [{ despuesDe: 50, minutos: 0 }] });
    expect(p.periodosPorDia).toBe(12);
    expect(p.minutosPeriodo).toBe(30);
    expect(p.dias).toEqual(['lunes']);
    expect(p.recreos).toEqual([{ despuesDe: 11, minutos: 5 }]);
  });

  it('ubica toda la carga sin cruces de docente ni aula y respetando el máximo diario', () => {
    const entrada = entradaBase();
    const resultado = generarHorario(entrada);
    expect(resultado.totalFaltantes).toBe(0);

    const { docentes, aulas, porCursoDia } = ocupacion(resultado.grilla, entrada);
    expect([...docentes.values()].every(n => n === 1)).toBe(true);
    expect([...aulas.values()].every(n => n === 1)).toBe(true);
    expect([...porCursoDia.values()].every(n => n <= entrada.parametros.maxMismoCursoPorDia)).toBe(true);
  });

  it('respeta lo ocupado por otras secciones y las clases fijadas', () => {
    const entrada = entradaBase();
    entrada.secciones = [entrada.secciones[0]];
    entrada.docentesOcupados = new Set(['10|lunes|0', '10|lunes|1']);
    entrada.fijadas = [{ seccionId: 1, dia: 'viernes', periodo: 6, asignacionId: 104, aulaId: 502 }];
    const resultado = generarHorario(entrada);
    const lunes = resultado.grilla.get(1)!.get('lunes')!;
    expect(lunes.get(0)?.asignacionId).not.toBe(101);
    expect(lunes.get(1)?.asignacionId).not.toBe(101);
    expect(resultado.grilla.get(1)!.get('viernes')!.get(6)).toEqual({ asignacionId: 104, aulaId: 502 });
    expect(resultado.totalFaltantes).toBe(0);
  });

  it('informa lo que no cabe en lugar de inventar espacio', () => {
    const entrada = entradaBase();
    entrada.parametros = { ...entrada.parametros, periodosPorDia: 2, recreos: [], dias: ['lunes'] };
    const resultado = generarHorario(entrada);
    expect(resultado.totalFaltantes).toBeGreaterThan(0);
    expect(resultado.faltantes.size).toBeGreaterThan(0);
  });
});
