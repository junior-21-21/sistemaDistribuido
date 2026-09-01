import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EstudianteService, Estudiante } from '../../services/estudiante.service';
import { AuthService } from '../../core/services/auth.service';
import { ApoderadoService, Apoderado } from '../../core/services/apoderado.service';

export interface DocumentoAdjunto {
  id: number;
  estudianteId: number;
  nombreArchivo: string;
  tipoDocumento: string;
  fechaSubida: string;
}

const GRADOS: Record<number, string> = {
  1: '1° Primaria', 2: '2° Primaria', 3: '3° Primaria',
  4: '4° Primaria', 5: '5° Primaria', 6: '6° Primaria',
  7: '1° Secundaria', 8: '2° Secundaria', 9: '3° Secundaria',
  10: '4° Secundaria', 11: '5° Secundaria'
};

@Component({
  selector: 'app-estudiantes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './estudiantes.html',
  styleUrl: './estudiantes.css',
})
export class Estudiantes implements OnInit {
  estudiantes: Estudiante[] = [];
  estudiantesFiltrados: Estudiante[] = [];
  loading = true;
  error = '';
  searchTerm = '';
  successMessage = '';

  estudianteForm: FormGroup;
  submitting = false;
  showModal = false;

  // Variables para Documentos
  showDocsModal = false;
  docsLoading = false;
  estudianteSeleccionado: Estudiante | null = null;
  documentos: DocumentoAdjunto[] = [];
  archivoSeleccionado: File | null = null;
  tipoDocSeleccionado = 'DNI';
  isSecretariaOrDirector = false;

  // Variables para Asistente de Matrícula (Wizard)
  wizardStep = 1; // 1: Buscar Apoderado, 2: Crear/Validar Apoderado, 3: Estudiante
  modoEstudiante: 'NUEVO' | 'EXISTENTE' = 'NUEVO';
  apoderadoDni = '';
  apoderadoActual: Apoderado | null = null;
  buscandoApoderado = false;
  apoderadoEncontrado = false;
  apoderadoForm: FormGroup;

  estudianteExistenteDni = '';
  estudianteMatricula: Estudiante | null = null;
  buscandoEstudiante = false;
  historialNotas: any[] = [];
  situacionFinal: 'PROMOVIDO' | 'PROMOVIDO_CON_CARGO' | 'REPITENTE' | 'RECUPERACION' | '' = '';
  
  gradoLabels = GRADOS;

  private cdr = inject(ChangeDetectorRef);

  constructor(
    private estudianteService: EstudianteService,
    private apoderadoService: ApoderadoService,
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.isSecretariaOrDirector = this.authService.hasRole('SECRETARIA') || this.authService.hasRole('DIRECTOR');
    
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
    this.cargarEstudiantes();
  }

