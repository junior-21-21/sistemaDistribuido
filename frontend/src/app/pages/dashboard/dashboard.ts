import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';

interface StatCard {
  label: string;
  value: string;
  icon: string;
  color: string;
  trend?: string;
}

interface QuickAction {
  label: string;
  icon: string;
  path: string;
  color: string;
}

interface Activity {
  texto: string;
  fecha: string;
  tipo: 'info' | 'success' | 'warning' | 'error';
  origen?: string;
  time?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  authService = inject(AuthService);
  dashboardService = inject(DashboardService);
  cdr = inject(ChangeDetectorRef);
  
  role = '';
  userName = '';
  greeting = '';
  roleLabel = '';
  roleColor = '';

  stats: StatCard[] = [];
  quickActions: QuickAction[] = [];
  recentActivity: Activity[] = [];
  currentDate = new Date().toLocaleDateString('es-PE', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });


  ngOnInit() {
    this.role = this.authService.getRole() || 'DIRECTOR';
    const user = this.authService.currentUser();
    this.userName = user?.name || 'Usuario';

    const hour = new Date().getHours();
    if (hour < 12) this.greeting = 'Buenos días';
    else if (hour < 19) this.greeting = 'Buenas tardes';
    else this.greeting = 'Buenas noches';

    this.configureByRole();
    this.loadRealStats();
    this.loadRealActivities();
  }

  loadRealActivities() {
    this.dashboardService.getActividadesRecientes().subscribe({
      next: (actividades) => {
        if (actividades && actividades.length > 0) {
          this.recentActivity = actividades.map(a => {
            const fecha = new Date(a.fecha);
            const hoy = new Date();
            const diffMinutos = Math.floor((hoy.getTime() - fecha.getTime()) / 60000);
            
            let timeStr = '';
            if (diffMinutos < 60) timeStr = `Hace ${diffMinutos} min`;
            else if (diffMinutos < 1440) timeStr = `Hace ${Math.floor(diffMinutos / 60)} horas`;
            else timeStr = `Hace ${Math.floor(diffMinutos / 1440)} días`;

            return {
              texto: a.texto,
              tipo: a.tipo as any,
              origen: a.origen,
              time: timeStr,
              fecha: a.fecha
            };
          });
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error fetching real activities:', err);
      }
    });
  }

  loadRealStats() {
    if (this.role === 'DIRECTOR') {
      this.dashboardService.getStats().subscribe({
        next: (data) => {
          this.stats = [
            { label: 'Total Estudiantes', value: data.totalEstudiantes.toString(), icon: 'bi-people-fill', color: '#ef4444', trend: 'Actualizado' },
            { label: 'Docentes Activos', value: data.docentesActivos.toString(), icon: 'bi-person-workspace', color: '#b91c1c', trend: 'Sincronizado' },
            { label: 'Matrículas Pendientes', value: data.matriculasPendientes.toString(), icon: 'bi-clipboard2-pulse', color: '#991b1b', trend: 'Requieren aprobación' },
            { label: 'Alertas de Deserción', value: data.alertasDesercion.toString(), icon: 'bi-exclamation-triangle-fill', color: '#dc2626', trend: 'Intervención urgente' }
          ];
          console.log('Real stats loaded:', this.stats);
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error fetching real stats:', err);
        }
      });
    }
  }

  configureByRole() {
    const configs: Record<string, any> = {
      DIRECTOR: {
        roleLabel: 'Director',
        roleColor: '#dc2626',
        stats: [
          { label: 'Total Estudiantes', value: 'Cargando...', icon: 'bi-people-fill', color: '#ef4444', trend: '...' },
          { label: 'Docentes Activos', value: 'Cargando...', icon: 'bi-person-workspace', color: '#b91c1c', trend: '...' },
          { label: 'Matrículas Pendientes', value: 'Cargando...', icon: 'bi-clipboard2-pulse', color: '#991b1b', trend: '...' },
          { label: 'Alertas de Deserción', value: 'Cargando...', icon: 'bi-exclamation-triangle-fill', color: '#dc2626', trend: '...' }
        ],
        quickActions: [
          { label: 'Aprobar Matrículas', icon: 'bi-check-circle-fill', path: '/estudiantes', color: '#ef4444' },
          { label: 'Ver Horarios', icon: 'bi-calendar3', path: '/cursos', color: '#b91c1c' },
          { label: 'Reportes MINEDU', icon: 'bi-bar-chart-fill', path: '/reportes', color: '#991b1b' },
          { label: 'Comunicados', icon: 'bi-megaphone-fill', path: '/comunicados', color: '#dc2626' }
        ],
        recentActivity: []
      },
      DOCENTE: {
        roleLabel: 'Docente',
        roleColor: '#dc2626',
        stats: [
          { label: 'Mis Estudiantes', value: '148', icon: 'bi-person-video3', color: '#ef4444', trend: '4 secciones' },
          { label: 'Notas Pendientes', value: '12', icon: 'bi-pencil-square', color: '#b91c1c', trend: 'Por registrar' },
          { label: 'Asistencia Hoy', value: '96%', icon: 'bi-calendar2-check-fill', color: '#991b1b', trend: 'Alta asistencia' },
          { label: 'Papeletas Emitidas', value: '3', icon: 'bi-file-earmark-break-fill', color: '#dc2626', trend: 'Este mes' }
        ],
        quickActions: [
          { label: 'Registrar Notas', icon: 'bi-pencil-square', path: '/evaluaciones', color: '#ef4444' },
          { label: 'Tomar Asistencia', icon: 'bi-person-check-fill', path: '/mis-clases', color: '#b91c1c' },
          { label: 'Papeleta Digital', icon: 'bi-exclamation-octagon-fill', path: '/disciplina', color: '#991b1b' },
          { label: 'Ticket Mantenimiento', icon: 'bi-tools', path: '/mantenimiento', color: '#dc2626' }
        ],
        recentActivity: []
      },
      ENFERMERA: {
        roleLabel: 'Personal de Tópico',
        roleColor: '#dc2626',
        stats: [
          { label: 'Atenciones Hoy', value: '8', icon: 'bi-hospital', color: '#ef4444', trend: '+3 vs ayer' },
          { label: 'Fichas Médicas', value: '1,240', icon: 'bi-clipboard2-pulse', color: '#b91c1c', trend: 'Total estudiantes' },
          { label: 'Vacunas Registradas', value: '342', icon: 'bi-bandaid-fill', color: '#991b1b', trend: 'Campaña activa' },
          { label: 'Emergencias', value: '0', icon: 'bi-heart-pulse-fill', color: '#dc2626', trend: 'Hoy sin incidentes' }
        ],
        quickActions: [
          { label: 'Nueva Atención', icon: 'bi-hospital', path: '/topico', color: '#ef4444' },
          { label: 'Fichas Médicas', icon: 'bi-clipboard2-pulse', path: '/fichas-medicas', color: '#b91c1c' },
          { label: 'Registro de Vacunas', icon: 'bi-bandaid-fill', path: '/topico', color: '#991b1b' },
          { label: 'Reportar Emergencia', icon: 'bi-heart-pulse-fill', path: '/topico', color: '#dc2626' }
        ],
        recentActivity: []
      },
      SECRETARIA: {
        roleLabel: 'Secretaría Académica',
        roleColor: '#dc2626',
        stats: [
          { label: 'Matrículas Hoy', value: '6', icon: 'bi-file-person', color: '#ef4444', trend: 'Procesadas' },
          { label: 'Pendientes Aprobación', value: '23', icon: 'bi-hourglass-split', color: '#b91c1c', trend: 'En revisión' },
          { label: 'Documentos Escaneados', value: '89', icon: 'bi-file-earmark-pdf', color: '#991b1b', trend: 'Este mes' },
          { label: 'Certificados Emitidos', value: '14', icon: 'bi-award-fill', color: '#dc2626', trend: 'Este año' }
        ],
        quickActions: [
          { label: 'Nueva Matrícula', icon: 'bi-file-person', path: '/estudiantes', color: '#ef4444' },
          { label: 'Buscar Estudiante', icon: 'bi-search', path: '/estudiantes', color: '#b91c1c' },
          { label: 'Emitir Certificado', icon: 'bi-award-fill', path: '/certificados', color: '#991b1b' },
          { label: 'Escanear Documento', icon: 'bi-file-earmark-pdf', path: '/documentos', color: '#dc2626' }
        ],
        recentActivity: []
      }
    };

    const config = configs[this.role] || configs['DIRECTOR'];
    this.roleLabel = config.roleLabel;
    this.roleColor = config.roleColor;
    this.stats = config.stats;
    this.quickActions = config.quickActions;
    this.recentActivity = config.recentActivity;
  }
}

