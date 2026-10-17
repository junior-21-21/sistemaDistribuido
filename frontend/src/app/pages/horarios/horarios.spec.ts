import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { HorarioService } from '../../services/horario.service';
import { Horarios } from './horarios';

const secciones = [
  { id: 1, nombre: 'A', gradoId: 1, gradoNombre: 'PRIMERO', nivelNombre: 'SECUNDARIA', turno: 'MANANA', activa: true },
  { id: 2, nombre: 'B', gradoId: 1, gradoNombre: 'PRIMERO', nivelNombre: 'SECUNDARIA', turno: 'MANANA', activa: true },
];
const asignaciones = [
  { id: 11, cursoId: 1, docenteId: 7, seccionId: 1, horasSemanales: 3, anioEscolar: 2026, activa: true },
  { id: 12, cursoId: 2, docenteId: 8, seccionId: 1, horasSemanales: 2, anioEscolar: 2026, activa: true },
  { id: 21, cursoId: 1, docenteId: 7, seccionId: 2, horasSemanales: 3, anioEscolar: 2026, activa: true },
];

function crear(roles: string[]) {
  localStorage.clear();
  const servicio = {
    listarDocentes: () => of([
      { id: 7, nombre: 'Ana', apellido: 'Flores', email: '', especialidad: '', estado: 'ACTIVO' },
      { id: 8, nombre: 'Luis', apellido: 'Paz', email: '', especialidad: '', estado: 'ACTIVO' },
    ]),
    listarCursos: () => of([
      { id: 1, nombre: 'Matemática', gradoId: 1, estado: 'ACTIVO' },
      { id: 2, nombre: 'Arte', gradoId: 1, estado: 'ACTIVO' },
    ]),
    listarAulas: () => of([
      { id: 101, nombre: 'Aula 101', activa: true, turnoDisponible: 'AMBOS' },
      { id: 102, nombre: 'Aula 102', activa: true, turnoDisponible: 'MANANA' },
      { id: 103, nombre: 'Aula T', activa: true, turnoDisponible: 'TARDE' },
    ]),
    listarSecciones: () => of(secciones),
    listarAsignaciones: () => of(asignaciones),
    listarPorSeccion: () => of([]),
    reemplazarVarias: vi.fn((lote: { seccionId: number; bloques: unknown[] }[]) =>
      of(Object.fromEntries(lote.map(item => [item.seccionId, item.bloques])))),
  };
  TestBed.configureTestingModule({
    imports: [Horarios],
    providers: [
      provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
      { provide: HorarioService, useValue: servicio },
      { provide: AuthService, useValue: { hasRole: (r: string) => roles.includes(r), hasAnyRole: (...rs: string[]) => rs.some(r => roles.includes(r)), getRoles: () => roles, currentUser: () => null } },
    ],
  });
  const fixture = TestBed.createComponent(Horarios);
  fixture.detectChanges();
  return { component: fixture.componentInstance, servicio };
}

describe('Horarios', () => {
  afterEach(() => localStorage.clear());

  it('usa los parámetros elegidos para armar la jornada', () => {
    const { component } = crear(['DIRECTOR']);
    expect(component.bloques()[0].inicio).toBe('07:30');
    expect(component.finJornada()).toBe('13:15');

    component.actualizarParametro('horaInicio', '08:00');
    component.actualizarParametro('minutosPeriodo', 40);
    expect(component.bloques()[0]).toMatchObject({ inicio: '08:00', fin: '08:40' });
    expect(JSON.parse(localStorage.getItem('pad_horario_parametros_MANANA')!).minutosPeriodo).toBe(40);
  });

  it('solo ofrece aulas activas compatibles con el turno', () => {
    const { component } = crear(['DIRECTOR']);
    expect(component.aulasTurno().map(a => a.id)).toEqual([101, 102]);
  });

  it('genera sin cruzar al docente compartido y permite guardar todo el turno', async () => {
    const { component, servicio } = crear(['DIRECTOR']);
    component.generar();
    await new Promise(r => setTimeout(r, 60));

    const m1 = component.matrices().get(1)!;
    const m2 = component.matrices().get(2)!;
    for (const dia of component.parametros().dias) {
      m1[dia].forEach((clase, periodo) => {
        if (clase?.docenteId === 7) expect(m2[dia][periodo]?.docenteId).not.toBe(7);
      });
    }
    expect(component.resumenSecciones().every(r => r.ubicados === r.total)).toBe(true);

    component.guardarTurno();
    expect(servicio.reemplazarVarias).toHaveBeenCalledTimes(1);
    expect(servicio.reemplazarVarias.mock.calls[0][0].map((x: { seccionId: number }) => x.seccionId)).toEqual([1, 2]);
    expect(component.cambiosSinGuardar().size).toBe(0);
  });

  it('impide soltar una clase donde el docente ya dicta en otra sección', async () => {
    const { component } = crear(['DIRECTOR']);
    component.generar();
    await new Promise(r => setTimeout(r, 60));
    component.seccionActiva.set(1);

    // Busca una hora donde Ana dicta en 1.º B y prueba llevar Matemática de 1.º A allí.
    const m2 = component.matrices().get(2)!;
    const dia = component.parametros().dias.find(d => m2[d].some(c => c?.docenteId === 7))!;
    const periodo = m2[dia].findIndex(c => c?.docenteId === 7);
    const carga = component.pendientes().find(c => c.docenteId === 7)!;
    component.arrastre.set({ origen: 'pendiente', seccionId: 1, carga });
    expect(component.estadoDestino(dia, periodo)).toBe('ocupado');
    component.soltar(new Event('drop') as DragEvent, dia, periodo);
    expect(component.mensaje()?.tipo).toBe('error');
  });

  it('Dirección arma y edita los horarios', () => {
    const { component } = crear(['DIRECTOR']);
    expect(component.soloLectura).toBe(false);
  });

  it('cualquier otro rol solo consulta los horarios', () => {
    const { component } = crear(['DOCENTE']);
    expect(component.soloLectura).toBe(true);
  });
});
