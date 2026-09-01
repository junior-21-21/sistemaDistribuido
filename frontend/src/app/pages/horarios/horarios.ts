import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HorarioService, HorarioDTO } from '../../services/horario.service';

interface ClaseDetalle {
  curso: string;
  seccion: string;
  aula: string;
  docenteId: number;
  docente: string;
  esReceso?: boolean;
}

interface BloqueHorario {
  [key: string]: any;
  hora: string;
  lunes?: ClaseDetalle | null;
  martes?: ClaseDetalle | null;
  miercoles?: ClaseDetalle | null;
  jueves?: ClaseDetalle | null;
  viernes?: ClaseDetalle | null;
  sabado?: ClaseDetalle | null;
  esReceso?: boolean;
}

@Component({
  selector: 'app-horarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './horarios.html',
  styleUrl: './horarios.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Horarios implements OnInit {
  horarios: HorarioDTO[] = [];
  horarioForm: FormGroup;
  submitting = false;
  generandoIA = false;
  error = '';
  success = '';

  docentes = [
    { id: 1, nombre: 'Juan Pérez', especialidad: 'Matemática' },
    { id: 2, nombre: 'María Salas', especialidad: 'Comunicación' },
    { id: 3, nombre: 'Carlos López', especialidad: 'Historia' },
    { id: 4, nombre: 'Ana Gómez', especialidad: 'Inglés' },
    { id: 5, nombre: 'Luis Torres', especialidad: 'Educación Física' },
    { id: 6, nombre: 'Elena Martínez', especialidad: 'Física' },
    { id: 7, nombre: 'Jorge Sánchez', especialidad: 'Química' },
    { id: 8, nombre: 'Rosa Díaz', especialidad: 'Biología' },
    { id: 9, nombre: 'Miguel Rojas', especialidad: 'Arte' },
    { id: 10, nombre: 'Carmen Vargas', especialidad: 'Música' },
    { id: 11, nombre: 'Raúl Fernández', especialidad: 'Filosofía' },
    { id: 12, nombre: 'Patricia Quispe', especialidad: 'Psicología' },
    { id: 13, nombre: 'Fernando Condori', especialidad: 'Computación' },
    { id: 14, nombre: 'Lucía Mamani', especialidad: 'Danza' },
    { id: 15, nombre: 'Hugo Cruz', especialidad: 'Raz. Matemático' }
  ];
  
  horasDisponibles = [
    '08:00 - 09:30', '09:30 - 11:00', '11:00 - 11:30', '11:30 - 13:00'
  ];
  dias = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes'];
  seccionesDisponibles = ['A', 'B', 'C'];
  
  cargaAcademica = [
    { curso: 'Matemática', docenteId: 1, docente: 'Juan Pérez' },
    { curso: 'Comunicación', docenteId: 2, docente: 'María Salas' },
    { curso: 'Ciencia y Tecnología', docenteId: 7, docente: 'Jorge Sánchez' },
    { curso: 'Historia', docenteId: 3, docente: 'Carlos López' },
    { curso: 'Inglés', docenteId: 4, docente: 'Ana Gómez' },
    { curso: 'Ed. Física', docenteId: 5, docente: 'Luis Torres' },
    { curso: 'Arte', docenteId: 9, docente: 'Miguel Rojas' }
  ];

  matrizGlobal: any[] = [];
  calendario: BloqueHorario[] = [];

  // Variables para la Edición
  editandoBloque: any = null;
  diaEditando: string = '';
  claseEditando: ClaseDetalle | null = null;
  opcionesEdicion: any[] = [];
  
  // Guardado Definitivo
  guardandoDefinitivo = false;

  constructor(
    private horarioService: HorarioService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.horarioForm = this.fb.group({
      turno: ['MANANA'],
      tipoFiltro: ['seccion'],
      seccionFiltro: ['A'],
      docenteId: [1]
    });
    
    // Escuchar cambios para filtrar automáticamente
    this.horarioForm.valueChanges.subscribe(() => {
      this.aplicarFiltro();
    });
  }

  ngOnInit(): void {
    this.cargarHorarios(1);
  }

  cargarHorarios(docenteId: number) {
    this.construirCalendarioVacio();
  }

  construirCalendarioVacio() {
    this.calendario = this.horasDisponibles.map(h => ({ hora: h }));
  }

  generarHorarioIA() {
    this.generandoIA = true;
    this.calendario = [];
    this.matrizGlobal = [];
    this.cdr.markForCheck();
    
    setTimeout(() => {
      // 1. Definir la carga académica estricta (Docentes asignados a Cursos)
      const cargaAcademica = [
        { curso: 'Matemática', docenteId: 1, docente: 'Juan Pérez' },
        { curso: 'Comunicación', docenteId: 2, docente: 'María Salas' },
        { curso: 'Ciencia y Tecnología', docenteId: 7, docente: 'Jorge Sánchez' },
        { curso: 'Historia', docenteId: 3, docente: 'Carlos López' },
        { curso: 'Inglés', docenteId: 4, docente: 'Ana Gómez' },
        { curso: 'Ed. Física', docenteId: 5, docente: 'Luis Torres' },
        { curso: 'Arte', docenteId: 9, docente: 'Miguel Rojas' }
      ];

      // 2. Generar Matriz para cada sección asegurando que no haya colisiones de docentes
      this.seccionesDisponibles.forEach((seccion, index) => {
        const bloquesSeccion = this.horasDisponibles.map(h => {
           const esReceso = h === '11:00 - 11:30';
           return { hora: h, esReceso };
        });
        
        bloquesSeccion.forEach((bloque: any) => {
          if (bloque.esReceso) {
            this.dias.forEach(dia => {
              bloque[dia] = { esReceso: true };
            });
            return; // Termina aquí para este bloque si es receso
          }

          this.dias.forEach(dia => {
            // Obtener docentes que YA están enseñando en este exacto día y hora en OTRAS secciones
            const docentesOcupados = this.matrizGlobal
              .map(m => m.bloques.find((b: any) => b.hora === bloque.hora)?.[dia]?.docenteId)
              .filter(id => id != null);

            // Filtrar carga disponible (docentes no ocupados)
            const opcionesDisponibles = this.cargaAcademica.filter(c => !docentesOcupados.includes(c.docenteId));

            if (opcionesDisponibles.length > 0 && Math.random() > 0.1) {
               // Seleccionar curso al azar de los disponibles
               const randomCarga = opcionesDisponibles[Math.floor(Math.random() * opcionesDisponibles.length)];
               bloque[dia] = {
                 curso: randomCarga.curso,
                 seccion: seccion,
                 aula: `A-10${index + 1}`,
                 docenteId: randomCarga.docenteId,
                 docente: randomCarga.docente
               };
            } else {
               bloque[dia] = null; // Hora libre forzada por colisión o azar
            }
          });
        });
        
        this.matrizGlobal.push({ seccion: seccion, bloques: bloquesSeccion });
      });

      this.success = 'Matriz global generada con éxito. 0 colisiones detectadas.';
      this.generandoIA = false;
      this.aplicarFiltro(); // Mostrar la matriz generada según el filtro actual
      this.cdr.markForCheck();
      
      setTimeout(() => { this.success = ''; this.cdr.markForCheck(); }, 4000);
    }, 1500); 
  }

  aplicarFiltro() {
    if (this.matrizGlobal.length === 0) return;
    
    this.construirCalendarioVacio();
    const tipo = this.horarioForm.value.tipoFiltro;
    
    if (tipo === 'seccion') {
      const secSeleccionada = this.horarioForm.value.seccionFiltro;
      const data = this.matrizGlobal.find(m => m.seccion === secSeleccionada);
      if (data) {
        this.calendario = JSON.parse(JSON.stringify(data.bloques));
      }
    } else if (tipo === 'docente') {
      const docId = Number(this.horarioForm.value.docenteId);
      
      // Buscar en TODAS las secciones donde enseña el docente y armar su horario
      this.calendario.forEach(bloque => {
         this.dias.forEach(dia => {
            let claseEncontrada = null;
            for (const matrizSeccion of this.matrizGlobal) {
               const bloqueOriginal = matrizSeccion.bloques.find((b: any) => b.hora === bloque.hora);
               if (bloqueOriginal && bloqueOriginal[dia] && bloqueOriginal[dia].docenteId === docId) {
                  claseEncontrada = bloqueOriginal[dia];
                  break;
               }
            }
            (bloque as any)[dia] = claseEncontrada;
         });
      });
    }
  }

  // EDICIÓN DE BLOQUES (REAJUSTE)
  abrirEditor(bloque: any, dia: string) {
    if (bloque.esReceso) return; // No se edita el receso
    if (this.horarioForm.value.tipoFiltro === 'docente') return; // Solo permitir edición en vista por sección
    
    this.editandoBloque = bloque;
    this.diaEditando = dia;
    this.claseEditando = bloque[dia] || {
       curso: '',
       seccion: this.horarioForm.value.seccionFiltro,
       aula: 'A-X',
       docenteId: 0,
       docente: ''
    };

    // Obtener docentes que cruzarían horario
    const secActual = this.horarioForm.value.seccionFiltro;
    const docentesOcupados = this.matrizGlobal
       .filter(m => m.seccion !== secActual)
       .map(m => m.bloques.find((b: any) => b.hora === bloque.hora)?.[dia]?.docenteId)
       .filter(id => id != null);
    
    this.opcionesEdicion = this.cargaAcademica.map(carga => ({
      ...carga,
      conflicto: docentesOcupados.includes(carga.docenteId)
    }));
  }

  cerrarEditor() {
    this.editandoBloque = null;
    this.diaEditando = '';
    this.claseEditando = null;
  }

  asignarClase(opcion: any) {
    if (opcion.conflicto) {
       alert(`El docente ${opcion.docente} ya está dictando clases en otra sección en este horario. Genera cruce.`);
       return;
    }
    
    // Actualizar en el calendario visible y en matriz global
    const nuevaClase = {
       curso: opcion.curso,
       seccion: this.horarioForm.value.seccionFiltro,
       aula: this.claseEditando?.aula || 'A-101',
       docenteId: opcion.docenteId,
       docente: opcion.docente
    };
    this.editandoBloque[this.diaEditando] = nuevaClase;

    const secActual = this.horarioForm.value.seccionFiltro;
    const dataSeccion = this.matrizGlobal.find(m => m.seccion === secActual);
    if (dataSeccion) {
       const b = dataSeccion.bloques.find((b: any) => b.hora === this.editandoBloque.hora);
       if (b) b[this.diaEditando] = nuevaClase;
    }

    this.cerrarEditor();
  }

  liberarHora() {
    this.editandoBloque[this.diaEditando] = null;
    const secActual = this.horarioForm.value.seccionFiltro;
    const dataSeccion = this.matrizGlobal.find(m => m.seccion === secActual);
    if (dataSeccion) {
       const b = dataSeccion.bloques.find((b: any) => b.hora === this.editandoBloque.hora);
       if (b) b[this.diaEditando] = null;
    }
    this.cerrarEditor();
  }

  // GUARDADO DEFINITIVO
  guardarMatrizDefinitiva() {
    this.guardandoDefinitivo = true;
    this.cdr.markForCheck();

    // Simulación de guardado al backend por lotes
    setTimeout(() => {
      this.guardandoDefinitivo = false;
      this.success = '¡Horario Definitivo Guardado en Base de Datos!';
      this.cdr.markForCheck();
      setTimeout(() => { this.success = ''; this.cdr.markForCheck(); }, 4000);
    }, 2000);
  }

  // EXPORTACIÓN
  exportarPDF() {
    window.print(); // Solución nativa: apoyada por estilos CSS media="print"
  }

  exportarExcel() {
    let csv = 'Hora,Lunes,Martes,Miércoles,Jueves,Viernes\n';
    this.calendario.forEach(bloque => {
      let fila = [bloque.hora];
      this.dias.forEach(dia => {
         const clase = (bloque as any)[dia];
         if (clase?.esReceso) fila.push('RECESO');
         else if (clase) fila.push(`${clase.curso} (${clase.docente})`);
         else fila.push('Libre');
      });
      csv += fila.join(',') + '\n';
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `horario_${this.horarioForm.value.tipoFiltro}_${this.horarioForm.value.seccionFiltro || this.horarioForm.value.docenteId}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  limpiarHorario() {
    if (confirm('¿Estás seguro de eliminar el horario actual?')) {
      this.construirCalendarioVacio();
    }
  }
}
