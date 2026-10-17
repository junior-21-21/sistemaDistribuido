import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { MainLayout } from './main-layout';
import { RealtimeService } from '../../core/realtime/realtime.service';

describe('MainLayout', () => {
  it('se crea y abre el canal en tiempo real', async () => {
    const conectar = vi.fn();
    const desconectar = vi.fn();
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RealtimeService, useValue: {
          conectar, desconectar,
          cambios$: { pipe: () => ({ subscribe: () => ({ unsubscribe() {} }) }) },
          estado: () => 'en-vivo', enVivo: () => true, eventos: () => [], noLeidos: () => 0,
          marcarLeidos: () => {},
          alCambiar: () => ({ subscribe: () => ({ unsubscribe() {} }) })
        } }
      ],
    });
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
    expect(conectar).toHaveBeenCalled();
    fixture.destroy();
    expect(desconectar).toHaveBeenCalled();
  });
});
