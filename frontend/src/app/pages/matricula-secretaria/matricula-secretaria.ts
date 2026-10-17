import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AreaPendiente, Estudiante, EstudianteService, MatriculaAnual, MatriculaRequest, Trayectoria } from '../../services/estudiante.service';
import { GradoService } from '../../core/services/grado.service';
import { SeccionService } from '../../core/services/seccion.service';
import { ApoderadoService, Apoderado } from '../../core/services/apoderado.service';
import { Grado } from '../../core/models/grado.model';
import { Seccion } from '../../core/models/seccion.model';

type TipoMatricula = 'NUEVO_INGRESO' | 'CONTINUIDAD' | 'TRASLADO_ENTRANTE';

interface Paso {
  numero: number;
  titulo: string;
  icono: string;
}

interface Parentesco {
  valor: string;
  etiqueta: string;
  icono: string;
}

/**
 * Matrícula como un asistente de 4 pasos (tipo → estudiante → aula → familia) con una ficha que
 * se va llenando en vivo. La matrícula en la IE pública es gratuita y solo exige el DNI.
 */
@Component({
  selector: 'app-matricula-secretaria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './matricula-secretaria.html',
  styleUrl: './matricula-secretaria.css'
})
export class MatriculaSecretaria implements OnInit {
  private fb = inject(FormBuilder);
  private estudianteService = inject(EstudianteService);
  private gradoService = inject(GradoService);
  private seccionService = inject(SeccionService);
  private apoderadoService = inject(ApoderadoService);
  private cdr = inject(ChangeDetectorRef);

  readonly anioMatricula = new Date().getFullYear();
  readonly pasos: Paso[] = [
    { numero: 1, titulo: '¿Cómo llega?', icono: 'bi-signpost-split' },
    { numero: 2, titulo: 'Estudiante', icono: 'bi-person-vcard' },
    { numero: 3, titulo: 'Aula', icono: 'bi-door-open' },
    { numero: 4, titulo: 'Familia', icono: 'bi-people' },
  ];
  readonly parentescos: Parentesco[] = [
    { valor: 'MADRE', etiqueta: 'Madre', icono: 'bi-gender-female' },
    { valor: 'PADRE', etiqueta: 'Padre', icono: 'bi-gender-male' },
    { valor: 'TUTOR', etiqueta: 'Tutor legal', icono: 'bi-shield-check' },
    { valor: 'ABUELO', etiqueta: 'Abuelo/a', icono: 'bi-heart' },
    { valor: 'HERMANO', etiqueta: 'Hermano/a', icono: 'bi-people' },
  ];

  matriculaForm!: FormGroup;
  apoderadoNuevoForm!: FormGroup;
  paso = 1;
  /** Dirección de la animación al cambiar de paso. */
  direccion: 'adelante' | 'atras' = 'adelante';

  grados: Grado[] = [];
  secciones: Seccion[] = [];
  ocupacion = new Map<number, number>();

  apoderadoEncontrado: Apoderado | null = null;
  apoderadoNoExiste = false;
  buscandoApoderado = false;
  guardandoApoderado = false;

  estudianteExistente: Estudiante | null = null;
  buscandoEstudiante = false;
  /** Historial del estudiante que continúa: situación final del año anterior y áreas pendientes. */
  trayectoria: Trayectoria | null = null;

  cargando = false;
  mensajeError: string | null = null;
  /** Resumen de la última matrícula registrada (pantalla de éxito). */
  matriculado: { nombre: string; aula: string; tipo: string } | null = null;

  ngOnInit(): void {
    this.initForm();
    this.cargarCatalogos();
  }

  // ---------------------------------------------------------------- formulario

