import { Component, DestroyRef, NgZone, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { SeccionService } from '../../core/services/seccion.service';
import { LayoutService } from '../../core/services/layout.service';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { Seccion } from '../../core/models/seccion.model';
import { NavItem, ROL_ETIQUETA, itemsParaRoles } from '../../core/navigation';
import { HaceTiempoPipe } from '../../shared/hace-tiempo.pipe';
import { ModulosColegioService } from '../../core/services/modulos-colegio.service';

interface Indicador {
  etiqueta: string;
  valor: number | null;
  icono: string;
  tono: 'azul' | 'verde' | 'ambar' | 'rojo';
  ayuda: string;
  ruta?: string;
}

interface Clase {
  id: number;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  cursoNombre?: string;
  seccionId: number;
  aulaId: number;
  seccionTexto?: string;
  aulaTexto?: string;
}

interface Ticket {
  id: number;
  categoria: string;
  descripcion: string;
  urgencia: string;
  estado: string;
  fechaReporte: string;
}

interface ActividadVista {
  id: string;
  texto: string;
  autor?: string;
  fecha: string;
  icono: string;
  enVivo: boolean;
}

const DIAS = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];
const DIA_LEGIBLE: Record<string, string> = {
  LUNES: 'lunes', MARTES: 'martes', MIERCOLES: 'miércoles', JUEVES: 'jueves', VIERNES: 'viernes', SABADO: 'sábado', DOMINGO: 'domingo'
};

/** Acción principal sugerida por rol: lo primero que la persona suele hacer al entrar. */
const ACCION_PRINCIPAL: Record<string, string> = {
  DOCENTE: '/asistencia',
  AUXILIAR: '/ingreso',
  SECRETARIA: '/matricula',
  ENFERMERA: '/topico',
  PSICOLOGO: '/derivaciones',
  MANTENIMIENTO: '/tickets',
  APODERADO: '/portal-apoderado',
  DIRECTOR: '/nominas',
  SUBDIRECTOR: '/horarios',
  PORTERO: '/ingreso',
};

interface PasoDia {
  id: string;
  titulo: string;
  detalle: string;
  ruta: string;
}

/**
 * Flujo recomendado del día (se marca como una lista de cuaderno). Una persona ve los pasos
 * de su puesto y, debajo, los de sus encargos (por ejemplo, auxiliar + responsable de convivencia).
 */
