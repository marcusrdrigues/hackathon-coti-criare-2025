import { expect, test } from '@playwright/test';
import { sufixo } from './apoio';

/**
 * "Esqueci minha senha" (spec 004). O e-mail não sai no ambiente de teste, então o link
 * é exercitado de duas formas: com a API de verdade para um link que não vale, e com a
 * resposta da API simulada para o caminho feliz, que os testes da API já cobrem por inteiro.
 */
test.describe('Redefinição de senha', () => {
  test('do login até a confirmação, sem dizer se o e-mail tem conta', async ({ page }) => {
    const email = `ninguem-${sufixo()}@teste.com`;
    await page.goto('/pages/login');
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('link', { name: 'Esqueci minha senha' }).click();

    await expect(page).toHaveURL(/\/pages\/esqueci-a-senha$/);
    // O e-mail digitado no login vem junto
    await expect(page.getByLabel('E-mail')).toHaveValue(email);
    await page.getByRole('button', { name: 'Enviar link' }).click();

    await expect(page.getByRole('heading', { name: 'Confira seu e-mail' })).toBeVisible();
    await expect(page.getByText(`Se houver uma conta com ${email}`)).toBeVisible();
    await page.getByRole('link', { name: 'Voltar para o login' }).click();
    await expect(page).toHaveURL(/\/pages\/login$/);
  });

  test('e-mail em branco é barrado na própria tela', async ({ page }) => {
    await page.goto('/pages/esqueci-a-senha');
    await page.getByRole('button', { name: 'Enviar link' }).click();
    await expect(page.getByText('Preencha o seu e-mail.')).toBeVisible();
  });

  test('link que não vale mostra o aviso e o atalho para pedir outro', async ({ page }) => {
    await page.goto('/redefinir-senha#token=um-link-que-nao-existe');

    await expect(page.getByRole('heading', { name: 'Link indisponível' })).toBeVisible();
    await expect(page.getByRole('alert')).toHaveText('Este link não vale mais. Peça um link novo.');
    // O token sai da barra de endereço assim que é lido
    expect(new URL(page.url()).hash).toBe('');
    await page.getByRole('link', { name: 'Pedir um link novo' }).click();
    await expect(page).toHaveURL(/\/pages\/esqueci-a-senha$/);
  });

  test('senha nova confere a repetição e volta para o login com o aviso', async ({ page }) => {
    let senhaEnviada: string | undefined;
    await page.route('**/api/v1/auth/redefinicao/consulta', (rota) => rota.fulfill({ status: 204 }));
    await page.route('**/api/v1/auth/redefinicao/confirmacao', (rota) => {
      senhaEnviada = (rota.request().postDataJSON() as { senha: string }).senha;
      return rota.fulfill({ status: 204 });
    });

    await page.goto('/redefinir-senha#token=um-link-valido');
    await page.getByLabel('Senha nova', { exact: true }).fill('senhaNova123');
    await page.getByLabel('Repita a senha nova').fill('outraCoisa1');
    await page.getByRole('button', { name: 'Salvar senha nova' }).click();
    await expect(page.getByText('As duas senhas não são iguais.')).toBeVisible();

    await page.getByLabel('Repita a senha nova').fill('senhaNova123');
    await page.getByRole('button', { name: 'Salvar senha nova' }).click();

    await expect(page).toHaveURL(/\/pages\/login$/);
    await expect(page.getByRole('status').filter({ hasText: 'Senha alterada' })).toBeVisible();
    expect(senhaEnviada).toBe('senhaNova123');
  });
});
