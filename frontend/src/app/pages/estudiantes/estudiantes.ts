import { Component, OnInit, ChangeDetectorRef, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EstudianteService, Estudiante } from '../../services/estudiante.service';
import { AuthService } from '../../core/services/auth.service';
import { ApoderadoService, Apoderado } from '../../core/services/apoderado.service';
import { Nota, NotaService } from '../../services/nota.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ContinuarSeccion, SeccionRecordada, UltimaSeccionService } from '../../shared/continuar-seccion/continuar-seccion';

export interface DocumentoAdjunto {
  id: number;
  estudianteId: number;
  nombreArchivo: string;
  tipoDocumento: string;
  fechaSubida: string;
}

const GRADOS: Record<number, string> = {};

@Component({
  selector: 'app-estudiantes',
  standalone: true,
  imports: [ContinuarSeccion, CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './estudiantes.html',
  styleUrl: './estudiantes.css',
})
export class Estudiantes implements OnInit {
  private readonly ultimaSeccion = inject(UltimaSeccionService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  estudiantes: Estudiante[] = [];
  estudiantesFiltrados: Estudiante[] = [];
  loading = true;
  error = '';

  // Navegacion por cajones
  vistaActual: 'grados' | 'secciones' | 'estudiantes' = 'grados';
  gradoNavegacion: any = null;
  seccionNavegacion: any = null;
  searchTerm = '';
  successMessage = '';
  paginaActual = 0;
  totalPaginas = 0;
  totalEstudiantes = 0;
  readonly tamanoPagina = 20;

  estudianteForm: FormGroup;
  submitting = false;
  showModal = false;

  // Variables para Documentos
  showDocsModal = false;
  docsLoading = false;
  estudianteSeleccionado: Estudiante | null = null;
  documentos: DocumentoAdjunto[] = [];
  archivoSeleccionado: File | null = null;
  documentoError = '';
  tipoDocSeleccionado = 'DNI';
  isSecretariaOrDirector = false;
  trasladoDe: Estudiante | null = null;
  // Cambio de sección dentro del grado (traslado interno)
  puedeCambiarSeccion = false;
  cambioDe: Estudiante | null = null;
  cambio = { seccionDestinoId: null as number | null, motivo: '' };
  opcionesCambio: { id: number; etiqueta: string; ocupados: number; capacidad: number; llena: boolean }[] = [];
  cargandoOpciones = false;
  traslado = { ieDestino: '', codigoModular: '', fecha: new Date().toISOString().slice(0, 10), motivo: '' };
  puedeMatricular = false;

  // Variables para Asistente de Matrícula (Wizard)
  wizardStep = 1; // 1: Buscar Apoderado, 2: Crear/Validar Apoderado, 3: Estudiante
  modoEstudiante: 'NUEVO' | 'EXISTENTE' = 'NUEVO';
  apoderadoDni = '';
  apoderadoActual: Apoderado | null = null;
  buscandoApoderado = false;
  apoderadoEncontrado = false;
  apoderadoForm: FormGroup;
  parentesco = 'MADRE';
  permisoRetiro = true;
  consentimientoAcademico = false;
  consentimientoSalud = false;
  consentimientoComunicaciones = false;
  evidenciaConsentimiento = '';
  matriculaError = '';

  estudianteExistenteDni = '';
  estudianteMatricula: Estudiante | null = null;
  buscandoEstudiante = false;
  historialNotas: any[] = [];
  historialMensaje = '';
  situacionFinal: 'PROMOVIDO' | 'PROMOVIDO_CON_CARGO' | 'REPITENTE' | 'RECUPERACION' | '' = '';
  
  gradoLabels = GRADOS;

  private cdr = inject(ChangeDetectorRef);

  constructor(
    private estudianteService: EstudianteService,
    private apoderadoService: ApoderadoService,
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService,
    private notaService: NotaService
  ) {
    this.isSecretariaOrDirector = this.authService.hasRole('SECRETARIA') || this.authService.hasRole('DIRECTOR');
    this.puedeCambiarSeccion = this.authService.hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR');
    this.puedeMatricular = this.authService.hasRole('SECRETARIA');
    
    this.apoderadoForm = this.fb.group({
      nombre: ['', [Validators.required]],
      apellido: ['', [Validators.required]],
      dni: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
      telefono: ['', Validators.required],
      email: ['']
    });

    this.estudianteForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      apellido: ['', [Validators.required, Validators.minLength(2)]],
      dni: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
      email: ['', [Validators.email]],
      telefono: [''],
      gradoId: [1, Validators.required],
      seccionId: [1, Validators.required]
    });
  }

  ngOnInit(): void {
    // Se actualiza solo cuando otra persona registra cambios.
    this.realtime.alCambiar('estudiantes', 'documentos', 'consentimientos').pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.cargarEstudiantes(this.paginaActual); });
    this.cargarSecciones();
  }

  cargarEstudiantes(pagina = 0): void {
    this.loading = true;
    this.error = '';
    let secciones = this.seccionesBase;
    if (this.filtroNivel) secciones = secciones.filter(s => s.nivelNombre === this.filtroNivel);
    if (this.filtroTurno) secciones = secciones.filter(s => s.turno === this.filtroTurno);
    if (this.filtroSeccion) secciones = secciones.filter(s => s.id === Number(this.filtroSeccion));
    const seccionIds = (this.filtroNivel || this.filtroTurno || this.filtroSeccion) ? secciones.map(s => Number(s.id)) : [];
    const gradoId = this.filtroGrado ? Number(this.filtroGrado) : undefined;

    this.estudianteService.getEstudiantesPage(pagina, this.tamanoPagina, this.searchTerm, gradoId, seccionIds).subscribe({
      next: (data) => {
        this.estudiantes = data.content;
        this.estudiantesFiltrados = data.content;
        this.paginaActual = data.number;
        this.totalPaginas = data.totalPages;
        this.totalEstudiantes = data.totalElements;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'No se pudo conectar a la Base de Datos. Por favor, enciende los microservicios de Java.';
        this.loading = false;
        this.estudiantes = [];
        this.estudiantesFiltrados = [];
        this.cdr.detectChanges();
      }
    });
  }

  seccionesBase: any[] = [];
  filtroNivel = 'SECUNDARIA';
  filtroGrado = '';
  filtroSeccion = '';
  filtroTurno = '';

  cargarSecciones(): void {
    this.http.get<any[]>('/api/secciones').subscribe({
      next: (data) => {
        this.seccionesBase = data;
        this.cargarEstudiantes(0);
        this.cdr.detectChanges();
      },
      error: () => {
        this.seccionesBase = [];
        this.cdr.detectChanges();
      }
    });
  }

  filtrar(): void {
    this.cargarEstudiantes(0);
  }

  exportarLista(): void {
    if (!this.seccionNavegacion) return;
    const filas = this.estudiantes.map(e => [e.dni, e.apellido, e.nombre, e.email, e.telefono, e.estado ?? '']);
    const escapar = (valor: unknown) => `"${String(valor ?? '').replace(/"/g, '""')}"`;
    const csv = [['DNI', 'Apellidos', 'Nombres', 'Email', 'Teléfono', 'Estado'], ...filas]
      .map(fila => fila.map(escapar).join(';'))
      .join('\r\n');
    // BOM para que Excel reconozca UTF-8 (tildes y ñ).
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `estudiantes-${this.gradoNavegacion?.nombre ?? 'grado'}-${this.seccionNavegacion?.nombre ?? 'seccion'}.csv`
      .replace(/\s+/g, '_');
    enlace.click();
    URL.revokeObjectURL(url);
    this.successMessage = `Se exportaron ${filas.length} estudiantes de la página actual.`;
    this.cdr.detectChanges();
    setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
  }


  cambiarPagina(delta: number): void {
    const destino = this.paginaActual + delta;
    if (destino >= 0 && destino < this.totalPaginas) this.cargarEstudiantes(destino);
  }

  gradoLabel(id: number): string {
    const s = this.seccionesBase.find(sec => sec.gradoId === id);
    if (s) return `${s.gradoNombre} ${s.nivelNombre}`;
    return GRADOS[id] || `Grado ${id}`;
  }

  getTurnoLabel(seccionId: number): string {
    const s = this.seccionesBase.find(sec => sec.id === seccionId);
    return s ? s.turno : 'N/A';
  }

  getSeccionNombre(seccionId: number): string {
    const s = this.seccionesBase.find(sec => sec.id === seccionId);
    return s ? s.nombre : 'N/A';
  }

  get nivelesDisponibles(): string[] {
    return [...new Set(this.seccionesBase.map(s => s.nivelNombre).filter(Boolean))];
  }

  get gradosDisponibles(): any[] {
    let secs = this.seccionesBase;
    if (this.filtroNivel) secs = secs.filter(s => s.nivelNombre === this.filtroNivel);
    
    const uniqueMap = new Map();
    secs.forEach(s => {
      if (s.gradoId && !uniqueMap.has(s.gradoId)) {
        uniqueMap.set(s.gradoId, { id: s.gradoId, nombre: s.gradoNombre });
      }
    });
    return Array.from(uniqueMap.values());
  }

  get seccionesDisponibles(): any[] {
    let secs = this.seccionesBase;
    if (this.filtroNivel) secs = secs.filter(s => s.nivelNombre === this.filtroNivel);
    if (this.filtroGrado) secs = secs.filter(s => s.gradoId.toString() === this.filtroGrado);
    
    const uniqueMap = new Map();
    secs.forEach(s => {
      if (s.id && !uniqueMap.has(s.id)) {
        uniqueMap.set(s.id, { id: s.id, nombre: s.nombre });
      }
    });
    return Array.from(uniqueMap.values());
  }

  get gradosMatricula(): any[] {
    const unicos = new Map<number, any>();
    this.seccionesBase
      .filter(s => s.activa !== false && s.nivelNombre === 'SECUNDARIA')
      .forEach(s => unicos.set(Number(s.gradoId), { id: Number(s.gradoId), nombre: s.gradoNombre }));
    return Array.from(unicos.values()).sort((a, b) => a.id - b.id);
  }

  get seccionesMatricula(): any[] {
    const gradoId = Number(this.estudianteForm.get('gradoId')?.value);
    const anio = new Date().getFullYear();
    return this.seccionesBase.filter(s => s.activa !== false
      && s.nivelNombre === 'SECUNDARIA'
      && Number(s.gradoId) === gradoId
      && (!s.anioEscolar || Number(s.anioEscolar) === anio));
  }

  abrirModal(): void {
    this.wizardStep = 1;
    this.apoderadoDni = '';
    this.apoderadoActual = null;
    this.apoderadoEncontrado = false;
    this.apoderadoForm.reset();
    this.parentesco = 'MADRE';
    this.permisoRetiro = true;
    this.consentimientoAcademico = false;
    this.consentimientoSalud = false;
    this.consentimientoComunicaciones = false;
    this.evidenciaConsentimiento = '';
    this.matriculaError = '';
    
    this.modoEstudiante = 'NUEVO';
    this.estudianteExistenteDni = '';
    this.estudianteMatricula = null;
    this.historialNotas = [];
    this.situacionFinal = '';
    const primerGrado = this.gradosMatricula[0]?.id ?? null;
    const primeraSeccion = this.seccionesBase.find(s => Number(s.gradoId) === primerGrado
      && s.nivelNombre === 'SECUNDARIA' && s.activa !== false)?.id ?? null;
    this.estudianteForm.reset({ gradoId: primerGrado, seccionId: primeraSeccion, email: '', telefono: '' });
    
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
  }

  cambiarGradoMatricula(): void {
    this.estudianteForm.patchValue({ seccionId: this.seccionesMatricula[0]?.id ?? null });
  }

  // --- WIZARD LOGIC ---
  buscarApoderado(): void {
    if (this.apoderadoDni.length !== 8) return;
    this.buscandoApoderado = true;
    
    this.apoderadoService.buscarPorDni(this.apoderadoDni).subscribe({
      next: (found) => {
        this.buscandoApoderado = false;
        this.apoderadoActual = found;
        this.apoderadoEncontrado = true;
        this.wizardStep = 3;
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.buscandoApoderado = false;
        if (response.status === 404) {
          this.apoderadoEncontrado = false;
          this.apoderadoForm.patchValue({ dni: this.apoderadoDni });
          this.wizardStep = 2;
        } else {
          this.error = 'No se pudo consultar el DNI del apoderado.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  guardarApoderadoWizard(): void {
    if (this.apoderadoForm.invalid) {
      this.apoderadoForm.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.apoderadoService.guardar(this.apoderadoForm.value).subscribe({
      next: (nuevo) => {
        this.apoderadoActual = nuevo;
        this.apoderadoEncontrado = true;
        this.submitting = false;
        this.wizardStep = 3;
        this.cdr.detectChanges();
      },
      error: () => {
        this.submitting = false;
        alert('Error al registrar apoderado.');
        this.cdr.detectChanges();
      }
    });
  }

  buscarEstudianteWizard(): void {
    if (this.estudianteExistenteDni.length !== 8) return;
    this.buscandoEstudiante = true;
    
    this.estudianteService.getEstudiantesPage(0, 2, this.estudianteExistenteDni).subscribe({
      next: page => {
        this.buscandoEstudiante = false;
        const found = page.content.find(e => e.dni === this.estudianteExistenteDni);
        if (found) {
          this.estudianteMatricula = found;
          this.cargarHistorialYCalcularSituacion(found.id!);
        } else {
          alert('Estudiante no encontrado en la base de datos. Por favor regístrelo como NUEVO.');
        }
        this.cdr.detectChanges();
      },
      error: () => { this.buscandoEstudiante = false; this.error = 'No se pudo buscar al estudiante.'; this.cdr.detectChanges(); }
    });
  }

  cargarHistorialYCalcularSituacion(estudianteId: number): void {
    this.loading = true;
    this.historialMensaje = '';
    this.notaService.getNotasPorEstudiante(estudianteId).subscribe({
      next: notas => {
        this.historialNotas = this.resumirNotasPorCurso(notas);
        if (!this.historialNotas.length) {
          this.situacionFinal = '';
          this.historialMensaje = 'No hay notas registradas. La situación final debe revisarse manualmente.';
        } else {
          const cursosDesaprobados = this.historialNotas.filter(nota => nota.notaFinal < 11).length;
          this.situacionFinal = cursosDesaprobados === 0
            ? 'PROMOVIDO'
            : cursosDesaprobados === 1 ? 'PROMOVIDO_CON_CARGO' : 'REPITENTE';
          const gradoActual = this.estudianteMatricula?.gradoId;
          if (gradoActual) {
            const gradoDestino = this.situacionFinal === 'REPITENTE'
              ? gradoActual
              : Math.min(gradoActual + 1, 11);
            this.estudianteForm.patchValue({ gradoId: gradoDestino });
          }
        }
        this.loading = false;
        this.wizardStep = 4;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.historialNotas = [];
        this.situacionFinal = '';
        this.historialMensaje = 'No se pudo consultar el historial. No se calculó una situación final.';
        this.wizardStep = 4;
        this.cdr.detectChanges();
      }
    });
  }

  private resumirNotasPorCurso(notas: Nota[]): Array<{ curso: string; notaFinal: number }> {
    const grupos = new Map<number, Nota[]>();
    notas.forEach(nota => grupos.set(nota.cursoId, [...(grupos.get(nota.cursoId) ?? []), nota]));
    return Array.from(grupos.entries()).map(([cursoId, registros]) => {
      const valores = registros.map(nota => nota.valorNumerico ?? this.valorEscala(nota.escalaLiteral));
      const promedio = valores.reduce((total, valor) => total + valor, 0) / valores.length;
      return {
        curso: registros[0].competencia || `Curso ${cursoId}`,
        notaFinal: Math.round(promedio * 10) / 10
      };
    });
  }

  private valorEscala(escala?: string): number {
    return ({ AD: 18, A: 15, B: 12, C: 9 } as Record<string, number>)[escala ?? ''] ?? 0;
  }

  guardarEstudiante(): void {
    if (this.modoEstudiante === 'NUEVO' && this.estudianteForm.invalid) {
      this.estudianteForm.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.matriculaError = '';
    
    // Si es nuevo o promoción
    let estudianteData = this.modoEstudiante === 'NUEVO' ? this.estudianteForm.value : {
       ...this.estudianteMatricula,
       gradoId: this.estudianteForm.get('gradoId')?.value,
       seccionId: this.estudianteForm.get('seccionId')?.value
    };

    if (!this.apoderadoActual?.id) {
      this.submitting = false;
      this.matriculaError = 'Selecciona o registra un apoderado antes de confirmar la matrícula.';
      return;
    }
    estudianteData = {
      ...estudianteData,
      email: estudianteData.email?.trim() || null,
      telefono: estudianteData.telefono?.trim() || null
    };
    const isUpdate = this.modoEstudiante === 'EXISTENTE' && estudianteData.id;
    const request = this.estudianteService.matricular({
      estudiante: estudianteData,
      apoderadoId: this.apoderadoActual.id,
      parentesco: this.parentesco,
      permisoRetiro: this.permisoRetiro,
      consentimientos: [
        { finalidad: 'DATOS_ACADEMICOS', accion: this.consentimientoAcademico ? 'OTORGADO' : 'DENEGADO', evidencia: this.evidenciaConsentimiento },
        { finalidad: 'SALUD', accion: this.consentimientoSalud ? 'OTORGADO' : 'DENEGADO', evidencia: this.evidenciaConsentimiento },
        { finalidad: 'COMUNICACIONES', accion: this.consentimientoComunicaciones ? 'OTORGADO' : 'DENEGADO', evidencia: this.evidenciaConsentimiento }
      ]
    });

    request.subscribe({
      next: (data: any) => {
        if (!isUpdate) {
          this.estudiantes.unshift(data);
        } else {
          const idx = this.estudiantes.findIndex(e => e.id === data.id);
          if (idx !== -1) this.estudiantes[idx] = data;
        }
        
        this.filtrar();
        this.submitting = false;
        this.showModal = false;
        this.successMessage = `Matrícula de "${data.nombre} ${data.apellido}" completada.`;
        this.cdr.detectChanges();
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.submitting = false;
        const primerMensaje = Object.values(err.error || {}).find(value => typeof value === 'string');
        this.matriculaError = err.error?.message
          || (typeof primerMensaje === 'string' ? primerMensaje : '')
          || 'No se pudo guardar la matrícula. Revisa los datos e inténtalo nuevamente.';
        this.cdr.detectChanges();
      }
    });
  }

  // === LÓGICA DE DOCUMENTOS ===
  abrirDocsModal(est: Estudiante): void {
    this.estudianteSeleccionado = est;
    this.showDocsModal = true;
    if (est.id) {
      this.cargarDocumentos(est.id);
    }
  }

  cerrarDocsModal(): void {
    this.showDocsModal = false;
    this.estudianteSeleccionado = null;
    this.documentos = [];
    this.archivoSeleccionado = null;
    this.documentoError = '';
  }

  cargarDocumentos(estudianteId: number): void {
    this.docsLoading = true;
    this.http.get<DocumentoAdjunto[]>(`/api/estudiantes/${estudianteId}/documentos`).subscribe({
      next: (data) => {
        this.documentos = data;
        this.docsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.docsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.documentoError = '';
    this.archivoSeleccionado = null;
    if (!file) return;

    const tiposPermitidos = ['application/pdf', 'image/jpeg', 'image/png'];
    if (!tiposPermitidos.includes(file.type)) {
      this.documentoError = 'Solo se permiten archivos PDF, JPEG o PNG.';
      input.value = '';
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.documentoError = 'El archivo supera el límite de 10 MB.';
      input.value = '';
      return;
    }
    this.archivoSeleccionado = file;
  }

  subirDocumento(): void {
    if (!this.archivoSeleccionado || !this.estudianteSeleccionado) return;
    
    this.submitting = true;
    this.documentoError = '';
    const formData = new FormData();
    formData.append('file', this.archivoSeleccionado);
    formData.append('tipoDocumento', this.tipoDocSeleccionado);

    this.http.post(`/api/estudiantes/${this.estudianteSeleccionado.id}/documentos`, formData).subscribe({
      next: () => {
        this.submitting = false;
        this.archivoSeleccionado = null;
        if (this.estudianteSeleccionado?.id) {
          this.cargarDocumentos(this.estudianteSeleccionado.id);
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.submitting = false;
        this.documentoError = error.error?.message || error.error || 'No se pudo subir el documento.';
        this.cdr.detectChanges();
      }
    });
  }

  // === NAVEGACION POR CAJONES ===
  /** Abre directamente la última sección usada (salta los pasos de grado y sección). */
  continuarUltima(seccion: SeccionRecordada): void {
    this.navegarAGrado({ id: seccion.gradoId, nombre: seccion.gradoNombre });
    this.navegarASeccion(seccion);
  }

  navegarAGrado(grado: any): void {
    this.gradoNavegacion = grado;
    this.vistaActual = 'secciones';
  }

  navegarASeccion(seccion: any): void {
    this.ultimaSeccion.guardar(seccion);
    this.seccionNavegacion = seccion;
    this.filtroGrado = seccion.gradoId?.toString();
    this.filtroSeccion = seccion.id?.toString();
    this.vistaActual = 'estudiantes';
    this.filtrar();
  }

  volverAGrados(): void {
    this.vistaActual = 'grados';
    this.gradoNavegacion = null;
    this.seccionNavegacion = null;
    this.filtroGrado = '';
    this.filtroSeccion = '';
  }

  volverASecciones(): void {
    this.vistaActual = 'secciones';
    this.seccionNavegacion = null;
    this.filtroSeccion = '';
  }

  getSeccionesDelGradoNav(): any[] {
    if (!this.gradoNavegacion) return [];
    return this.seccionesBase.filter(s => s.gradoId?.toString() === this.gradoNavegacion.id?.toString());
  }

  getSeccionesMananaNav(): any[] {
    return this.getSeccionesDelGradoNav().filter(s => s.turno === 'MANANA');
  }

  getSeccionesTardeNav(): any[] {
    return this.getSeccionesDelGradoNav().filter(s => s.turno === 'TARDE');
  }

  contarEstudiantesEnSeccion(seccionId: number): string {
    return '...';
  }

  descargarDocumento(documento: DocumentoAdjunto): void {
    if (!this.estudianteSeleccionado) return;
    this.documentoError = '';
    this.http.get(
      `/api/estudiantes/${this.estudianteSeleccionado.id}/documentos/${documento.id}/descargar`,
      { responseType: 'blob' }
    ).subscribe({
      next: contenido => {
        const url = URL.createObjectURL(contenido);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = documento.nombreArchivo;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.documentoError = 'No se pudo descargar el documento.';
        this.cdr.detectChanges();
      }
    });
  }

  abrirTraslado(est: Estudiante): void {
    this.trasladoDe = est;
    this.traslado = { ieDestino: '', codigoModular: '', fecha: new Date().toISOString().slice(0, 10), motivo: '' };
  }

  confirmarTraslado(): void {
    const est = this.trasladoDe;
    if (!est?.id || !this.traslado.ieDestino.trim()) return;
    this.submitting = true;
    this.estudianteService.trasladarSalida(est.id, this.traslado).subscribe({
      next: () => {
        this.submitting = false;
        this.trasladoDe = null;
        this.successMessage = `${est.nombre} ${est.apellido} quedó registrado como trasladado.`;
        this.cargarEstudiantes(this.paginaActual);
        this.cdr.detectChanges();
      },
      error: error => {
        this.submitting = false;
        this.error = error.error?.message || 'No se pudo registrar el traslado.';
        this.cdr.detectChanges();
      }
    });
  }

  abrirCambioSeccion(est: Estudiante): void {
    this.cambioDe = est;
    this.cambio = { seccionDestinoId: null, motivo: '' };
    const anio = new Date().getFullYear();
    const candidatas = this.seccionesBase.filter(s => s.activa !== false && s.anioEscolar === anio
      && Number(s.gradoId) === Number(est.gradoId) && Number(s.id) !== Number(est.seccionId));
    this.opcionesCambio = [];
    this.cargandoOpciones = true;
    // Los estudiantes activos por sección salen del estado de nóminas.
    this.http.get<{ seccionId: number; estudiantes: number }[]>('/api/nominas', { params: { anio } }).subscribe({
      next: estados => {
        const ocupados = new Map(estados.map(e => [e.seccionId, e.estudiantes]));
        this.opcionesCambio = candidatas.map(s => {
          const n = ocupados.get(Number(s.id)) ?? 0;
          const capacidad = Number(s.capacidadMaxima) || 0;
          return { id: Number(s.id), etiqueta: `${s.gradoNombre ?? ''} ${s.nombre} · ${s.turno === 'TARDE' ? 'Tarde' : 'Mañana'}`.trim(),
            ocupados: n, capacidad, llena: capacidad > 0 && n >= capacidad };
        });
        this.cargandoOpciones = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.opcionesCambio = candidatas.map(s => ({ id: Number(s.id), etiqueta: `${s.gradoNombre ?? ''} ${s.nombre}`.trim(),
          ocupados: 0, capacidad: Number(s.capacidadMaxima) || 0, llena: false }));
        this.cargandoOpciones = false;
        this.cdr.detectChanges();
      }
    });
  }

  confirmarCambioSeccion(): void {
    const est = this.cambioDe;
    const destino = this.cambio.seccionDestinoId;
    if (!est?.id || destino == null || this.cambio.motivo.trim().length < 5) return;
    this.submitting = true;
    this.estudianteService.cambiarSeccion(est.id, destino, this.cambio.motivo.trim()).subscribe({
      next: () => {
        this.submitting = false;
        this.cambioDe = null;
        const etiqueta = this.opcionesCambio.find(o => o.id === destino)?.etiqueta ?? 'la nueva sección';
        this.successMessage = `${est.nombre} ${est.apellido} pasó a ${etiqueta}.`;
        this.cargarEstudiantes(this.paginaActual);
        this.cdr.detectChanges();
      },
      error: error => {
        this.submitting = false;
        this.error = error.error?.message || 'No se pudo cambiar de sección.';
        this.cdr.detectChanges();
      }
    });
  }
}
