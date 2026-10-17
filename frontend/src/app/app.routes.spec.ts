import { routes } from './app.routes';

describe('rutas por rol', () => {
  it('tickets: el docente reporta; Mantenimiento o, si no hay, Dirección los atiende', () => {
    const root = routes.find(route => route.path === '');
    const tickets = root?.children?.find(route => route.path === 'tickets');

    expect(tickets?.data?.['roles']).toEqual(['DOCENTE', 'MANTENIMIENTO', 'DIRECTOR', 'SUBDIRECTOR']);
  });
});

describe('organización del colegio (MINEDU)', () => {
  const hijos = routes.find(route => route.path === '')?.children ?? [];
  const roles = (path: string) => hijos.find(route => route.path === path)?.data?.['roles'] as string[];

  it('portería ve ingresos y salidas, pero no estudiantes', () => {
    expect(roles('ingreso')).toContain('PORTERO');
    expect(roles('permisos')).toContain('PORTERO');
    expect(roles('estudiantes')).not.toContain('PORTERO');
  });

  it('las nóminas son de Secretaría y Dirección', () => {
    expect(roles('nominas')).toEqual(['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR']);
  });

  it('Subdirección no administra cuentas ni auditoría', () => {
    expect(roles('personal')).not.toContain('SUBDIRECTOR');
    expect(roles('auditoria')).not.toContain('SUBDIRECTOR');
  });
});
