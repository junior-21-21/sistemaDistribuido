/**
 * Mapa único de navegación. Lo usan el menú lateral, el buscador (Ctrl+K), el título de la
 * barra superior y los accesos rápidos del inicio, para que todo diga lo mismo.
 * Los roles deben coincidir con los de app.routes.ts.
 */
export interface NavItem {
  path: string;
  label: string;
  icon: string;
  descripcion: string;
  grupo: GrupoNav;
  roles: string[];
  /** Palabras extra para el buscador. */
  claves?: string;
  /** Módulos de tiempo real que afectan a esta pantalla (para el indicador "nuevo"). */
  modulos?: string[];
  /** Módulo opcional del colegio: la pantalla solo aparece si Dirección lo habilitó. */
  moduloColegio?: ModuloColegio;
}

/** Módulos que Dirección habilita solo si el colegio tiene ese personal. */
export type ModuloColegio = 'PSICOLOGIA' | 'ENFERMERIA' | 'MANTENIMIENTO' | 'PORTERIA';

export type GrupoNav = 'Inicio' | 'Académico' | 'Convivencia y bienestar' | 'Secretaría' | 'Gestión';

export const GRUPOS: GrupoNav[] = ['Inicio', 'Académico', 'Convivencia y bienestar', 'Secretaría', 'Gestión'];

const TODOS = ['DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR', 'APODERADO', 'PSICOLOGO', 'ENFERMERA',
  'MANTENIMIENTO', 'PORTERO', 'RESPONSABLE_CONVIVENCIA', 'PRIMEROS_AUXILIOS'];

