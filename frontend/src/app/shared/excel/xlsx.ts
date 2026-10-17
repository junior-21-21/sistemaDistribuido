/**
 * Generador mínimo de archivos .xlsx (Office Open XML) sin dependencias: una hoja, estilos
 * básicos, celdas combinadas y paneles inmovilizados. El ZIP se escribe sin compresión.
 */

/** Estilos disponibles (índices de cellXfs en styles.xml). */
export enum EstiloXlsx {
  Normal = 0,
  Encabezado = 1,
  Texto = 2,
  NotaAD = 3,
  NotaA = 4,
  NotaB = 5,
  NotaC = 6,
  Numero = 7,
  Titulo = 8,
  Peso = 9,
  Subtitulo = 10
}

export interface CeldaXlsx {
  v: string | number | null;
  s?: EstiloXlsx;
}

export interface HojaXlsx {
  nombre: string;
  filas: CeldaXlsx[][];
  anchos?: number[];
  combinar?: string[];
  inmovilizar?: { columnas: number; filas: number };
}

const ESTILOS = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1"><numFmt numFmtId="164" formatCode="0.0"/></numFmts>
<fonts count="5">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="15"/><color rgb="FF1F4D3A"/><name val="Calibri"/></font>
<font><i/><sz val="10"/><color rgb="FF555555"/><name val="Calibri"/></font>
</fonts>
<fills count="8">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF1F4D3A"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFD1F2DC"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFDCE6FF"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFFF0C7"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFFDADA"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F2"/></patternFill></fill>
</fills>
<borders count="2">
<border><left/><right/><top/><bottom/><diagonal/></border>
<border><left style="thin"><color rgb="FFBFC7C2"/></left><right style="thin"><color rgb="FFBFC7C2"/></right><top style="thin"><color rgb="FFBFC7C2"/></top><bottom style="thin"><color rgb="FFBFC7C2"/></bottom><diagonal/></border>
</borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="11">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
<xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="2" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>`;

function columnaLetra(indice: number): string {
  let letra = '';
  for (let n = indice + 1; n > 0; n = Math.floor((n - 1) / 26)) {
    letra = String.fromCharCode(65 + ((n - 1) % 26)) + letra;
  }
  return letra;
}

function escaparXml(texto: string): string {
  return texto.replace(/[<>&"]/g, c => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;', '"': '&quot;' }[c]!))
    // Caracteres de control no permitidos en XML
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
}

function hojaXml(hoja: HojaXlsx): string {
  const filas = hoja.filas.map((fila, f) => {
    const celdas = fila.map((celda, c) => {
      const ref = `${columnaLetra(c)}${f + 1}`;
      const estilo = celda.s ? ` s="${celda.s}"` : '';
      if (celda.v === null || celda.v === '') return `<c r="${ref}"${estilo}/>`;
      if (typeof celda.v === 'number') return `<c r="${ref}"${estilo}><v>${celda.v}</v></c>`;
      return `<c r="${ref}"${estilo} t="inlineStr"><is><t xml:space="preserve">${escaparXml(celda.v)}</t></is></c>`;
    }).join('');
    return `<row r="${f + 1}">${celdas}</row>`;
  }).join('');
  const panel = hoja.inmovilizar
    ? `<sheetViews><sheetView workbookViewId="0"><pane xSplit="${hoja.inmovilizar.columnas}" ySplit="${hoja.inmovilizar.filas}" topLeftCell="${columnaLetra(hoja.inmovilizar.columnas)}${hoja.inmovilizar.filas + 1}" activePane="bottomRight" state="frozen"/></sheetView></sheetViews>`
    : '';
  const anchos = hoja.anchos?.length
    ? `<cols>${hoja.anchos.map((ancho, i) => `<col min="${i + 1}" max="${i + 1}" width="${ancho}" customWidth="1"/>`).join('')}</cols>`
    : '';
  const combinar = hoja.combinar?.length
    ? `<mergeCells count="${hoja.combinar.length}">${hoja.combinar.map(r => `<mergeCell ref="${r}"/>`).join('')}</mergeCells>`
    : '';
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">${panel}${anchos}<sheetData>${filas}</sheetData>${combinar}</worksheet>`;
}

