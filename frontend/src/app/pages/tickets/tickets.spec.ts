import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { HorarioService } from '../../services/horario.service';
import { Ticket, Tickets } from './tickets';

describe('Tickets', () => {
  let component: Tickets;
  let fixture: ComponentFixture<Tickets>;
  let http: HttpTestingController;

  const auth = {
    hasRole: vi.fn((role: string) => role === 'MANTENIMIENTO')
  };
  const horarios = {
    listarAulas: vi.fn(() => of([{ id: 3, nombre: 'Laboratorio', activa: true }]))
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [Tickets],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: HorarioService, useValue: horarios }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Tickets);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('permite a Mantenimiento consultar y resolver tickets con notas', () => {
    fixture.detectChanges();
    const ticket: Ticket = {
      id: 7, docenteId: 2, aulaId: 3, categoria: 'ELECTRICO',
      descripcion: 'Proyector sin energía', urgencia: 'ALTA',
      estado: 'PENDIENTE', fechaReporte: '2026-09-23T10:00:00'
    };
    http.expectOne('/api/tickets').flush([ticket]);

    expect(component.puedeGestionar).toBe(true);
    expect(component.puedeCrear).toBe(false);
    expect(component.getAulaNombre(3)).toBe('Laboratorio');

    component.ediciones[7] = { estado: 'RESUELTO', notasResolucion: 'Se cambió el tomacorriente.' };
    component.actualizarTicket(ticket);

    const request = http.expectOne('/api/tickets/7/estado');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      estado: 'RESUELTO',
      notasResolucion: 'Se cambió el tomacorriente.'
    });
    request.flush({ ...ticket, estado: 'RESUELTO', notasResolucion: 'Se cambió el tomacorriente.' });

    expect(component.tickets[0].estado).toBe('RESUELTO');
    expect(component.actualizandoId).toBeNull();
  });

  it('exige notas antes de resolver', () => {
    component.ediciones[7] = { estado: 'RESUELTO', notasResolucion: '' };
    component.actualizarTicket({
      id: 7, docenteId: 2, aulaId: 3, categoria: 'ELECTRICO',
      descripcion: 'Falla', urgencia: 'ALTA', estado: 'PENDIENTE',
      fechaReporte: '2026-09-23T10:00:00'
    });

    expect(component.error).toContain('notas de resolución');
    http.expectNone('/api/tickets/7/estado');
  });
});
