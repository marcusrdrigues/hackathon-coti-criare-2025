import { expect, test } from '@playwright/test';
import { cnpjValido, entrarComoDemo, sufixo } from './apoio';

test.describe('Acesso', () => {
  test('rota protegida sem sessão volta para o login', async ({ page }) => {
    await page.goto('/pages/dashboard');
    await expect(page).toHaveURL(/\/pages\/login/);
    await expect(page.getByRole('heading', { name: 'Entrar no portal' })).toBeVisible();
  });

  test('login e cadastro cabem na tela, sem rolagem', async ({ page }) => {
    for (const rota of ['/pages/login', '/pages/cadastro']) {
      await page.goto(rota);
      await expect(page.locator('.acesso')).toBeVisible();
      const rola = await page.evaluate(() => document.documentElement.scrollHeight > window.innerHeight);
      expect(rola, `${rota} não deveria rolar`).toBe(false);
    }
  });

  test('senha errada mostra o aviso e não entra', async ({ page }) => {
    await page.goto('/pages/login');
    // E-mail sem conta: não consome as tentativas de login da conta de demonstração
    await page.getByLabel('E-mail').fill(`ninguem-${sufixo()}@teste.com`);
    await page.getByLabel('Senha').fill('senha-errada');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page).toHaveURL(/\/pages\/login/);
  });

  test('cadastro valida o CNPJ, cria a conta e já entra no portal', async ({ page }) => {
    const id = sufixo();
    await page.goto('/pages/cadastro');
    await page.locator('label[for="tipo-fornecedor"]').click();
    await page.getByLabel('Nome ou razão social').fill(`Fornecedor E2E ${id}`);

    const cnpj = page.getByLabel('CNPJ');
    await cnpj.fill('11111111111111');
    await cnpj.blur();
    await expect(page.getByText('Esse CNPJ não é válido')).toBeVisible();

    await cnpj.fill(cnpjValido());
    await page.getByLabel('E-mail').fill(`e2e-${id}@teste.com`);
    await page.getByLabel('Senha').fill('senhaE2E123');
    await page.getByRole('button', { name: 'Criar conta' }).click();

    await expect(page).toHaveURL(/\/pages\/dashboard-fornecedor$/);
  });

  test('sessão sobrevive ao recarregar a página', async ({ page }) => {
    await entrarComoDemo(page, 'empresa');
    await page.reload();
    await expect(page).toHaveURL(/\/pages\/dashboard$/);
    await expect(page.getByRole('heading', { name: 'Início', level: 1 })).toBeVisible();
  });

  test('sair encerra a sessão', async ({ page }) => {
    await entrarComoDemo(page, 'fornecedor');
    // Barra lateral no computador, topo no celular: o menu da conta é o mesmo
    await page.getByRole('button', { name: 'Menu do usuário' }).click();
    await page.getByRole('button', { name: 'Sair' }).click();
    await expect(page).toHaveURL(/\/pages\/login/);

    await page.goto('/pages/dashboard-fornecedor');
    await expect(page).toHaveURL(/\/pages\/login/);
  });
});

test.describe('Tema', () => {
  test('segue o sistema por padrão', async ({ browser, baseURL }) => {
    const contexto = await browser.newContext({ colorScheme: 'dark', baseURL });
    const pagina = await contexto.newPage();
    await pagina.goto('/pages/login');
    await expect(pagina.locator('html')).toHaveAttribute('data-theme', 'dark');
    await contexto.close();
  });

  test('escolha no seletor vale na hora e depois de recarregar', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'light' });
    await page.goto('/pages/login');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

    await page.getByTitle('Escuro').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    await page.getByTitle('Igual ao sistema').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  });
});
