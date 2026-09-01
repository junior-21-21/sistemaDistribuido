import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';

// Apagado: El sistema ahora intentar conectarse s o s al backend de Java y a la Base de Datos.
const MOCK_MODE = false;

export const offlineInterceptor: HttpInterceptorFn = (req, next) => {
  if (!MOCK_MODE) {
    return next(req);
  }

  // Cortocircuito absoluto: Ni siquiera enviamos la peticion a Vite/Backend
  // Esto evita que Vite arroje ECONNREFUSED en la terminal.
  
  if (req.url.includes('/api/secciones')) {
    return of(new HttpResponse({
      status: 200,
      body: [
        { id: 1, nombre: 'A', gradoId: 1, gradoNombre: '1ro', nivelNombre: 'PRIMARIA', turno: 'MANANA' },
        { id: 2, nombre: 'B', gradoId: 1, gradoNombre: '1ro', nivelNombre: 'PRIMARIA', turno: 'TARDE' },
        { id: 3, nombre: 'A', gradoId: 7, gradoNombre: '1ro', nivelNombre: 'SECUNDARIA', turno: 'MANANA' }
      ]
    }));
  }

  if (req.url.includes('/api/estudiantes')) {
    return of(new HttpResponse({
      status: 200,
      body: [
        { id: 1, nombre: 'Rodrigo', apellido: 'Quispe Sullca', dni: '74521003', email: 'rodrigo@iep.edu.pe', telefono: '966123001', gradoId: 1, seccionId: 1, estado: 'ACTIVO' },
        { id: 2, nombre: 'Valentina', apellido: 'Huaman Flores', dni: '74521004', email: 'valentina@iep.edu.pe', telefono: '966123002', gradoId: 1, seccionId: 1, estado: 'ACTIVO' },
        { id: 3, nombre: 'Sebastian', apellido: 'Ccahuana Puma', dni: '74521005', email: 'sebastian@iep.edu.pe', telefono: '966123003', gradoId: 1, seccionId: 2, estado: 'ACTIVO' },
        { id: 4, nombre: 'Gabriela', apellido: 'Palomino Ayala', dni: '74521006', email: 'gabriela@iep.edu.pe', telefono: '966123004', gradoId: 2, seccionId: 1, estado: 'ACTIVO' },
        { id: 5, nombre: 'Diego', apellido: 'Condori Cardenas', dni: '74521007', email: 'diego@iep.edu.pe', telefono: '966123005', gradoId: 2, seccionId: 1, estado: 'ACTIVO' }
      ]
    }));
  }

  if (req.url.includes('/api/reportes-disciplinarios') || req.url.includes('/api/tickets') || req.url.includes('/api/docentes')) {
    return of(new HttpResponse({ status: 200, body: [] }));
  }

  if (req.method === 'GET' && req.url.includes('/api/')) {
    return of(new HttpResponse({ status: 200, body: [] }));
  }

  // Si no es ninguna de las anteriores, pasamos la peticion (aunque fallar en el proxy)
  return next(req);
};
