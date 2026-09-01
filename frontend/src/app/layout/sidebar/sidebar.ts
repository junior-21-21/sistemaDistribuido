import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { filter } from 'rxjs';

interface MenuItem {
  path: string;
  label: string;
  icon: string;
  roles: string[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);
  
  allMenuItems: MenuItem[] = [
    { path: '/dashboard', icon: 'bi-house-door', label: 'Inicio', roles: ['DIRECTOR', 'SECRETARIA', 'DOCENTE', 'ENFERMERA'] },
    { path: '/personal', icon: 'bi-shield-lock', label: 'Personal', roles: ['DIRECTOR'] },
    { path: '/secciones', icon: 'bi-grid', label: 'Secciones', roles: ['DIRECTOR', 'SECRETARIA'] },
    { path: '/estudiantes', icon: 'bi-people', label: 'Matrículas', roles: ['DIRECTOR', 'SECRETARIA'] },
    { path: '/horarios', icon: 'bi-calendar3', label: 'Horarios', roles: ['DIRECTOR', 'SECRETARIA'] },
    { path: '/mis-clases', icon: 'bi-journal-bookmark', label: 'Clases', roles: ['DOCENTE'] },
    { path: '/evaluaciones', icon: 'bi-clipboard-check', label: 'Notas', roles: ['DOCENTE'] },
    { path: '/disciplina', icon: 'bi-exclamation-triangle', label: 'Disciplina', roles: ['DOCENTE', 'DIRECTOR'] },
    { path: '/tickets', icon: 'bi-tools', label: 'Tickets', roles: ['DOCENTE', 'DIRECTOR'] },
    { path: '/fichas-medicas', icon: 'bi-hospital', label: 'Fichas', roles: ['ENFERMERA'] },
    { path: '/topico', icon: 'bi-bandaid', label: 'Tópico', roles: ['ENFERMERA'] },
  ];

  menuItems: MenuItem[] = [];
  activeIndex = 0;

  ngOnInit() {
    const role = this.authService.getRole() || '';
    
    // Limitamos a un máximo de ~6 para diseño liquid nav, o los ajustamos dinámicamente
    this.menuItems = this.allMenuItems.filter(item => item.roles.includes(role)).slice(0, 7);
    
    this.updateActiveIndex(this.router.url);

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateActiveIndex(event.urlAfterRedirects);
    });
  }

  updateActiveIndex(url: string) {
    const idx = this.menuItems.findIndex(item => url.includes(item.path));
    if (idx !== -1) {
      this.activeIndex = idx;
    } else {
      this.activeIndex = 0;
    }
  }
}