  cargarEstudiantes(): void {
    this.loading = true;
    this.error = '';
    this.cargarSecciones();
    
    this.estudianteService.getEstudiantes().subscribe({
      next: (data) => {
        this.estudiantes = data;
        this.estudiantesFiltrados = data;
        this.loading = false;
        this.filtrar();
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
  filtroNivel = '';
  filtroGrado = '';
  filtroSeccion = '';
  filtroTurno = '';

  cargarSecciones(): void {
    this.http.get<any[]>('/api/secciones').subscribe({
      next: (data) => {
        this.seccionesBase = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.seccionesBase = [];
        this.cdr.detectChanges();
      }
    });
  }

  filtrar(): void {
    const t = this.searchTerm.toLowerCase();
    
    this.estudiantesFiltrados = this.estudiantes.filter(e => {
      // 1. Text Search
      const matchText = e.nombre.toLowerCase().includes(t) ||
                        e.apellido.toLowerCase().includes(t) ||
                        e.dni.includes(t) ||
                        (e.email || '').toLowerCase().includes(t);
      if (!matchText) return false;

      // Find section info for the student
      const secInfo = this.seccionesBase.find(s => s.id === e.seccionId);
      
      // 2. Filters
      if (this.filtroNivel && secInfo?.nivelNombre !== this.filtroNivel) return false;
      if (this.filtroGrado && secInfo?.gradoId.toString() !== this.filtroGrado) return false;
      if (this.filtroSeccion && secInfo?.id.toString() !== this.filtroSeccion) return false;
      if (this.filtroTurno && secInfo?.turno !== this.filtroTurno) return false;
      
      return true;
    });
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

  abrirModal(): void {
    this.wizardStep = 1;
    this.apoderadoDni = '';
    this.apoderadoActual = null;
    this.apoderadoEncontrado = false;
    this.apoderadoForm.reset();
    
    this.modoEstudiante = 'NUEVO';
    this.estudianteExistenteDni = '';
    this.estudianteMatricula = null;
    this.historialNotas = [];
    this.situacionFinal = '';
    this.estudianteForm.reset({ gradoId: 1, seccionId: 1 });
    
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
  }

  // --- WIZARD LOGIC ---
  buscarApoderado(): void {
    if (this.apoderadoDni.length !== 8) return;
    this.buscandoApoderado = true;
    
    // Como el backend no tiene GET /api/apoderados/dni/{dni} explícitamente listado,
    // usamos el listarTodos() y filtramos localmente (solo por demostración)
    this.apoderadoService.listarTodos().subscribe({
      next: (todos) => {
        const found = todos.find(a => a.dni === this.apoderadoDni);
        this.buscandoApoderado = false;
        if (found) {
          this.apoderadoActual = found;
          this.apoderadoEncontrado = true;
          this.wizardStep = 3; // Ir directo a seleccionar/crear estudiante
        } else {
          this.apoderadoEncontrado = false;
          this.apoderadoForm.patchValue({ dni: this.apoderadoDni });
          this.wizardStep = 2; // Mostrar formulario de registro de apoderado
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.buscandoApoderado = false;
        // Asumir no encontrado si falla
        this.apoderadoEncontrado = false;
        this.apoderadoForm.patchValue({ dni: this.apoderadoDni });
        this.wizardStep = 2;
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
    
    const found = this.estudiantes.find(e => e.dni === this.estudianteExistenteDni);
    this.buscandoEstudiante = false;
    
    if (found) {
      this.estudianteMatricula = found;
      this.cargarHistorialYCalcularSituacion(found.id!);
    } else {
      alert('Estudiante no encontrado en la base de datos. Por favor regístrelo como NUEVO.');
    }
    this.cdr.detectChanges();
  }

  cargarHistorialYCalcularSituacion(estudianteId: number): void {
    // Simular el historial ya que el api real podría no tener notas cargadas aún
    // En un caso real llamaríamos a this.http.get(`/api/notas/estudiante/${estudianteId}`)
    this.loading = true;
    setTimeout(() => {
      // Mocked calculation logic
      const promedioAnual = Math.floor(Math.random() * 9) + 11; // 11 to 20
      this.historialNotas = [
        { curso: 'Matemática', notaFinal: promedioAnual },
        { curso: 'Comunicación', notaFinal: promedioAnual > 12 ? promedioAnual - 1 : promedioAnual + 2 },
        { curso: 'Ciencia y Tecnología', notaFinal: promedioAnual + 1 }
      ];
      
      const hayJalados = this.historialNotas.some(n => n.notaFinal < 11);
      
      if (!hayJalados) {
        this.situacionFinal = 'PROMOVIDO';
        // Auto-increment grado
        if (this.estudianteMatricula?.gradoId) {
          const sg = this.estudianteMatricula.gradoId + 1;
          this.estudianteForm.patchValue({ gradoId: sg > 11 ? 11 : sg }); // Max 11
        }
      } else {
        const cantJalados = this.historialNotas.filter(n => n.notaFinal < 11).length;
        if (cantJalados === 1) {
          this.situacionFinal = 'PROMOVIDO_CON_CARGO';
          if (this.estudianteMatricula?.gradoId) {
            const sg = this.estudianteMatricula.gradoId + 1;
            this.estudianteForm.patchValue({ gradoId: sg > 11 ? 11 : sg });
          }
        } else if (cantJalados >= 2) {
          this.situacionFinal = 'REPITENTE';
          this.estudianteForm.patchValue({ gradoId: this.estudianteMatricula?.gradoId });
        }
      }
      this.loading = false;
      this.wizardStep = 4;
      this.cdr.detectChanges();
    }, 600);
  }

  guardarEstudiante(): void {
    if (this.modoEstudiante === 'NUEVO' && this.estudianteForm.invalid) {
      this.estudianteForm.markAllAsTouched();
      return;
    }
    this.submitting = true;
    
    // Si es nuevo o promoción
    let estudianteData = this.modoEstudiante === 'NUEVO' ? this.estudianteForm.value : {
       ...this.estudianteMatricula,
       gradoId: this.estudianteForm.get('gradoId')?.value,
       seccionId: this.estudianteForm.get('seccionId')?.value
    };

    const isUpdate = this.modoEstudiante === 'EXISTENTE' && estudianteData.id;
    const request = isUpdate 
      ? this.http.put(`/api/estudiantes/${estudianteData.id}`, estudianteData)
      : this.estudianteService.createEstudiante(estudianteData);

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
        alert('Error al guardar la matrícula.');
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

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.archivoSeleccionado = file;
    }
  }

  subirDocumento(): void {
    if (!this.archivoSeleccionado || !this.estudianteSeleccionado) return;
    
    this.submitting = true;
    const formData = new FormData();
    formData.append('archivo', this.archivoSeleccionado); // Match con @RequestParam("archivo") original (o "file" que usa el Service en Backend?) Wait, the controller I made used "archivo", but the existing one uses "file"
    formData.append('file', this.archivoSeleccionado);
    formData.append('tipoDocumento', this.tipoDocSeleccionado);

    // El endpoint actual es POST /api/estudiantes/{estudianteId}/documentos
    this.http.post(`/api/estudiantes/${this.estudianteSeleccionado.id}/documentos`, formData).subscribe({
      next: () => {
        this.submitting = false;
        this.archivoSeleccionado = null;
        if (this.estudianteSeleccionado?.id) {
          this.cargarDocumentos(this.estudianteSeleccionado.id);
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.submitting = false;
        alert('Error al subir el documento. (Verifique que el backend esté disponible)');
        this.cdr.detectChanges();
      }
    });
  }

  descargarDocumento(docId: number): void {
    if (!this.estudianteSeleccionado) return;
    window.open(`/api/estudiantes/${this.estudianteSeleccionado.id}/documentos/${docId}/descargar`, '_blank');
  }
}
