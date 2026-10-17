import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { EventoCambio, RealtimeService } from './realtime.service';

function streamDe(texto: string): Response {
  const cuerpo = new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(new TextEncoder().encode(texto));
      // Se deja abierto, como un SSE real.
    }
  });
  return new Response(cuerpo, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
}

describe('RealtimeService', () => {
  beforeEach(() => {
    localStorage.setItem('pad_token', 'eyJ.prueba');
    localStorage.setItem('pad_user', JSON.stringify({ name: 'Ana', role: 'DOCENTE', roles: ['DOCENTE'], username: 'docente1' }));
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('interpreta eventos SSE, marca los propios y cuenta los no leídos', async () => {
    const evento = (actorUsuario: string) => JSON.stringify({
      id: actorUsuario, modulo: 'notas', accion: 'CREAR', descripcion: 'registró notas',
      actor: 'X', actorUsuario, fecha: new Date().toISOString()
    });
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(streamDe(
      'event:conectado\ndata:{}\n\n' +
      ':latido\n\n' +
      `id:1\nevent:cambio\ndata:${evento('coord.academica')}\n\n` +
      `id:2\nevent:cambio\ndata:${evento('docente1')}\n\n`
    ));

    const service = TestBed.inject(RealtimeService);
    const recibidos: EventoCambio[] = [];
    service.cambios$.subscribe(e => recibidos.push(e));
    service.conectar();
    await vi.waitFor(() => expect(recibidos.length).toBe(2));

    expect(service.estado()).toBe('en-vivo');
    expect(recibidos[0].propio).toBe(false);
    expect(recibidos[1].propio).toBe(true);
    expect(service.noLeidos()).toBe(1);
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/tiempo-real/stream', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer eyJ.prueba' })
    }));
    service.desconectar();
    expect(service.estado()).toBe('desconectado');
  });
});
