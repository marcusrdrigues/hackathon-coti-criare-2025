import { expect, test } from '@playwright/test';

const EMAIL = process.env['SUPERADMIN_EMAIL'];
const SENHA = process.env['SUPERADMIN_SENHA'];

/**
 * O superadmin entra pela mesma tela de login e cai na área administrativa:
 * vê as organizações e não alcança as telas delas. Só roda quando a API foi
 * configurada com um superadmin (no CI, ver .github/workflows/ci.yml).
 */
test('superadmin vê as organizações e fica fora das telas delas', async ({ page }) => {
  test.skip(!EMAIL || !SENHA, 'API sem superadmin configurado');

  await page.goto('/pages/login');
  await page.getByLabel('E-mail').fill(EMAIL!);
  await page.getByLabel('Senha').fill(SENHA!);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();

  await expect(page).toHaveURL(/\/pages\/admin$/);
  await expect(page.getByRole('heading', { name: 'Organizações', level: 1 })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: 'Criare Consulting' })).toBeVisible();
  await expect(page.getByText(/de \d+ organizações/)).toBeVisible();

  // Ordenar por nome recarrega a lista do começo
  await page.getByText('Nome', { exact: true }).click();
  await expect(page.getByRole('listitem').first()).toBeVisible();

  // Sem organização: nada de equipe nem das telas de compras
  await expect(page.getByRole('link', { name: 'Equipe' })).toHaveCount(0);
  await page.goto('/pages/negociacoes');
  await expect(page).toHaveURL(/\/pages\/admin$/);
  await page.goto('/pages/dashboard');
  await expect(page).toHaveURL(/\/pages\/admin$/);
});