const PASOS_POR_ROL: Record<string, PasoDia[]> = {
  DIRECTOR: [
    { id: 'indicadores', titulo: 'Mirar indicadores', detalle: 'Matrícula, docentes y riesgo de deserción', ruta: '/dashboard' },
    { id: 'carga', titulo: 'Carga docente y horarios', detalle: 'Repartir cursos, generar y ajustar', ruta: '/horarios' },
    { id: 'notas', titulo: 'Notas y bimestres', detalle: 'Cerrar o reabrir con motivo', ruta: '/evaluaciones' },
    { id: 'nominas', titulo: 'Aprobar nóminas de matrícula', detalle: 'Firma de Dirección por sección', ruta: '/nominas' },
    { id: 'permisos', titulo: 'Permisos formales', detalle: 'Solicitudes de Secretaría por aprobar', ruta: '/permisos' },
    { id: 'personal', titulo: 'Personal y encargos', detalle: 'Cuentas, tutores, TOE y módulos', ruta: '/personal' },
    { id: 'auditoria', titulo: 'Auditoría', detalle: 'Quién hizo qué y cuándo', ruta: '/auditoria' },
  ],
  SUBDIRECTOR: [
    { id: 'carga', titulo: 'Carga docente y horarios', detalle: 'De tu nivel', ruta: '/horarios' },
    { id: 'notas', titulo: 'Consolidado de notas', detalle: 'Revisar y cerrar bimestres', ruta: '/evaluaciones' },
    { id: 'asistencia', titulo: 'Monitoreo de asistencia', detalle: 'Aulas con más faltas', ruta: '/asistencia' },
    { id: 'tutores', titulo: 'Tutores de aula', detalle: 'Un docente tutor por sección', ruta: '/secciones' },
  ],
  PORTERO: [
    { id: 'porteria', titulo: 'Control de ingreso', detalle: 'Escanea el DNI en la puerta', ruta: '/ingreso' },
    { id: 'salidas', titulo: 'Papeletas de salida de hoy', detalle: 'Verifica quién recoge al estudiante', ruta: '/permisos' },
  ],
  DOCENTE: [
    { id: 'asistencia', titulo: 'Tomar asistencia', detalle: 'Al iniciar cada clase, en segundos', ruta: '/asistencia' },
    { id: 'notas', titulo: 'Registrar notas', detalle: 'Con porcentajes; también por voz', ruta: '/evaluaciones' },
    { id: 'convivencia', titulo: 'Anotar incidencias o méritos', detalle: 'Las ve el responsable de convivencia', ruta: '/disciplina' },
    { id: 'aula', titulo: 'Reportar fallas del aula', detalle: 'Se genera un ticket', ruta: '/tickets' },
  ],
  AUXILIAR: [
    { id: 'porteria', titulo: 'Control de ingreso', detalle: 'Escanea el DNI en la puerta', ruta: '/ingreso' },
    { id: 'asistencia', titulo: 'Revisar asistencia por aula', detalle: 'Quién faltó o llegó tarde', ruta: '/asistencia' },
    { id: 'convivencia', titulo: 'Registrar incidencias', detalle: 'Recreo, formación y salida', ruta: '/disciplina' },
    { id: 'permisos', titulo: 'Justificaciones y papeletas', detalle: 'Atiende al apoderado y aprueba', ruta: '/permisos' },
  ],
  SECRETARIA: [
    { id: 'matricula', titulo: 'Atender matrículas', detalle: 'Gratuita: solo pide el DNI', ruta: '/matricula' },
    { id: 'legajos', titulo: 'Completar legajos', detalle: 'Documentos y apoderados', ruta: '/estudiantes' },
    { id: 'promocion', titulo: 'Traslados y promoción', detalle: 'Pasar de grado y recuperación', ruta: '/promocion' },
    { id: 'nominas', titulo: 'Nóminas de matrícula', detalle: 'Imprimir y pasar a firma de Dirección', ruta: '/nominas' },
    { id: 'permisos', titulo: 'Mesa de partes: permisos', detalle: 'Registrar solicitudes formales', ruta: '/permisos' },
  ],
  RESPONSABLE_CONVIVENCIA: [
    { id: 'casos', titulo: 'Casos de violencia y SíseVe', detalle: 'Protocolo, citación y derivación', ruta: '/disciplina' },
    { id: 'derivaciones', titulo: 'Derivaciones', detalle: 'Casos que necesitan apoyo', ruta: '/derivaciones' },
    { id: 'avisos', titulo: 'Avisos a familias', detalle: 'Revisar notificaciones enviadas', ruta: '/notificaciones' },
  ],
  PRIMEROS_AUXILIOS: [
    { id: 'topico', titulo: 'Tópico', detalle: 'Atenciones y fichas de salud', ruta: '/topico' },
  ],
  ENFERMERA: [
    { id: 'topico', titulo: 'Atenciones de tópico', detalle: 'Registrar cada atención', ruta: '/topico' },
  ],
  PSICOLOGO: [
    { id: 'derivaciones', titulo: 'Revisar derivaciones nuevas', detalle: 'Prioriza las urgentes', ruta: '/derivaciones' },
  ],
  MANTENIMIENTO: [
    { id: 'tickets', titulo: 'Atender tickets', detalle: 'Primero los de urgencia alta', ruta: '/tickets' },
  ],
  APODERADO: [
    { id: 'portal', titulo: 'Ver notas y asistencia de mis hijos', detalle: 'Se actualiza en tiempo real', ruta: '/portal-apoderado' },
    { id: 'justificar', titulo: 'Justificar una falta o pedir permiso', detalle: 'Desde el portal, sin ir al colegio', ruta: '/portal-apoderado' },
  ],
};

