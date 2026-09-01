import { Routes } from '@angular/router';
import { MainLayout } from './layout/main-layout/main-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { Estudiantes } from './pages/estudiantes/estudiantes';
import { Evaluaciones } from './pages/evaluaciones/evaluaciones';
import { Topico } from './pages/topico/topico';
import { Tickets } from './pages/tickets/tickets';
import { Derivaciones } from './pages/derivaciones/derivaciones';
import { Horarios } from './pages/horarios/horarios';
import { LoginComponent } from './pages/login/login';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Disciplina } from './pages/disciplina/disciplina';
import { Personal } from './pages/personal/personal';
import { Secciones } from './pages/secciones/secciones';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: Dashboard },
      {
        path: 'personal',
        component: Personal,
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR'] }
      },
      {
        path: 'estudiantes',
        component: Estudiantes,
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SECRETARIA'] }
      },
      {
        path: 'evaluaciones',
        component: Evaluaciones,
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE'] }
      },
      {
        path: 'topico',
        component: Topico,
        canActivate: [roleGuard],
        data: { roles: ['ENFERMERA'] }
      },
      {
        path: 'fichas-medicas',
        component: Topico,
        canActivate: [roleGuard],
        data: { roles: ['ENFERMERA'] }
      },
      {
        path: 'tickets',
        component: Tickets,
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'DIRECTOR'] }
      },
      {
        path: 'derivaciones',
        component: Derivaciones,
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'DIRECTOR'] }
      },
      {
        path: 'horarios',
        component: Horarios,
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SECRETARIA'] }
      },
      {
        path: 'disciplina',
        component: Disciplina,
        canActivate: [roleGuard],
        data: { roles: ['DOCENTE', 'DIRECTOR'] }
      },
      {
        path: 'secciones',
        component: Secciones,
        canActivate: [roleGuard],
        data: { roles: ['DIRECTOR', 'SECRETARIA'] }
      }
    ]
  },
  { path: '**', redirectTo: '' }
];



