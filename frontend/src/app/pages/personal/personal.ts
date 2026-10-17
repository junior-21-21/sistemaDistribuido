import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  CrearPersonalRequest,
  EstadoPersonal,
  PersonalData,
  PersonalService
} from '../../services/personal.service';
import { Apoderado, ApoderadoService } from '../../core/services/apoderado.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ModulosColegioService } from '../../core/services/modulos-colegio.service';
import {
  CATALOGO_ROLES,
  MODULO_ETIQUETA,
  ModuloColegio,
  ROL_ETIQUETA,
  RolCatalogo
} from '../../core/navigation';

/** Selección de puesto + encargos usada al crear y al editar una cuenta. */
interface SeleccionRoles {
  puesto: string;
  encargos: string[];
}

const PUEDEN_TENER_ENCARGO = ['DOCENTE', 'AUXILIAR'];

@Component({
  selector: 'app-personal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personal.html',
  styleUrl: './personal.css'
})
export class Personal implements OnInit {
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly personalService = inject(PersonalService);
  private readonly apoderadoService = inject(ApoderadoService);
  readonly modulos = inject(ModulosColegioService);

  readonly etiqueta: Record<string, string | undefined> = ROL_ETIQUETA;
  readonly etiquetaModulo = MODULO_ETIQUETA;
  readonly listaModulos: ModuloColegio[] = ['PSICOLOGIA', 'ENFERMERIA', 'MANTENIMIENTO', 'PORTERIA'];
  readonly encargos = CATALOGO_ROLES.filter(rol => rol.tipo === 'ENCARGO');
  readonly todosLosRoles = CATALOGO_ROLES.filter(rol => rol.codigo !== 'APODERADO');

  personalList: PersonalData[] = [];
  filteredList: PersonalData[] = [];
  searchTerm = '';
  filtroRol = '';
  cargando = false;
  guardando = false;
  accionEnCurso: number | null = null;
  cambiandoModulo: ModuloColegio | null = null;
  mostrarFormulario = false;
  mensaje = '';
  tipoMensaje: 'success' | 'error' = 'success';
  claveTemporal = '';
  paginaActual = 0;
  totalPaginas = 0;
  totalPersonal = 0;
  apoderados: Apoderado[] = [];
  /** Responsables actuales de cada encargo (para el resumen). */
  responsables: Record<string, PersonalData[] | undefined> = {};

  nuevoPersonal: CrearPersonalRequest = this.formularioVacio();
  seleccionNueva: SeleccionRoles = { puesto: 'DOCENTE', encargos: [] };

  /** Cuenta que se está editando y su selección provisional. */
  editando: PersonalData | null = null;
  seleccionEdicion: SeleccionRoles = { puesto: 'DOCENTE', encargos: [] };

