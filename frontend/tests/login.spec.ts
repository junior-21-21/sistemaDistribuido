import { test, expect } from '@playwright/test';

test.describe('Autenticación y Login', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/estadisticas/dashboard', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalEstudiantes: 0,
        docentesActivos: 0,
        matriculasPendientes: 0,
        alertasDesercion: 0,
        mensaje: 'Prueba'
      })
    }));
    await page.route('**/api/actividades/recientes', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '[]'
    }));
  });

  test('debería mostrar mensaje de error con credenciales incorrectas', async ({ page }) => {
    await page.route('**/api/auth/login', route => route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'Credenciales inválidas' })
    }));
    await page.goto('/login');
    await page.fill('input#username', 'director');
    await page.fill('input#password', 'wrongpassword');
    await page.getByRole('button', { name: /Ingresar a la plataforma/i }).click();

    await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
  });

  test('debería hacer login correctamente y redireccionar al dashboard', async ({ page }) => {
    await page.route('**/api/auth/login', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'jwt-de-prueba',
        username: 'director',
        nombre: 'Director de Prueba',
        rol: 'DIRECTOR',
        email: 'director@prueba.local',
        debeCambiarClave: false
      })
    }));
    await page.goto('/login');
    await page.fill('input#username', 'director');
    await page.fill('input#password', 'ValidTest2026*');
    await page.getByRole('button', { name: /Ingresar a la plataforma/i }).click();

    await expect(page).toHaveURL(/.*dashboard/);
    await expect(page.locator('h1.page-title')).toContainText('Panel de');
  });

  test('debería conservar roles múltiples y habilitar sus módulos', async ({ page }) => {
    await page.route('**/api/auth/login', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'jwt-multirrol',
        username: 'coord.academica',
        nombre: 'Coordinadora Académica',
        rol: 'COORDINADOR_ACADEMICO',
        roles: ['COORDINADOR_ACADEMICO', 'DOCENTE'],
        email: 'coord@prueba.local',
        debeCambiarClave: false
      })
    }));
    await page.goto('/login');
    await page.fill('input#username', 'coord.academica');
    await page.fill('input#password', 'ValidTest2026*');
    await page.getByRole('button', { name: /Ingresar a la plataforma/i }).click();

    await expect(page).toHaveURL(/.*dashboard/);
    await expect(page.getByRole('link', { name: /Carga y horarios/i })).toBeVisible();
    const roles = await page.evaluate(() => JSON.parse(localStorage.getItem('pad_roles') || '[]'));
    expect(roles).toEqual(['COORDINADOR_ACADEMICO', 'DOCENTE']);
  });

  test('debería obligar el cambio de clave temporal', async ({ page }) => {
    await page.route('**/api/auth/login', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'jwt-temporal', username: 'admin', nombre: 'Administrador',
        rol: 'ADMIN_SISTEMA', roles: ['ADMIN_SISTEMA'], email: 'admin@prueba.local',
        debeCambiarClave: true
      })
    }));
    await page.goto('/login');
    await page.fill('input#username', 'admin');
    await page.fill('input#password', 'Temporal2026*');
    await page.getByRole('button', { name: /Ingresar a la plataforma/i }).click();

    await expect(page).toHaveURL(/.*cambiar-clave/);
  });
});
