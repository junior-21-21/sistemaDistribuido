/**
 * Generador mínimo de PDF sin dependencias para reportes tabulares: cabecera con título, tarjetas de
 * resumen, tabla con colores por celda (se repite el encabezado en cada página), leyenda y pie con
 * numeración. Usa las fuentes estándar Helvetica con codificación WinAnsi (tildes y ñ incluidas).
 */

export interface ColumnaPdf {
  titulo: string;
  /** Segunda línea opcional del encabezado (por ejemplo, la hora del bloque). */
  detalle?: string;
  /** Peso relativo del ancho. */
  ancho: number;
  alinear?: 'izq' | 'centro';
}

export interface CeldaPdf {
  texto: string;
  /** Color de fondo en hexadecimal (#rrggbb). */
  fondo?: string;
  color?: string;
  negrita?: boolean;
}

export interface TablaPdf {
  titulo: string;
  subtitulo?: string;
  /** Pares etiqueta–valor bajo el título (Sección, Curso, Periodo…). */
  datos?: [string, string][];
  resumen?: { etiqueta: string; valor: string; color: string }[];
  columnas: ColumnaPdf[];
  filas: CeldaPdf[][];
  leyenda?: { texto: string; color: string }[];
  pie?: string;
}

const ANCHO = 842;
const ALTO = 595;
const MARGEN = 32;
const FILA = 15;
const VERDE = '#1f4d3a';

// Anchos AFM (milésimas de em) de los caracteres 32–126
const HELVETICA = [278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556,
  556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833,
  722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278,
  556, 556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584];
const HELVETICA_NEGRITA = [278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556,
  556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611, 975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611,
  833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556, 333, 556, 611, 556, 611, 556,
  333, 611, 611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584];

const WIN_ANSI: Record<string, number> = { '€': 0x80, '…': 0x85, '‘': 0x91, '’': 0x92, '“': 0x93, '”': 0x94, '•': 0x95, '–': 0x96, '—': 0x97 };

function anchoTexto(texto: string, tam: number, negrita = false): number {
  const tabla = negrita ? HELVETICA_NEGRITA : HELVETICA;
  let total = 0;
  for (const c of texto.normalize('NFD').replace(/[̀-ͯ]/g, '')) {
    const codigo = c.charCodeAt(0);
    total += codigo >= 32 && codigo <= 126 ? tabla[codigo - 32] : 556;
  }
  return (total * tam) / 1000;
}

/** Recorta el texto con «…» para que entre en el ancho disponible. */
function ajustar(texto: string, ancho: number, tam: number, negrita = false): string {
  if (anchoTexto(texto, tam, negrita) <= ancho) return texto;
  let recorte = texto;
  while (recorte.length > 1 && anchoTexto(recorte + '…', tam, negrita) > ancho) recorte = recorte.slice(0, -1);
  return recorte + '…';
}

function cadenaPdf(texto: string): string {
  let salida = '';
  for (const c of texto.normalize('NFC')) {
    const codigo = WIN_ANSI[c] ?? c.charCodeAt(0);
    const byte = codigo < 256 ? codigo : 63;
    if (byte === 40 || byte === 41 || byte === 92) salida += '\\' + String.fromCharCode(byte);
    else if (byte < 32 || byte > 126) salida += '\\' + byte.toString(8).padStart(3, '0');
    else salida += String.fromCharCode(byte);
  }
  return `(${salida})`;
}