const NOTAS = ['', 'celeste', 'rosada', 'verde'];
const GIROS = ['-1.5deg', '1deg', '-0.5deg', '1.5deg'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, FormsModule, HaceTiempoPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly seccionService = inject(SeccionService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly zona = inject(NgZone);
  readonly layout = inject(LayoutService);
  readonly realtime = inject(RealtimeService);

  readonly roles = this.authService.getRoles();
  readonly role = this.authService.getRole() || this.roles[0] || 'DIRECTOR';
  readonly nombre = (this.authService.currentUser()?.name ?? 'Usuario').split(' ')[0];
  readonly etiquetasRol = this.roles.map(rol => ROL_ETIQUETA[rol] ?? rol);
  readonly saludo = this.calcularSaludo();
  readonly fechaHoy = new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });

  readonly accesos: NavItem[] = itemsParaRoles(this.roles, inject(ModulosColegioService).estado())
    .filter(item => item.path !== '/dashboard');
  readonly principal: NavItem | undefined = this.accesos.find(item => item.path === ACCION_PRINCIPAL[this.role]) ?? this.accesos[0];
  readonly secundarios = this.accesos.filter(item => item !== this.principal).slice(0, 6);

  readonly indicadores = signal<Indicador[]>([]);

  /** Pasos del día según el rol principal; lo marcado se recuerda solo durante el día. */
  /** Pasos del puesto y de los encargos, sin repetir ni incluir pantallas a las que no tiene acceso. */
  readonly pasosDelDia: PasoDia[] = (() => {
    const rutas = new Set(['/dashboard', ...this.accesos.map(item => item.path)]);
    const vistos = new Set<string>();
    return [this.role, ...this.roles.filter(rol => rol !== this.role)]
      .flatMap(rol => PASOS_POR_ROL[rol] ?? [])
      .filter(paso => rutas.has(paso.ruta) && !vistos.has(paso.ruta + paso.id) && !!vistos.add(paso.ruta + paso.id));
  })();
  private readonly claveDia = `pad_lista_${this.role}_${new Date().toISOString().slice(0, 10)}`;
  readonly hechos = signal<Set<string>>(this.leerHechos());
  readonly hechosHoy = computed(() => this.pasosDelDia.filter(p => this.hechos().has(p.id)).length);
  readonly clases = signal<Clase[]>([]);
  readonly diaClases = signal('');
  readonly tickets = signal<Ticket[]>([]);
  readonly secciones = signal<Seccion[]>([]);
  readonly actividadesServidor = signal<ActividadVista[]>([]);
  readonly cargandoClases = signal(true);
  readonly ahora = signal(new Date());
  seccionSeleccionadaId: number | null = null;
  private reloj: ReturnType<typeof setInterval> | null = null;

  readonly esDocente = this.tiene('DOCENTE');
  readonly esMantenimiento = this.tiene('MANTENIMIENTO');
  readonly gestionaAulas = this.tiene('DIRECTOR', 'SUBDIRECTOR');
  private readonly veActividades = this.tiene('DIRECTOR', 'SUBDIRECTOR', 'RESPONSABLE_CONVIVENCIA');

  /** Actividad: primero lo ocurrido en vivo durante esta sesión, luego el historial del servidor. */
  readonly actividad = computed<ActividadVista[]>(() => {
    const vivos = this.realtime.eventos().map(evento => ({
      id: evento.id,
      texto: evento.descripcion,
      autor: evento.propio ? 'Tú' : evento.actor,
      fecha: evento.fecha,
      icono: this.accesos.find(item => item.modulos?.includes(evento.modulo))?.icon ?? 'bi-lightning-charge',
      enVivo: true
    }));
    return [...vivos, ...this.actividadesServidor()].slice(0, 8);
  });

  readonly claseActual = computed(() => {
    if (this.diaClases() !== 'hoy') return null;
    const minutos = this.ahora().getHours() * 60 + this.ahora().getMinutes();
    return this.clases().find(c => this.aMinutos(c.horaInicio) <= minutos && minutos < this.aMinutos(c.horaFin)) ?? null;
  });

  readonly siguienteClase = computed(() => {
    if (this.diaClases() !== 'hoy') return this.clases()[0] ?? null;
    const minutos = this.ahora().getHours() * 60 + this.ahora().getMinutes();
    return this.clases().find(c => this.aMinutos(c.horaInicio) > minutos) ?? null;
  });

  ngOnInit(): void {
    // Reloj fuera de Angular: la señal ya avisa a la vista cuando cambia.
    this.zona.runOutsideAngular(() => {
      this.reloj = setInterval(() => this.ahora.set(new Date()), 30000);
    });
    this.cargarIndicadores();
    if (this.esDocente) this.cargarClases();
    if (this.esMantenimiento) this.cargarTickets();
    if (this.gestionaAulas) this.cargarSecciones();
    if (this.veActividades) this.cargarActividades();

    // El panel se refresca solo cuando alguien registra algo relevante.
    this.realtime.alCambiar('estudiantes', 'docentes', 'asistencias', 'tickets', 'horarios', 'asignaciones-academicas', 'secciones')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(evento => {
        if (['estudiantes', 'docentes', 'asistencias', 'tickets'].includes(evento.modulo)) this.cargarIndicadores();
        if (evento.modulo === 'tickets' && this.esMantenimiento) this.cargarTickets();
        if (['horarios', 'asignaciones-academicas'].includes(evento.modulo) && this.esDocente) this.cargarClases();
        if (evento.modulo === 'secciones' && this.gestionaAulas) this.cargarSecciones();
      });
  }

  ngOnDestroy(): void {
    if (this.reloj) clearInterval(this.reloj);
  }

  pasoHecho(id: string): boolean {
    return this.hechos().has(id);
  }

  alternarPaso(id: string): void {
    this.hechos.update(set => {
      const copia = new Set(set);
      copia.has(id) ? copia.delete(id) : copia.add(id);
      try {
        localStorage.setItem(this.claveDia, JSON.stringify([...copia]));
      } catch {
        /* sin almacenamiento */
      }
      return copia;
    });
  }

  colorNota(i: number): string {
    return NOTAS[i % NOTAS.length];
  }

  giroNota(i: number): string {
    return GIROS[i % GIROS.length];
  }

  private leerHechos(): Set<string> {
    try {
      return new Set(JSON.parse(localStorage.getItem(`pad_lista_${this.authService.getRole() || 'DIRECTOR'}_${new Date().toISOString().slice(0, 10)}`) ?? '[]'));
    } catch {
      return new Set();
    }
  }

  abrirBuscador(): void {
    this.layout.buscadorAbierto.set(true);
  }

  irAlAula(): void {
    if (this.seccionSeleccionadaId) this.router.navigate(['/gestor-aula', this.seccionSeleccionadaId]);
  }

  hora(valor: string): string {
    return valor?.slice(0, 5) ?? '';
  }

  progresoClase(clase: Clase): number {
    const minutos = this.ahora().getHours() * 60 + this.ahora().getMinutes();
    const inicio = this.aMinutos(clase.horaInicio);
    const fin = this.aMinutos(clase.horaFin);
    return Math.min(100, Math.max(0, Math.round(((minutos - inicio) / Math.max(1, fin - inicio)) * 100)));
  }

  private cargarIndicadores(): void {
    if (this.tiene('DIRECTOR', 'SUBDIRECTOR')) {
      this.dashboardService.getStats().subscribe({
        next: data => this.indicadores.set([
          { etiqueta: 'Estudiantes', valor: data.totalEstudiantes, icono: 'bi-people-fill', tono: 'azul', ayuda: 'Matriculados activos', ruta: '/estudiantes' },
          { etiqueta: 'Docentes activos', valor: data.docentesActivos, icono: 'bi-person-workspace', tono: 'verde', ayuda: 'Con carga este año', ruta: '/horarios' },
          { etiqueta: 'Matrículas pendientes', valor: data.matriculasPendientes, icono: 'bi-hourglass-split', tono: 'ambar', ayuda: 'Por completar', ruta: '/estudiantes' },
          { etiqueta: 'Riesgo de deserción', valor: data.alertasDesercion, icono: 'bi-exclamation-triangle-fill', tono: 'rojo', ayuda: 'Necesitan seguimiento', ruta: '/asistencia' },
        ]),
        error: () => this.indicadores.set([])
      });
      return;
    }
    if (this.tiene('RESPONSABLE_CONVIVENCIA', 'AUXILIAR')) {
      this.http.get<unknown[]>('/api/asistencias/riesgo').subscribe({
        next: lista => this.indicadores.set([
          { etiqueta: 'Con faltas recientes', valor: lista.length, icono: 'bi-person-exclamation', tono: lista.length ? 'ambar' : 'verde',
            ayuda: 'Estudiantes para hacer seguimiento', ruta: '/asistencia' }
        ]),
        error: () => this.indicadores.set([])
      });
    }
  }

  private cargarClases(): void {
    forkJoin({
      horarios: this.http.get<Clase[]>('/api/horarios/mios'),
      secciones: this.http.get<Seccion[]>('/api/secciones').pipe(catchError(() => of([] as Seccion[]))),
      aulas: this.http.get<{ id: number; nombre: string }[]>('/api/aulas').pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ horarios: crudos, secciones, aulas }) => {
        // Nombres legibles en lugar de identificadores.
        const horarios = crudos.map(h => {
          const sec = secciones.find(x => x.id === h.seccionId);
          return {
            ...h,
            seccionTexto: sec ? `${sec.gradoNombre ?? ''} ${sec.nombre}`.trim() : `Sección ${h.seccionId}`,
            aulaTexto: aulas.find(a => a.id === h.aulaId)?.nombre ?? `Aula ${h.aulaId}`
          };
        });
        // Hoy; si hoy no hay clases (fin de semana), el próximo día con clases.
        const hoy = new Date().getDay();
        let dia = DIAS[hoy];
        let delDia = horarios.filter(h => h.diaSemana === dia);
        for (let i = 1; delDia.length === 0 && i < 7 && horarios.length > 0; i++) {
          dia = DIAS[(hoy + i) % 7];
          delDia = horarios.filter(h => h.diaSemana === dia);
        }
        delDia.sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
        this.clases.set(delDia);
        this.diaClases.set(dia === DIAS[hoy] ? 'hoy' : `el ${DIA_LEGIBLE[dia] ?? dia.toLowerCase()}`);
        this.cargandoClases.set(false);
      },
      error: () => {
        this.clases.set([]);
        this.cargandoClases.set(false);
      }
    });
  }

  private cargarTickets(): void {
    this.http.get<Ticket[]>('/api/tickets').subscribe({
      next: lista => {
        const peso: Record<string, number> = { ALTA: 0, MEDIA: 1, BAJA: 2 };
        this.tickets.set(lista
          .filter(t => t.estado !== 'RESUELTO' && t.estado !== 'CERRADO')
          .sort((a, b) => (peso[a.urgencia] ?? 3) - (peso[b.urgencia] ?? 3)));
      },
      error: () => this.tickets.set([])
    });
  }

  private cargarSecciones(): void {
    this.seccionService.getSecciones().subscribe({
      next: data => this.secciones.set(data),
      error: () => this.secciones.set([])
    });
  }

  private cargarActividades(): void {
    this.dashboardService.getActividadesRecientes().subscribe({
      next: lista => this.actividadesServidor.set(lista.map(a => ({
        id: `srv-${a.id}`,
        texto: a.texto,
        fecha: a.fecha,
        icono: 'bi-clock-history',
        enVivo: false
      }))),
      error: () => this.actividadesServidor.set([])
    });
  }

  private tiene(...roles: string[]): boolean {
    return roles.some(rol => this.roles.includes(rol));
  }

  private aMinutos(hora: string): number {
    const [h, m] = (hora ?? '00:00').split(':').map(Number);
    return h * 60 + (m || 0);
  }

  private calcularSaludo(): string {
    const hora = new Date().getHours();
    if (hora < 12) return 'Buenos días';
    if (hora < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }
}