export const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Inicio', icon: 'bi-house-door', grupo: 'Inicio', roles: TODOS,
    descripcion: 'Resumen del día y accesos rápidos', claves: 'panel dashboard resumen hoy' },
  { path: '/portal-apoderado', label: 'Mis hijos', icon: 'bi-mortarboard', grupo: 'Inicio', roles: ['APODERADO'],
    descripcion: 'Notas, asistencia y documentos de tus hijos', claves: 'portal familia apoderado', modulos: ['notas', 'asistencias'] },

  { path: '/evaluaciones', label: 'Notas', icon: 'bi-journal-check', grupo: 'Académico',
    roles: ['DOCENTE', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Registrar y revisar calificaciones por bimestre', claves: 'evaluaciones calificaciones rendimiento bimestre', modulos: ['notas', 'bimestres'] },
  { path: '/asistencia', label: 'Asistencia', icon: 'bi-person-check', grupo: 'Académico',
    roles: ['DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Tomar asistencia de la clase en segundos', claves: 'faltas tardanzas lista', modulos: ['asistencias'] },
  { path: '/horarios', label: 'Carga y horarios', icon: 'bi-calendar3', grupo: 'Académico',
    roles: ['DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Asignar cursos a docentes y generar horarios', claves: 'carga academica horario generar', modulos: ['horarios', 'asignaciones-academicas'] },
  { path: '/gestor-aula', label: 'Gestor de aula', icon: 'bi-easel', grupo: 'Académico',
    roles: ['DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Docentes y bloques de una sección', claves: 'aula seccion bloque', modulos: ['horarios', 'asignaciones-academicas'] },
  { path: '/secciones', label: 'Secciones', icon: 'bi-grid-3x3-gap', grupo: 'Académico',
    roles: ['DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Grados, secciones, turnos y vacantes', claves: 'grados aulas turnos vacantes', modulos: ['secciones'] },
  { path: '/cursos', label: 'Cursos', icon: 'bi-book', grupo: 'Académico', roles: ['DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Catálogo de áreas y cursos', claves: 'areas materias', modulos: ['cursos'] },

  { path: '/ingreso', label: 'Ingreso al colegio', icon: 'bi-upc-scan', grupo: 'Convivencia y bienestar',
    roles: ['AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'PORTERO', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Escanear el DNI en la puerta y ver quién llegó', claves: 'porteria entrada dni codigo barras tardanza llegada', modulos: ['ingresos'] },
  { path: '/disciplina', label: 'Convivencia', icon: 'bi-shield-check', grupo: 'Convivencia y bienestar',
    roles: ['DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Incidencias y seguimiento de conducta', claves: 'disciplina incidencia conducta papeleta', modulos: ['reportes-disciplinarios', 'disciplina'] },
  { path: '/permisos', label: 'Permisos y justificaciones', icon: 'bi-envelope-paper', grupo: 'Convivencia y bienestar',
    roles: ['AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DOCENTE', 'SECRETARIA', 'PORTERO', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Justificar faltas, papeletas de salida y permisos formales',
    claves: 'justificacion falta inasistencia permiso papeleta salida retiro recoger mesa de partes', modulos: ['permisos'] },
  { path: '/derivaciones', label: 'Psicología', icon: 'bi-heart-pulse', grupo: 'Convivencia y bienestar',
    roles: ['DOCENTE', 'PSICOLOGO', 'RESPONSABLE_CONVIVENCIA'],
    descripcion: 'Derivaciones y su atención', claves: 'derivacion psicologo', modulos: ['derivaciones'],
    moduloColegio: 'PSICOLOGIA' },
  { path: '/topico', label: 'Tópico', icon: 'bi-bandaid', grupo: 'Convivencia y bienestar', roles: ['ENFERMERA', 'PRIMEROS_AUXILIOS'],
    descripcion: 'Atenciones de salud y fichas médicas', claves: 'enfermeria salud atencion fichas medicas alergias seguro', modulos: ['atenciones-medicas', 'fichas-medicas'] },
  { path: '/notificaciones', label: 'Notificaciones', icon: 'bi-send-check', grupo: 'Convivencia y bienestar',
    roles: ['DIRECTOR', 'SUBDIRECTOR', 'RESPONSABLE_CONVIVENCIA'],
    descripcion: 'Mensajes enviados a las familias', claves: 'mensajes avisos familias correo whatsapp', modulos: ['notificaciones'] },

  { path: '/estudiantes', label: 'Estudiantes', icon: 'bi-people', grupo: 'Secretaría', roles: ['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Buscar fichas, apoderados y documentos', claves: 'alumnos padron fichas', modulos: ['estudiantes', 'documentos', 'consentimientos'] },
  { path: '/matricula', label: 'Matricular', icon: 'bi-person-plus', grupo: 'Secretaría', roles: ['SECRETARIA'],
    descripcion: 'Registrar un nuevo estudiante', claves: 'nuevo alumno inscripcion matricula', modulos: ['estudiantes'] },

  { path: '/nominas', label: 'Nóminas de matrícula', icon: 'bi-journal-bookmark', grupo: 'Secretaría',
    roles: ['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Nómina por sección: Secretaría la prepara y Dirección la aprueba',
    claves: 'nomina matricula siagie firma aprobar director lista oficial', modulos: ['nominas', 'estudiantes'] },
  { path: '/promocion', label: 'Promoción y recuperación', icon: 'bi-mortarboard', grupo: 'Secretaría',
    roles: ['SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Pasar de grado y áreas por subsanar', claves: 'cierre año promocion repite recuperacion subsanacion traslado pasar grado', modulos: ['recuperacion'] },
  { path: '/tickets', label: 'Mantenimiento', icon: 'bi-tools', grupo: 'Gestión', roles: ['DOCENTE', 'MANTENIMIENTO', 'DIRECTOR', 'SUBDIRECTOR'],
    descripcion: 'Reportar y atender fallas de aulas', claves: 'tickets falla reparacion', modulos: ['tickets'] },
  { path: '/personal', label: 'Personal y encargos', icon: 'bi-person-gear', grupo: 'Gestión', roles: ['DIRECTOR'],
    descripcion: 'Cuentas, puestos, encargos y módulos del colegio',
    claves: 'usuarios roles contraseña cuentas encargos responsable convivencia primeros auxilios modulos psicologia enfermeria',
    modulos: ['usuarios', 'institucion'] },
  { path: '/auditoria', label: 'Auditoría', icon: 'bi-clock-history', grupo: 'Gestión', roles: ['DIRECTOR'],
    descripcion: 'Quién hizo qué y cuándo', claves: 'registro historial trazabilidad' },
];

export const ROL_ETIQUETA: Record<string, string> = {
  DIRECTOR: 'Dirección',
  SUBDIRECTOR: 'Subdirección',
  PORTERO: 'Portería',
  SECRETARIA: 'Secretaría',
  DOCENTE: 'Docente',
  AUXILIAR: 'Auxiliar de educación',
  APODERADO: 'Familia',
  PSICOLOGO: 'Psicología',
  ENFERMERA: 'Enfermería',
  MANTENIMIENTO: 'Mantenimiento',
  RESPONSABLE_CONVIVENCIA: 'Coordinación de TOE',
  PRIMEROS_AUXILIOS: 'Primeros auxilios',
};

export type TipoRol = 'BASE' | 'OPCIONAL' | 'ENCARGO';

export interface RolCatalogo {
  codigo: string;
  tipo: TipoRol;
  icono: string;
  descripcion: string;
  modulo?: ModuloColegio;
}

/** Catálogo de roles del colegio (alineado con RolInstitucional del backend). */
export const CATALOGO_ROLES: RolCatalogo[] = [
  { codigo: 'DIRECTOR', tipo: 'BASE', icono: 'bi-building', descripcion: 'Máxima autoridad: aprueba las nóminas de matrícula y los permisos formales, gestiona al personal y supervisa todo.' },
  { codigo: 'SUBDIRECTOR', tipo: 'BASE', icono: 'bi-diagram-3', descripcion: 'Lo pedagógico de su nivel: carga y horarios, tutores, monitoreo y consolidado de notas.' },
  { codigo: 'DOCENTE', tipo: 'BASE', icono: 'bi-easel2', descripcion: 'Solo sus secciones: asistencia, notas, incidencias y derivaciones.' },
  { codigo: 'AUXILIAR', tipo: 'BASE', icono: 'bi-person-badge', descripcion: 'Asistencia y puntualidad, incidencias, papeletas de salida, justificaciones y citaciones a padres.' },
  { codigo: 'SECRETARIA', tipo: 'BASE', icono: 'bi-folder2-open', descripcion: 'Mesa de partes, matrícula (la aprueba Dirección), constancias, traslados y SIAGIE.' },
  { codigo: 'PSICOLOGO', tipo: 'OPCIONAL', modulo: 'PSICOLOGIA', icono: 'bi-heart-pulse', descripcion: 'Atiende las derivaciones (si el colegio tiene psicólogo/a).' },
  { codigo: 'ENFERMERA', tipo: 'OPCIONAL', modulo: 'ENFERMERIA', icono: 'bi-bandaid', descripcion: 'Tópico y fichas de salud (si el colegio tiene enfermera/o).' },
  { codigo: 'MANTENIMIENTO', tipo: 'OPCIONAL', modulo: 'MANTENIMIENTO', icono: 'bi-tools', descripcion: 'Atiende las fallas reportadas (si no hay, las ve Dirección).' },
  { codigo: 'PORTERO', tipo: 'OPCIONAL', modulo: 'PORTERIA', icono: 'bi-door-open', descripcion: 'Personal de servicio y guardianía: ingreso con DNI y control de papeletas de salida.' },
  { codigo: 'RESPONSABLE_CONVIVENCIA', tipo: 'ENCARGO', icono: 'bi-shield-check', descripcion: 'Coordinación de TOE: plan de tutoría y convivencia, casos de violencia, SíseVe y derivaciones.' },
  { codigo: 'PRIMEROS_AUXILIOS', tipo: 'ENCARGO', icono: 'bi-plus-square', descripcion: 'Tópico y fichas de salud cuando no hay enfermera (auxiliar o alguien de TOE).' },
];

export const MODULO_ETIQUETA: Record<ModuloColegio, string> = {
  PSICOLOGIA: 'Psicología',
  ENFERMERIA: 'Enfermería',
  MANTENIMIENTO: 'Mantenimiento',
  PORTERIA: 'Portería',
};

/** Pantallas que puede ver una persona según sus roles y los módulos habilitados del colegio. */
export function itemsParaRoles(roles: string[], modulos?: Partial<Record<ModuloColegio, boolean>>): NavItem[] {
  return NAV_ITEMS.filter(item => item.roles.some(rol => roles.includes(rol))
    && (!item.moduloColegio || !modulos || modulos[item.moduloColegio] !== false));
}

export function itemPorRuta(url: string): NavItem | undefined {
  const limpia = url.split('?')[0].split('#')[0];
  return [...NAV_ITEMS]
    .sort((a, b) => b.path.length - a.path.length)
    .find(item => limpia === item.path || limpia.startsWith(item.path + '/'));
}

export function iniciales(nombre: string | undefined | null): string {
  if (!nombre) return '?';
  const partes = nombre.trim().split(/\s+/).filter(Boolean);
  return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase() || '?';
}