function rgb(hex: string): string {
  const n = parseInt(hex.replace('#', ''), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(v => (v / 255).toFixed(3)).join(' ');
}

class Lienzo {
  readonly ops: string[] = [];
  rect(x: number, y: number, w: number, h: number, fondo: string, borde?: string): void {
    this.ops.push(`${rgb(fondo)} rg ${borde ? rgb(borde) + ' RG 0.5 w ' : ''}${x.toFixed(2)} ${(ALTO - y - h).toFixed(2)} ${w.toFixed(2)} ${h.toFixed(2)} re ${borde ? 'B' : 'f'}`);
  }
  linea(x1: number, y1: number, x2: number, y2: number, color: string, grosor = 0.5): void {
    this.ops.push(`${rgb(color)} RG ${grosor} w ${x1.toFixed(2)} ${(ALTO - y1).toFixed(2)} m ${x2.toFixed(2)} ${(ALTO - y2).toFixed(2)} l S`);
  }
  texto(x: number, y: number, texto: string, tam: number, color = '#1d2722', negrita = false): void {
    this.ops.push(`BT /${negrita ? 'F2' : 'F1'} ${tam} Tf ${rgb(color)} rg ${x.toFixed(2)} ${(ALTO - y).toFixed(2)} Td ${cadenaPdf(texto)} Tj ET`);
  }
  centrado(x: number, ancho: number, y: number, texto: string, tam: number, color?: string, negrita = false): void {
    const t = ajustar(texto, ancho - 4, tam, negrita);
    this.texto(x + (ancho - anchoTexto(t, tam, negrita)) / 2, y, t, tam, color, negrita);
  }
}

export function crearPdf(reporte: TablaPdf): Uint8Array {
  const paginas: Lienzo[] = [];
  const util = ANCHO - MARGEN * 2;
  const pesos = reporte.columnas.reduce((s, c) => s + c.ancho, 0);
  const anchos = reporte.columnas.map(c => (c.ancho / pesos) * util);
  const conDetalle = reporte.columnas.some(c => c.detalle);
  const altoEncabezado = conDetalle ? 24 : 17;

  const nuevaPagina = (primera: boolean): { lienzo: Lienzo; y: number } => {
    const lienzo = new Lienzo();
    paginas.push(lienzo);
    let y = MARGEN;
    if (primera) {
      lienzo.rect(MARGEN, y, util, 54, VERDE);
      lienzo.rect(MARGEN, y + 54, util, 3, '#a86b32');
      lienzo.texto(MARGEN + 16, y + 24, reporte.titulo, 17, '#f4f1e8', true);
      if (reporte.subtitulo) lienzo.texto(MARGEN + 16, y + 42, reporte.subtitulo, 9.5, '#cfe3d8');
      y += 70;
      if (reporte.datos?.length) {
        let x = MARGEN;
        for (const [etiqueta, valor] of reporte.datos) {
          lienzo.texto(x, y, etiqueta.toUpperCase(), 6.5, '#6b7a72', true);
          lienzo.texto(x, y + 11, valor, 9.5, '#1d2722', true);
          x += Math.max(110, anchoTexto(valor, 9.5, true) + 24);
        }
        y += 22;
      }
      if (reporte.resumen?.length) {
        const ancho = (util - (reporte.resumen.length - 1) * 8) / reporte.resumen.length;
        reporte.resumen.forEach((r, i) => {
          const x = MARGEN + i * (ancho + 8);
          lienzo.rect(x, y, ancho, 36, '#f6f8f6', '#dbe3de');
          lienzo.rect(x, y, 4, 36, r.color);
          lienzo.texto(x + 12, y + 17, r.valor, 13, r.color, true);
          lienzo.texto(x + 12, y + 29, r.etiqueta, 7.5, '#56645c');
        });
        y += 46;
      }
    } else {
      lienzo.texto(MARGEN, y + 10, reporte.titulo, 10, VERDE, true);
      y += 20;
    }
    // Encabezado de la tabla
    let x = MARGEN;
    lienzo.rect(MARGEN, y, util, altoEncabezado, VERDE);
    reporte.columnas.forEach((columna, i) => {
      if (columna.alinear === 'izq') lienzo.texto(x + 4, y + 11, ajustar(columna.titulo, anchos[i] - 6, 7.5, true), 7.5, '#ffffff', true);
      else lienzo.centrado(x, anchos[i], y + 11, columna.titulo, 7.5, '#ffffff', true);
      if (columna.detalle) lienzo.centrado(x, anchos[i], y + 20, columna.detalle, 6, '#cfe3d8');
      x += anchos[i];
    });
    return { lienzo, y: y + altoEncabezado };
  };

  let { lienzo, y } = nuevaPagina(true);
  const limite = ALTO - MARGEN - 40;
  reporte.filas.forEach((fila, f) => {
    if (y + FILA > limite) ({ lienzo, y } = nuevaPagina(false));
    if (f % 2 === 1) lienzo.rect(MARGEN, y, util, FILA, '#f6f8f6');
    let x = MARGEN;
    fila.forEach((celda, i) => {
      const ancho = anchos[i] ?? 0;
      if (celda.fondo) lienzo.rect(x + 1, y + 1.5, ancho - 2, FILA - 3, celda.fondo);
      if (reporte.columnas[i]?.alinear === 'izq') {
        lienzo.texto(x + 4, y + 10.5, ajustar(celda.texto, ancho - 6, 7.5, celda.negrita), 7.5, celda.color, celda.negrita);
      } else {
        lienzo.centrado(x, ancho, y + 10.5, celda.texto, 7.5, celda.color, celda.negrita);
      }
      x += ancho;
    });
    lienzo.linea(MARGEN, y + FILA, MARGEN + util, y + FILA, '#e3e9e5');
    y += FILA;
  });

  if (reporte.leyenda?.length) {
    if (y + 24 > limite) ({ lienzo, y } = nuevaPagina(false));
    let x = MARGEN;
    y += 14;
    for (const item of reporte.leyenda) {
      lienzo.rect(x, y - 7, 9, 9, item.color);
      lienzo.texto(x + 13, y, item.texto, 7.5, '#3d4a43');
      x += anchoTexto(item.texto, 7.5) + 30;
    }
  }

  paginas.forEach((pagina, i) => {
    pagina.linea(MARGEN, ALTO - MARGEN - 16, ANCHO - MARGEN, ALTO - MARGEN - 16, '#dbe3de');
    if (reporte.pie) pagina.texto(MARGEN, ALTO - MARGEN - 4, reporte.pie, 7, '#6b7a72');
    const numero = `Página ${i + 1} de ${paginas.length}`;
    pagina.texto(ANCHO - MARGEN - anchoTexto(numero, 7), ALTO - MARGEN - 4, numero, 7, '#6b7a72');
  });

  // Ensamblado de objetos: 1 catálogo, 2 páginas, 3 y 4 fuentes, luego (contenido, página) por cada hoja
  const objetos: string[] = [];
  const hijos = paginas.map((_, i) => `${6 + i * 2} 0 R`).join(' ');
  objetos[1] = '<< /Type /Catalog /Pages 2 0 R >>';
  objetos[2] = `<< /Type /Pages /Kids [${hijos}] /Count ${paginas.length} >>`;
  objetos[3] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>';
  objetos[4] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>';
  paginas.forEach((pagina, i) => {
    const contenido = pagina.ops.join('\n');
    objetos[5 + i * 2] = `<< /Length ${contenido.length} >>\nstream\n${contenido}\nendstream`;
    objetos[6 + i * 2] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${ANCHO} ${ALTO}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${5 + i * 2} 0 R >>`;
  });
  let pdf = '%PDF-1.4\n%\xe2\xe3\xcf\xd3\n';
  const posiciones: number[] = [];
  for (let n = 1; n < objetos.length; n++) {
    posiciones[n] = pdf.length;
    pdf += `${n} 0 obj\n${objetos[n]}\nendobj\n`;
  }
  const xref = pdf.length;
  pdf += `xref\n0 ${objetos.length}\n0000000000 65535 f \n`;
  for (let n = 1; n < objetos.length; n++) pdf += `${String(posiciones[n]).padStart(10, '0')} 00000 n \n`;
  pdf += `trailer\n<< /Size ${objetos.length} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;
  // Todo el contenido es ASCII o bytes Latin-1 escritos como caracteres sueltos
  const bytes = new Uint8Array(pdf.length);
  for (let i = 0; i < pdf.length; i++) bytes[i] = pdf.charCodeAt(i) & 0xff;
  return bytes;
}

export function descargarPdf(nombreArchivo: string, reporte: TablaPdf): void {
  const datos = crearPdf(reporte);
  const url = URL.createObjectURL(new Blob([datos.buffer as ArrayBuffer], { type: 'application/pdf' }));
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo.endsWith('.pdf') ? nombreArchivo : `${nombreArchivo}.pdf`;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
