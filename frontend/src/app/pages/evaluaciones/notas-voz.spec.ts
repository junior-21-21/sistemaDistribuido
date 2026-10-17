import { AlumnoVoz, aLiteral, interpretar } from './notas-voz';

const alumnos: AlumnoVoz[] = [
  { id: 1, nombre: 'Juan', apellido: 'Quispe Mamani' },
  { id: 2, nombre: 'María', apellido: 'Huamán Torres' },
  { id: 3, nombre: 'Luis', apellido: 'Quispe Rojas' },
  { id: 4, nombre: 'Rosa', apellido: 'Ccahuana Flores' },
];

describe('notas por voz', () => {
  it('reconoce nombre y escala literal', () => {
    expect(interpretar('Juan Quispe A', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 1 }, nota: 'A' });
    expect(interpretar('María Huamán, logro destacado', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 2 }, nota: 'AD' });
    expect(interpretar('Rosa Ccahuana en proceso', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 4 }, nota: 'B' });
    expect(interpretar('luis quispe ce', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 3 }, nota: 'C' });
  });

  it('convierte notas vigesimales dictadas en número o palabras', () => {
    expect(interpretar('Juan Quispe, 15', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 1 }, nota: 'A' });
    expect(interpretar('Juan Quispe dieciocho', alumnos)).toMatchObject({ nota: 'AD' });
    expect(interpretar('Rosa Ccahuana diez', alumnos)).toMatchObject({ nota: 'C' });
    expect(aLiteral(11)).toBe('B');
    expect(aLiteral(25)).toBeNull();
  });

  it('tolera errores del reconocimiento y nombres por orden de lista', () => {
    expect(interpretar('Rosa Cahuana A', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 4 } });
    expect(interpretar('número 2 B', alumnos)).toMatchObject({ tipo: 'ok', alumno: { id: 2 }, nota: 'B' });
  });

  it('pide aclarar si el apellido es compartido o falta la nota', () => {
    expect(interpretar('Quispe A', alumnos)).toMatchObject({ tipo: 'ambiguo' });
    expect(interpretar('Juan Quispe', alumnos)).toEqual({ tipo: 'sin-nota' });
    expect(interpretar('Pedro Pérez A', alumnos)).toMatchObject({ tipo: 'sin-alumno' });
  });
});
