const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

async function generatePDF() {
    console.log('Iniciando generador de PDF con Playwright...');
    const htmlPath = path.resolve(__dirname, 'diseno_base_datos.html');
    const outputPath = path.resolve(__dirname, 'DISEÑO_BASE_DE_DATOS.pdf');
    
    // Launch browser (using installed Chrome executable for consistency)
    const browser = await chromium.launch({
        headless: true,
        executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
    });
    
    const page = await browser.newPage();
    
    // Set viewport
    await page.setViewportSize({ width: 1200, height: 1600 });
    
    console.log('Cargando HTML:', htmlPath);
    await page.goto('file:///' + htmlPath.replace(/\\/g, '/'), {
        waitUntil: 'networkidle'
    });
    
    // Wait for mermaid diagrams to be fully rendered (all .mermaid divs convert to svg)
    await page.waitForTimeout(3000);
    
    console.log('Generando PDF en:', outputPath);
    await page.pdf({
        path: outputPath,
        format: 'A4',
        printBackground: true,
        margin: {
            top: '12mm',
            right: '12mm',
            bottom: '12mm',
            left: '12mm'
        }
    });
    
    await browser.close();
    console.log('PDF generado exitosamente!');
}

generatePDF().catch(err => {
    console.error('Error generando PDF:', err);
    process.exit(1);
});
