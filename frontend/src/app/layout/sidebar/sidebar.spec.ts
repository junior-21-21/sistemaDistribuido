import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pad_token', 'eyJ.prueba');
    localStorage.setItem('pad_roles', JSON.stringify(['DOCENTE']));
    localStorage.setItem('pad_user', JSON.stringify({ name: 'Ana Flores', role: 'DOCENTE', roles: ['DOCENTE'], username: 'docente1' }));
    TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => localStorage.clear());

  it('muestra con nombre solo las pantallas del rol, agrupadas', async () => {
    const fixture = TestBed.createComponent(Sidebar);
    await fixture.whenStable();
    fixture.detectChanges();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Asistencia');
    expect(texto).toContain('Notas');
    expect(texto).toContain('Académico');
    expect(texto).not.toContain('Cuentas y personal');
    expect(texto).not.toContain('Matricular');
    expect(fixture.nativeElement.querySelector('.avatar')?.textContent?.trim()).toBe('AF');
  });
});
