import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { NotaService } from './nota.service';

describe('NotaService - contrato de bimestres', () => {
  let service: NotaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta el estado anual y abrevia el período en las rutas de gestión', () => {
    service.listarBimestres(2026).subscribe();
    const listado = http.expectOne(request => request.url === '/api/bimestres'
      && request.params.get('anio') === '2026');
    expect(listado.request.method).toBe('GET');
    listado.flush([]);

    service.cerrarBimestre(2026, 'I BIMESTRE', 'Cierre regular del período').subscribe();
    const cierre = http.expectOne('/api/bimestres/2026/I/cerrar');
    expect(cierre.request.body).toEqual({ motivo: 'Cierre regular del período' });
    cierre.flush({ anio: 2026, bimestre: 'I BIMESTRE', estado: 'CERRADO' });
  });

  it('usa contratos separados para reapertura y auditoría', () => {
    service.reabrirBimestre(2026, 'II BIMESTRE', 'Corrección autorizada por dirección').subscribe();
    expect(http.expectOne('/api/bimestres/2026/II/reabrir').request.method).toBe('POST');

    service.auditoriaBimestre(2026, 'II BIMESTRE').subscribe();
    expect(http.expectOne('/api/bimestres/2026/II/auditoria').request.method).toBe('GET');
  });
});
