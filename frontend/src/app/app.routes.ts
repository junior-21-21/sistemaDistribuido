import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then(module => module.LoginComponent)
  },
  {
    path: 'cambiar-clave',
    loadComponent: () => import('./pages/cambiar-clave/cambiar-clave').then(module => module.CambiarClave),
    canActivate: [authGuard]
  },
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout').then(module => module.MainLayout),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then(module => module.Dashboard)
      },
      {
        path: 'personal',
        loadComponent: () => import('./pages/personal/personal').then(module => module.Personal),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR'] }
      },
      {
        path: 'cursos',
        loadComponent: () => import('./pages/cursos/cursos').then(module => module.Cursos),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'estudiantes',
        loadComponent: () => import('./pages/estudiantes/estudiantes').then(module => module.Estudiantes),
        canActivate: [roleGuard],
        data: { roles: ['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'evaluaciones',
        loadComponent: () => import('./pages/evaluaciones/evaluaciones').then(module => module.Evaluaciones),
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'asistencia',
        loadComponent: () => import('./pages/asistencia/asistencia').then(module => module.AsistenciaPage),
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'topico',
        loadComponent: () => import('./pages/topico/topico').then(module => module.Topico),
        canActivate: [roleGuard],
        data: { roles: ['ENFERMERA', 'PRIMEROS_AUXILIOS'] }
      },
      {
        path: 'fichas-medicas',
        loadComponent: () => import('./pages/topico/topico').then(module => module.Topico),
        canActivate: [roleGuard],
        data: { roles: ['ENFERMERA', 'PRIMEROS_AUXILIOS'] }
      },
      {
        path: 'tickets',
        loadComponent: () => import('./pages/tickets/tickets').then(module => module.Tickets),
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'MANTENIMIENTO', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'derivaciones',
        loadComponent: () => import('./pages/derivaciones/derivaciones').then(module => module.Derivaciones),
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'PSICOLOGO', 'RESPONSABLE_CONVIVENCIA'] }
      },
      {
        path: 'horarios',
        loadComponent: () => import('./pages/horarios/horarios').then(module => module.Horarios),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'promocion',
        loadComponent: () => import('./pages/promocion/promocion').then(module => module.Promocion),
        canActivate: [roleGuard],
        data: { roles: ['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'ingreso',
        loadComponent: () => import('./pages/ingreso/ingreso').then(module => module.IngresoPage),
        canActivate: [roleGuard],
        data: { roles: ['AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'PORTERO', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'disciplina',
        loadComponent: () => import('./pages/disciplina/disciplina').then(module => module.Disciplina),
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'secciones',
        loadComponent: () => import('./pages/secciones/secciones').then(module => module.Secciones),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'gestor-aula',
        loadComponent: () => import('./pages/gestor-aula/gestor-aula').then(module => module.GestorAula),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'gestor-aula/:seccionId',
        loadComponent: () => import('./pages/gestor-aula/gestor-aula').then(module => module.GestorAula),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'portal-apoderado',
        loadComponent: () => import('./pages/portal-apoderado/portal-apoderado').then(module => module.PortalApoderado),
        canActivate: [roleGuard],
        data: { roles: ['APODERADO'] }
      },
      {
        path: 'auditoria',
        loadComponent: () => import('./pages/auditoria/auditoria').then(module => module.Auditoria),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR'] }
      },
      {
        path: 'matricula',
        loadComponent: () => import('./pages/matricula-secretaria/matricula-secretaria').then(module => module.MatriculaSecretaria),
        canActivate: [roleGuard],
        data: { roles: ['SECRETARIA'] }
      },
      {
        path: 'permisos',
        loadComponent: () => import('./pages/permisos/permisos').then(module => module.Permisos),
        canActivate: [roleGuard],
        data: { roles: ['AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DOCENTE', 'SECRETARIA', 'PORTERO', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'nominas',
        loadComponent: () => import('./pages/nominas/nominas').then(module => module.Nominas),
        canActivate: [roleGuard],
        data: { roles: ['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR'] }
      },
      {
        path: 'notificaciones',
        loadComponent: () => import('./pages/notificaciones/notificaciones').then(module => module.Notificaciones),
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SUBDIRECTOR', 'RESPONSABLE_CONVIVENCIA'] }
      }
    ]
  },
  { path: '**', redirectTo: '' }
];



