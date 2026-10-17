import { Pipe, PipeTransform } from '@angular/core';

/** "hace un momento", "hace 5 min", "hace 2 h", "ayer"… */
@Pipe({ name: 'haceTiempo', standalone: true })
export class HaceTiempoPipe implements PipeTransform {
  transform(valor: string | Date | null | undefined, _tick?: unknown): string {
    if (!valor) return '';
    const fecha = typeof valor === 'string' ? new Date(valor) : valor;
    const segundos = Math.max(0, Math.round((Date.now() - fecha.getTime()) / 1000));
    if (segundos < 45) return 'hace un momento';
    const minutos = Math.round(segundos / 60);
    if (minutos < 60) return `hace ${minutos} min`;
    const horas = Math.round(minutos / 60);
    if (horas < 24) return `hace ${horas} h`;
    const dias = Math.round(horas / 24);
    if (dias === 1) return 'ayer';
    if (dias < 7) return `hace ${dias} días`;
    return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short' });
  }
}