  private initForm(): void {
    this.matriculaForm = this.fb.group({
      estudiante: this.fb.group({
        id: [null as number | null],
        nombre: ['', [Validators.required, Validators.maxLength(80)]],
        apellido: ['', [Validators.required, Validators.maxLength(120)]],
        dni: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
        // Solo el DNI (o acta de nacimiento) es exigible para matricular en una IE pública.
        email: ['', [Validators.email]],
        telefono: [''],
        gradoId: ['', Validators.required],
        seccionId: ['', Validators.required]
      }),
      tipoMatricula: ['NUEVO_INGRESO' as TipoMatricula, Validators.required],
      ieProcedencia: [''],
      codigoModularProcedencia: [''],
      apoderadoBuscador: [''],
      apoderadoId: ['', Validators.required],
      parentesco: ['MADRE', Validators.required],
      permisoRetiro: [false],
      consentimientoDatos: [true],
      consentimientoSalud: [true]
    });
    this.apoderadoNuevoForm = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9+ ]{7,15}$')]],
      email: ['', Validators.email]
    });

    // Búsquedas automáticas al completar los 8 dígitos del DNI
    this.matriculaForm.get('estudiante.dni')?.valueChanges.subscribe(dni => {
      this.estudianteExistente = null;
      this.trayectoria = null;
      if (/^[0-9]{8}$/.test(dni ?? '')) this.buscarEstudiante(dni);
    });
    this.matriculaForm.get('apoderadoBuscador')?.valueChanges.subscribe(dni => {
      this.apoderadoEncontrado = null;
      this.apoderadoNoExiste = false;
      this.matriculaForm.get('apoderadoId')?.setValue('');
      if (/^[0-9]{8}$/.test(dni ?? '')) this.buscarApoderado();
    });
  }

  private cargarCatalogos(): void {
    this.gradoService.getGrados().subscribe({
      next: res => {
        const secundaria = res.filter(g => g.activo !== false && (!g.nivelNombre || g.nivelNombre === 'SECUNDARIA'));
        this.grados = secundaria.length ? secundaria : res;
        this.cdr.detectChanges();
      },
      error: () => this.grados = []
    });
    this.seccionService.getSecciones().subscribe({
      next: res => { this.secciones = res.filter(s => s.activa !== false); this.cdr.detectChanges(); },
      error: () => this.secciones = []
    });
    this.contarOcupacion();
  }

  /** Estudiantes activos por sección, para mostrar las vacantes. */
  private contarOcupacion(): void {
    this.estudianteService.getEstudiantes().subscribe({
      next: lista => {
        const mapa = new Map<number, number>();
        for (const e of lista) {
          if (e.seccionId && (e.estado ?? 'ACTIVO') === 'ACTIVO') mapa.set(e.seccionId, (mapa.get(e.seccionId) ?? 0) + 1);
        }
        this.ocupacion = mapa;
        this.cdr.detectChanges();
      }
    });
  }

  get tipoMatricula(): TipoMatricula {
    return this.matriculaForm?.get('tipoMatricula')?.value ?? 'NUEVO_INGRESO';
  }

  get estudiante(): FormGroup {
    return this.matriculaForm.get('estudiante') as FormGroup;
  }

  campo(ruta: string) {
    return this.matriculaForm.get(ruta);
  }

  invalido(ruta: string): boolean {
    const control = this.matriculaForm.get(ruta);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  elegirTipo(tipo: TipoMatricula): void {
    this.matriculaForm.get('tipoMatricula')?.setValue(tipo);
    const procedencia = this.matriculaForm.get('ieProcedencia');
    if (tipo === 'TRASLADO_ENTRANTE') procedencia?.setValidators([Validators.required, Validators.maxLength(150)]);
    else procedencia?.clearValidators();
    procedencia?.updateValueAndValidity();
    // Si cambió el tipo, vuelve a evaluar el DNI ya escrito
    const dni = this.estudiante.get('dni')?.value;
    if (/^[0-9]{8}$/.test(dni ?? '')) this.buscarEstudiante(dni);
  }

  // ---------------------------------------------------------------- estudiante

  private buscarEstudiante(dni: string): void {
    this.buscandoEstudiante = true;
    this.estudianteService.getEstudiantesPage(0, 5, dni).subscribe({
      next: pagina => {
        this.buscandoEstudiante = false;
        this.estudianteExistente = pagina.content.find(e => e.dni === dni) ?? null;
        if (!this.estudianteExistente && this.estudiante.get('id')?.value) this.estudiante.get('id')?.setValue(null);
        this.cdr.detectChanges();
      },
      error: () => { this.buscandoEstudiante = false; this.cdr.detectChanges(); }
    });
  }

  /** Carga los datos del estudiante que ya estudiaba en el colegio (matrícula por continuidad). */
  usarEstudianteExistente(): void {
    const e = this.estudianteExistente;
    if (!e) return;
    if (this.tipoMatricula !== 'CONTINUIDAD') this.elegirTipo('CONTINUIDAD');
    // El cierre del año ya dejó al estudiante en el grado y la sección que le corresponden
    this.estudiante.patchValue({
      id: e.id, nombre: e.nombre, apellido: e.apellido, email: e.email ?? '', telefono: e.telefono ?? '',
      gradoId: e.gradoId ?? '', seccionId: e.seccionId ?? ''
    }, { emitEvent: false });
    this.estudianteService.trayectoria(e.id!).subscribe({
      next: t => { this.trayectoria = t; this.cdr.detectChanges(); },
      error: () => { this.trayectoria = null; this.cdr.detectChanges(); }
    });
  }

  /** Última matrícula anterior al año que se matricula (de ahí sale la situación final). */
  get matriculaAnterior(): MatriculaAnual | undefined {
    return this.trayectoria?.matriculas
      .filter(m => m.anioEscolar < this.anioMatricula)
      .sort((a, b) => b.anioEscolar - a.anioEscolar)[0];
  }

  get situacionAnterior(): string | undefined {
    return this.matriculaAnterior?.situacionFinal ?? undefined;
  }

  /** Áreas que el estudiante lleva por subsanar (hasta diciembre) o que aún está recuperando. */
  get areasPendientes(): AreaPendiente[] {
    return (this.trayectoria?.areasPendientes ?? []).filter(a => a.estado === 'PENDIENTE');
  }

  get yaTieneMatriculaDelAnio(): boolean {
    return !!this.trayectoria?.matriculas.some(m => m.anioEscolar === this.anioMatricula);
  }

  /** Bloqueos de la norma: no se puede matricular a quien sigue en recuperación o ya egresó. */
  get bloqueoTrayectoria(): string {
    if (!this.usandoExistente) return '';
    if (this.situacionAnterior === 'RECUPERACION') return 'Sigue en recuperación: cierra primero su caso';
    if (this.situacionAnterior === 'EGRESADO' || this.estudianteExistente?.estado === 'EGRESADO') return 'Ya egresó de Secundaria';
    return '';
  }

  get usandoExistente(): boolean {
    return !!this.estudiante.get('id')?.value;
  }

  // ---------------------------------------------------------------- aula

  get gradoSeleccionado(): Grado | undefined {
    return this.grados.find(g => String(g.id) === String(this.estudiante.get('gradoId')?.value));
  }

  get seccionesDelGrado(): Seccion[] {
    const gradoId = Number(this.estudiante.get('gradoId')?.value);
    return this.secciones.filter(s => s.gradoId === gradoId)
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
  }

  get seccionSeleccionada(): Seccion | undefined {
    return this.secciones.find(s => String(s.id) === String(this.estudiante.get('seccionId')?.value));
  }

  elegirGrado(grado: Grado): void {
    this.estudiante.patchValue({ gradoId: grado.id, seccionId: '' });
  }

  elegirSeccion(seccion: Seccion): void {
    if (this.llena(seccion)) return;
    this.estudiante.patchValue({ seccionId: seccion.id });
  }

  matriculados(seccion: Seccion): number {
    return this.ocupacion.get(seccion.id!) ?? 0;
  }

  vacantes(seccion: Seccion): number {
    return Math.max(0, (seccion.capacidadMaxima || 0) - this.matriculados(seccion));
  }

  llena(seccion: Seccion): boolean {
    // Un estudiante que continúa en su misma sección no ocupa una vacante nueva.
    const esSuya = this.usandoExistente && this.estudianteExistente?.seccionId === seccion.id;
    return !!seccion.capacidadMaxima && this.vacantes(seccion) === 0 && !esSuya;
  }

  porcentajeOcupado(seccion: Seccion): number {
    return seccion.capacidadMaxima ? Math.min(100, Math.round(this.matriculados(seccion) * 100 / seccion.capacidadMaxima)) : 0;
  }

  // ---------------------------------------------------------------- familia

  buscarApoderado(): void {
    const dni = this.matriculaForm.get('apoderadoBuscador')?.value;
    if (!/^[0-9]{8}$/.test(dni ?? '')) return;
    this.buscandoApoderado = true;
    this.apoderadoService.buscarPorDni(dni).subscribe({
      next: apod => {
        this.apoderadoEncontrado = apod;
        this.apoderadoNoExiste = false;
        this.matriculaForm.get('apoderadoId')?.setValue(apod.id);
        this.buscandoApoderado = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.apoderadoEncontrado = null;
        this.apoderadoNoExiste = true;
        this.matriculaForm.get('apoderadoId')?.setValue('');
        this.buscandoApoderado = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Registra al apoderado en el momento si aún no existe y lo deja seleccionado. */
  registrarApoderado(): void {
    if (this.apoderadoNuevoForm.invalid) {
      this.apoderadoNuevoForm.markAllAsTouched();
      return;
    }
    const dni = this.matriculaForm.get('apoderadoBuscador')?.value;
    const datos = this.apoderadoNuevoForm.value;
    this.guardandoApoderado = true;
    this.apoderadoService.guardar({
      dni, nombre: datos.nombre.trim(), apellido: datos.apellido.trim(),
      telefono: datos.telefono.trim(), email: datos.email?.trim() || undefined
    }).subscribe({
      next: apod => {
        this.guardandoApoderado = false;
        this.apoderadoEncontrado = apod;
        this.apoderadoNoExiste = false;
        this.matriculaForm.get('apoderadoId')?.setValue(apod.id);
        this.apoderadoNuevoForm.reset();
        this.cdr.detectChanges();
      },
      error: err => {
        this.guardandoApoderado = false;
        this.mensajeError = err.error?.message || 'No se pudo registrar al apoderado.';
        this.cdr.detectChanges();
      }
    });
  }

  elegirParentesco(valor: string): void {
    this.matriculaForm.get('parentesco')?.setValue(valor);
  }

  alternar(control: string): void {
    const c = this.matriculaForm.get(control);
    c?.setValue(!c.value);
  }

  // ---------------------------------------------------------------- pasos

  /** Qué falta para poder avanzar desde el paso actual (vacío = puede avanzar). */
  faltaEnPaso(paso: number): string {
    switch (paso) {
      case 1:
        return this.tipoMatricula === 'TRASLADO_ENTRANTE' && this.campo('ieProcedencia')?.invalid
          ? 'Escribe la institución de procedencia' : '';
      case 2: {
        if (this.estudiante.get('dni')?.invalid) return 'Escribe el DNI de 8 dígitos';
        if (this.estudianteExistente && !this.usandoExistente) return 'Este DNI ya está registrado: usa sus datos';
        if (this.bloqueoTrayectoria) return this.bloqueoTrayectoria;
        if (this.estudiante.get('nombre')?.invalid || this.estudiante.get('apellido')?.invalid) return 'Completa nombres y apellidos';
        if (this.estudiante.get('email')?.invalid) return 'Revisa el correo';
        return '';
      }
      case 3:
        if (!this.estudiante.get('gradoId')?.value) return 'Elige el grado';
        if (!this.estudiante.get('seccionId')?.value) return 'Elige la sección';
        return '';
      default:
        return this.matriculaForm.get('apoderadoId')?.value ? '' : 'Busca o registra al apoderado';
    }
  }

  pasoCompleto(paso: number): boolean {
    return !this.faltaEnPaso(paso);
  }

  irA(paso: number): void {
    // Solo se puede saltar hacia atrás o a pasos cuyos anteriores ya estén completos
    for (let p = 1; p < paso; p++) if (!this.pasoCompleto(p)) return;
    this.direccion = paso >= this.paso ? 'adelante' : 'atras';
    this.paso = paso;
    this.mensajeError = null;
  }

  siguiente(): void {
    const falta = this.faltaEnPaso(this.paso);
    if (falta) {
      this.matriculaForm.markAllAsTouched();
      return;
    }
    if (this.paso < 4) this.irA(this.paso + 1);
    else this.matricular();
  }

  atras(): void {
    if (this.paso > 1) this.irA(this.paso - 1);
  }

  get progreso(): number {
    return [1, 2, 3, 4].filter(p => this.pasoCompleto(p)).length * 25;
  }

  get etiquetaTipo(): string {
    return { NUEVO_INGRESO: 'Nuevo ingreso', CONTINUIDAD: 'Continúa', TRASLADO_ENTRANTE: 'Traslado' }[this.tipoMatricula];
  }

  get iniciales(): string {
    const n = (this.estudiante.get('nombre')?.value ?? '').trim();
    const a = (this.estudiante.get('apellido')?.value ?? '').trim();
    return ((n[0] ?? '') + (a[0] ?? '')).toUpperCase() || '?';
  }

  get etiquetaParentesco(): string {
    return this.parentescos.find(p => p.valor === this.matriculaForm.get('parentesco')?.value)?.etiqueta ?? '';
  }

  // ---------------------------------------------------------------- registrar

  matricular(): void {
    if (this.matriculaForm.invalid || [1, 2, 3, 4].some(p => !this.pasoCompleto(p))) {
      this.matriculaForm.markAllAsTouched();
      return;
    }
    const formValue = this.matriculaForm.getRawValue();
    const request: MatriculaRequest = {
      estudiante: { ...formValue.estudiante, id: formValue.estudiante.id ?? undefined },
      apoderadoId: formValue.apoderadoId,
      parentesco: formValue.parentesco,
      permisoRetiro: formValue.permisoRetiro,
      tipoMatricula: formValue.tipoMatricula,
      ieProcedencia: formValue.tipoMatricula === 'TRASLADO_ENTRANTE' ? formValue.ieProcedencia : null,
      codigoModularProcedencia: formValue.tipoMatricula === 'TRASLADO_ENTRANTE' ? formValue.codigoModularProcedencia : null,
      consentimientos: [
        { finalidad: 'DATOS_ACADEMICOS', accion: formValue.consentimientoDatos ? 'OTORGADO' : 'DENEGADO' },
        { finalidad: 'SALUD', accion: formValue.consentimientoSalud ? 'OTORGADO' : 'DENEGADO' }
      ]
    };

    this.cargando = true;
    this.mensajeError = null;
    this.estudianteService.matricular(request).subscribe({
      next: () => {
        this.matriculado = {
          nombre: `${formValue.estudiante.nombre} ${formValue.estudiante.apellido}`,
          aula: `${this.gradoSeleccionado?.nombre ?? ''} · Sección ${this.seccionSeleccionada?.nombre ?? ''}`,
          tipo: this.etiquetaTipo
        };
        this.cargando = false;
        this.contarOcupacion();
        this.cdr.detectChanges();
      },
      error: err => {
        this.mensajeError = err.error?.message || 'No se pudo registrar la matrícula. Revisa los datos.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Limpia todo para atender al siguiente estudiante de la fila. */
  nuevaMatricula(): void {
    this.matriculaForm.reset({
      estudiante: { id: null, nombre: '', apellido: '', dni: '', email: '', telefono: '', gradoId: '', seccionId: '' },
      tipoMatricula: 'NUEVO_INGRESO', ieProcedencia: '', codigoModularProcedencia: '',
      apoderadoBuscador: '', apoderadoId: '', parentesco: 'MADRE',
      permisoRetiro: false, consentimientoDatos: true, consentimientoSalud: true
    });
    this.elegirTipo('NUEVO_INGRESO');
    this.apoderadoEncontrado = null;
    this.apoderadoNoExiste = false;
    this.estudianteExistente = null;
    this.trayectoria = null;
    this.matriculado = null;
    this.direccion = 'atras';
    this.paso = 1;
  }
}