  ngOnInit(): void {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('usuarios').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.cargarPersonal(this.paginaActual); this.cargarResponsables(); });
    this.modulos.cargar();
    this.cargarPersonal();
    this.cargarResponsables();
    this.apoderadoService.listarTodos().subscribe({ next: items => this.apoderados = items });
  }

  // ---------------------------------------------------------------- puestos y encargos

  /** Puestos que se pueden asignar ahora (los opcionales solo si su módulo está habilitado). */
  puestosDisponibles(incluirApoderado: boolean): RolCatalogo[] {
    const estado = this.modulos.estado() ?? {};
    return CATALOGO_ROLES.filter(rol =>
      (rol.tipo === 'BASE' && (incluirApoderado || rol.codigo !== 'APODERADO'))
      || (rol.tipo === 'OPCIONAL' && !!rol.modulo && estado[rol.modulo] === true));
  }

  admiteEncargos(seleccion: SeleccionRoles): boolean {
    return PUEDEN_TENER_ENCARGO.includes(seleccion.puesto);
  }

  elegirPuesto(seleccion: SeleccionRoles, puesto: string): void {
    seleccion.puesto = puesto;
    if (!this.admiteEncargos(seleccion)) seleccion.encargos = [];
  }

  alternarEncargo(seleccion: SeleccionRoles, encargo: string): void {
    seleccion.encargos = seleccion.encargos.includes(encargo)
      ? seleccion.encargos.filter(item => item !== encargo)
      : [...seleccion.encargos, encargo];
  }

  private rolesDe(seleccion: SeleccionRoles): string[] {
    return [seleccion.puesto, ...(this.admiteEncargos(seleccion) ? seleccion.encargos : [])];
  }

  puestoDe(persona: PersonalData): string {
    const roles = persona.roles?.length ? persona.roles : [persona.rol];
    return roles.find(rol => !this.esEncargo(rol)) ?? persona.rol;
  }

  encargosDe(persona: PersonalData): string[] {
    return (persona.roles ?? []).filter(rol => this.esEncargo(rol));
  }

  /** Otros puestos de la persona además del principal (por ejemplo, subdirector que también enseña). */
  otrosPuestosDe(persona: PersonalData): string[] {
    const puesto = this.puestoDe(persona);
    return (persona.roles ?? []).filter(rol => rol !== puesto && !this.esEncargo(rol));
  }

  esEncargo(rol: string): boolean {
    return CATALOGO_ROLES.some(item => item.codigo === rol && item.tipo === 'ENCARGO');
  }

  iconoDe(rol: string): string {
    return CATALOGO_ROLES.find(item => item.codigo === rol)?.icono ?? 'bi-person';
  }

  abrirEdicion(persona: PersonalData): void {
    this.editando = persona;
    this.seleccionEdicion = { puesto: this.puestoDe(persona), encargos: this.encargosDe(persona) };
    this.limpiarMensaje();
  }

  cancelarEdicion(): void {
    this.editando = null;
  }

  guardarEdicion(): void {
    const persona = this.editando;
    if (!persona) return;
    // Conserva los otros puestos (p. ej. Dirección + Docente) y reemplaza puesto principal y encargos.
    const otros = this.otrosPuestosDe(persona).filter(rol => rol !== this.seleccionEdicion.puesto);
    const roles = [...this.rolesDe(this.seleccionEdicion), ...otros];
    this.accionEnCurso = persona.id;
    this.personalService.actualizarRoles(persona.id, roles)
      .pipe(finalize(() => this.accionEnCurso = null))
      .subscribe({
        next: actualizado => {
          this.reemplazar(actualizado);
          this.editando = null;
          this.cargarResponsables();
          this.mostrarExito(`Se actualizaron el puesto y los encargos de ${actualizado.nombre}. Verá los cambios al volver a ingresar.`);
        },
        error: error => this.mostrarError(error, 'No se pudieron guardar los cambios.')
      });
  }

  // ---------------------------------------------------------------- módulos del colegio

  moduloHabilitado(modulo: ModuloColegio): boolean {
    return this.modulos.estado()?.[modulo] === true;
  }

  alternarModulo(modulo: ModuloColegio): void {
    const habilitar = !this.moduloHabilitado(modulo);
    this.cambiandoModulo = modulo;
    this.limpiarMensaje();
    this.modulos.cambiar(modulo, habilitar)
      .pipe(finalize(() => this.cambiandoModulo = null))
      .subscribe({
        next: () => this.mostrarExito(habilitar
          ? `Módulo de ${MODULO_ETIQUETA[modulo]} habilitado. Ya puedes crear cuentas con ese puesto.`
          : `Módulo de ${MODULO_ETIQUETA[modulo]} deshabilitado.`),
        error: error => this.mostrarError(error, 'No se pudo cambiar el módulo.')
      });
  }

  // ---------------------------------------------------------------- listado

  cargarPersonal(pagina = 0): void {
    this.cargando = true;
    this.personalService.listarPagina(pagina, 20, this.searchTerm, this.filtroRol)
      .pipe(finalize(() => this.cargando = false))
      .subscribe({
        next: page => {
          this.personalList = page.content;
          this.filteredList = page.content;
          this.paginaActual = page.number;
          this.totalPaginas = page.totalPages;
          this.totalPersonal = page.totalElements;
        },
        error: error => this.mostrarError(error, 'No se pudo cargar el personal.')
      });
  }

  private cargarResponsables(): void {
    for (const encargo of this.encargos) {
      this.personalService.listarPagina(0, 50, '', encargo.codigo).subscribe({
        next: page => this.responsables = {
          ...this.responsables,
          [encargo.codigo]: page.content.filter(item => item.estado === 'ACTIVO')
        }
      });
    }
  }

  filtrar(): void {
    this.cargarPersonal(0);
  }

  cambiarPagina(delta: number): void {
    const destino = this.paginaActual + delta;
    if (destino >= 0 && destino < this.totalPaginas) this.cargarPersonal(destino);
  }

  abrirFormulario(): void {
    this.mostrarFormulario = true;
    this.limpiarMensaje();
  }

  cancelarFormulario(): void {
    this.mostrarFormulario = false;
    this.nuevoPersonal = this.formularioVacio();
    this.seleccionNueva = { puesto: 'DOCENTE', encargos: [] };
  }

  crearPersonal(): void {
    if (this.guardando) return;
    this.guardando = true;
    this.limpiarMensaje();
    const roles = this.rolesDe(this.seleccionNueva);
    const request = { ...this.nuevoPersonal, roles, rol: roles[0],
      apoderadoId: this.seleccionNueva.puesto === 'APODERADO' ? this.nuevoPersonal.apoderadoId : null };
    this.personalService.crear(request)
      .pipe(finalize(() => this.guardando = false))
      .subscribe({
        next: creado => {
          this.personalList = [...this.personalList, creado];
          this.filteredList = [...this.personalList];
          this.cancelarFormulario();
          this.cargarResponsables();
          this.mostrarExito(`La cuenta de ${creado.nombre} fue creada. Debe cambiar su clave al ingresar.`);
        },
        error: error => this.mostrarError(error, 'No se pudo crear la cuenta.')
      });
  }

  toggleEstado(persona: PersonalData): void {
    const nuevoEstado: EstadoPersonal = persona.estado === 'ACTIVO' ? 'PAUSADO' : 'ACTIVO';
    this.actualizarEstado(persona, nuevoEstado);
  }

  resetClave(persona: PersonalData): void {
    if (!confirm(`¿Generar una nueva clave temporal para ${persona.nombre}?`)) return;
    this.accionEnCurso = persona.id;
    this.limpiarMensaje();
    this.personalService.restablecerClave(persona.id)
      .pipe(finalize(() => this.accionEnCurso = null))
      .subscribe({
        next: response => {
          persona.debeCambiarClave = true;
          this.claveTemporal = response.claveTemporal;
          this.mostrarExito(`Clave temporal generada para ${persona.nombre}. Cópiala ahora; no volverá a mostrarse.`);
        },
        error: error => this.mostrarError(error, 'No se pudo restablecer la clave.')
      });
  }

  darDeBaja(persona: PersonalData): void {
    if (confirm(`¿Dar de baja a ${persona.nombre}? Su acceso quedará revocado.`)) {
      this.actualizarEstado(persona, 'BAJA');
    }
  }

  private actualizarEstado(persona: PersonalData, estado: EstadoPersonal): void {
    this.accionEnCurso = persona.id;
    this.limpiarMensaje();
    this.personalService.cambiarEstado(persona.id, estado)
      .pipe(finalize(() => this.accionEnCurso = null))
      .subscribe({
        next: actualizado => {
          this.reemplazar(actualizado);
          this.cargarResponsables();
          this.mostrarExito(`El acceso de ${actualizado.nombre} quedó ${actualizado.estado.toLowerCase()}.`);
        },
        error: error => this.mostrarError(error, 'No se pudo cambiar el estado de la cuenta.')
      });
  }

  private reemplazar(actualizado: PersonalData): void {
    const indice = this.personalList.findIndex(item => item.id === actualizado.id);
    if (indice >= 0) this.personalList[indice] = actualizado;
    this.personalList = [...this.personalList];
    this.filteredList = [...this.personalList];
  }

  private formularioVacio(): CrearPersonalRequest {
    return { username: '', clave: '', nombre: '', apellido: '', email: '', rol: 'DOCENTE', roles: ['DOCENTE'], apoderadoId: null };
  }

  private mostrarExito(mensaje: string): void {
    this.tipoMensaje = 'success';
    this.mensaje = mensaje;
  }

  private mostrarError(error: HttpErrorResponse, predeterminado: string): void {
    this.tipoMensaje = 'error';
    this.mensaje = error.error?.message || error.error?.error || predeterminado;
  }

  limpiarMensaje(): void {
    this.mensaje = '';
    this.claveTemporal = '';
  }
}
