import { describe, expect, it } from 'vitest';

import { crearPdf } from '../../shared/pdf/pdf-tabla';
import { construirMatriz, describirPeriodo, rangoDelPeriodo, reportePdf, reporteXlsx } from './reporte-asistencia';

const alumnos = [
  { id: 1, nombre: 'Ana', apellido: 'Quispe' },
  { id: 2, nombre: 'Luis', apellido: 'Ñahui' }
];
const registro = (estudianteId: number, fecha: string, horarioId: number, estado: string) =>
  ({ estudianteId, fecha, horarioId, estado, docenteId: 1, seccionId: 1, cursoId: 1 });

describe('reporte de asistencia', () => {
  it('calcula el rango de día, semana (lunes a viernes) y mes', () => {
    expect(rangoDelPeriodo('DIA', '2026-10-14')).toEqual({ desde: '2026-10-14', hasta: '2026-10-14' });
    expect(rangoDelPeriodo('SEMANA', '2026-10-14')).toEqual({ desde: '2026-10-12', hasta: '2026-10-16' });
    expect(rangoDelPeriodo('SEMANA', '2026-10-18')).toEqual({ desde: '2026-10-12', hasta: '2026-10-16' });
    expect(rangoDelPeriodo('MES', '2026-02-10')).toEqual({ desde: '2026-02-01', hasta: '2026-02-28' });
    expect(describirPeriodo('SEMANA', '2026-10-12', '2026-10-16')).toBe('semana del 12 al 16 de octubre');
    expect(describirPeriodo('DIA', '2026-10-14', '2026-10-14')).toBe('miércoles 14 de octubre de 2026');
  });

  it('arma la matriz por clase con totales y porcentaje (P + T) / clases', () => {
    const m = construirMatriz([
      registro(1, '2026-10-13', 7, 'PRESENTE'), registro(2, '2026-10-13', 7, 'FALTA'),
      registro(1, '2026-10-12', 5, 'TARDE'), registro(2, '2026-10-12', 5, 'JUSTIFICADO')
    ], alumnos, id => (id === 5 ? '08:00' : '09:30'));
    expect(m.sesiones.map(s => s.fecha)).toEqual(['2026-10-12', '2026-10-13']);
    expect(m.filas[0].alumno.apellido).toBe('Ñahui');
    expect(m.filas[0].marcas).toEqual(['J', 'F']);
    expect(m.filas[0].porcentaje).toBe(0);
    expect(m.filas[1].porcentaje).toBe(100);
    expect(m.totales).toMatchObject({ P: 1, T: 1, F: 1, J: 1, porcentaje: 50 });
  });

  it('genera un PDF válido y una hoja de Excel con encabezados', () => {
    const m = construirMatriz([registro(1, '2026-10-13', 7, 'PRESENTE')], alumnos, () => '08:00');
    const ctx = { periodo: 'SEMANA' as const, desde: '2026-10-12', hasta: '2026-10-16', curso: 'Matemática',
      seccion: 'PRIMERO A', generadoPor: 'docente1' };
    const pdf = new TextDecoder('latin1').decode(crearPdf(reportePdf(m, ctx)));
    expect(pdf.startsWith('%PDF-1.4')).toBe(true);
    expect(pdf.trimEnd().endsWith('%%EOF')).toBe(true);
    // La tabla de referencias apunta al inicio real de cada objeto
    const xref = Number(pdf.match(/startxref\n(\d+)/)![1]);
    const primera = Number(pdf.slice(xref).split('\n')[3].slice(0, 10));
    expect(pdf.slice(primera, primera + 7)).toBe('1 0 obj');
    // Las tildes viajan como octales WinAnsi (á = \341)
    expect(pdf).toContain('(Matem\\341tica)');
    const hoja = reporteXlsx(m, ctx);
    expect(hoja.filas[3].map(c => c.v)).toEqual(['N°', 'Estudiante', '13/10 08:00', 'P', 'T', 'F', 'J', '% Asist.']);
  });
});