const TABLA_CRC = (() => {
  const tabla = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    tabla[n] = c >>> 0;
  }
  return tabla;
})();

function crc32(datos: Uint8Array): number {
  let crc = 0xffffffff;
  for (const byte of datos) crc = TABLA_CRC[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

/** Empaqueta los archivos en un ZIP sin compresión (método «stored»). */
function zip(archivos: { nombre: string; contenido: string }[]): Uint8Array {
  const codificador = new TextEncoder();
  const partes: Uint8Array[] = [];
  const central: Uint8Array[] = [];
  let desplazamiento = 0;
  for (const archivo of archivos) {
    const nombre = codificador.encode(archivo.nombre);
    const datos = codificador.encode(archivo.contenido);
    const crc = crc32(datos);
    const local = new DataView(new ArrayBuffer(30));
    local.setUint32(0, 0x04034b50, true);
    local.setUint16(4, 20, true);
    local.setUint16(6, 0x0800, true); // nombres en UTF-8
    local.setUint32(14, crc, true);
    local.setUint32(18, datos.length, true);
    local.setUint32(22, datos.length, true);
    local.setUint16(26, nombre.length, true);
    partes.push(new Uint8Array(local.buffer), nombre, datos);

    const entrada = new DataView(new ArrayBuffer(46));
    entrada.setUint32(0, 0x02014b50, true);
    entrada.setUint16(4, 20, true);
    entrada.setUint16(6, 20, true);
    entrada.setUint16(8, 0x0800, true);
    entrada.setUint32(16, crc, true);
    entrada.setUint32(20, datos.length, true);
    entrada.setUint32(24, datos.length, true);
    entrada.setUint16(28, nombre.length, true);
    entrada.setUint32(42, desplazamiento, true);
    central.push(new Uint8Array(entrada.buffer), nombre);
    desplazamiento += 30 + nombre.length + datos.length;
  }
  const tamanoCentral = central.reduce((total, parte) => total + parte.length, 0);
  const fin = new DataView(new ArrayBuffer(22));
  fin.setUint32(0, 0x06054b50, true);
  fin.setUint16(8, archivos.length, true);
  fin.setUint16(10, archivos.length, true);
  fin.setUint32(12, tamanoCentral, true);
  fin.setUint32(16, desplazamiento, true);
  const todo = [...partes, ...central, new Uint8Array(fin.buffer)];
  const salida = new Uint8Array(todo.reduce((total, parte) => total + parte.length, 0));
  let posicion = 0;
  for (const parte of todo) {
    salida.set(parte, posicion);
    posicion += parte.length;
  }
  return salida;
}

export function crearXlsx(hoja: HojaXlsx): Uint8Array {
  const nombreHoja = escaparXml(hoja.nombre.replace(/[\\/?*[\]:]/g, ' ').slice(0, 31) || 'Hoja1');
  return zip([
    { nombre: '[Content_Types].xml', contenido: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>` },
    { nombre: '_rels/.rels', contenido: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>` },
    { nombre: 'xl/workbook.xml', contenido: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${nombreHoja}" sheetId="1" r:id="rId1"/></sheets></workbook>` },
    { nombre: 'xl/_rels/workbook.xml.rels', contenido: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>` },
    { nombre: 'xl/styles.xml', contenido: ESTILOS },
    { nombre: 'xl/worksheets/sheet1.xml', contenido: hojaXml(hoja) }
  ]);
}

export function descargarXlsx(nombreArchivo: string, hoja: HojaXlsx): void {
  const datos = crearXlsx(hoja);
  const url = URL.createObjectURL(new Blob([datos as BlobPart],
    { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(url);
}
