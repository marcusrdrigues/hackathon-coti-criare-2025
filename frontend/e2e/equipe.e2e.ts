import { expect, test } from '@playwright/test';
import { entrarComoDemo, sufixo } from './apoio';

/**
 * A equipe de ponta a ponta: a proprietária convida, a pessoa abre o link num outro
 * navegador, define a senha e já trabalha pela empresa; removida, ela perde o acesso.
 */
test('proprietária convida, a pessoa entra na equipe e depois é removida', async ({
  browser,
  baseURL,
}) => {
  const id = sufixo();
  const nome = `Pessoa E2E ${id}`;
  const email = `equipe-${id}@teste.com`;
  const proprietaria = await (await browser.newContext({ baseURL })).newPage();
  const convidada = await (await browser.newContext({ baseURL })).newPage();
  let link = '';

  await test.step('a proprietária cria o convite e copia o link', async () => {
    await entrarComoDemo(proprietaria, 'empresa');
    await proprietaria
      .getByRole('navigation', { name: 'Navegação principal' })
      .getByRole('link', { name: 'Equipe' })
      .click();
    await expect(proprietaria.getByRole('heading', { name: 'Equipe', level: 1 })).toBeVisible();

    await proprietaria.getByRole('button', { name: 'Convidar pessoa' }).click();
    await proprietaria.getByLabel('Nome').fill(nome);
    await proprietaria.getByLabel('E-mail').fill(email);
    await proprietaria.getByRole('button', { name: 'Criar convite' }).click();

    const campoLink = proprietaria.getByLabel('Link do convite');
    await expect(campoLink).toHaveValue(/\/convite#/);
    link = await campoLink.inputValue();
    await proprietaria.getByRole('button', { name: 'Fechar' }).click();
    await expect(proprietaria.getByRole('listitem').filter({ hasText: email })).toBeVisible();
  });

  await test.step('a pessoa abre o link, define a senha e já entra na empresa', async () => {
    await convidada.goto(link);
    await expect(convidada.getByRole('heading', { name: 'Entrar na equipe' })).toBeVisible();
    await expect(convidada.locator('.convite-origem')).toContainText('Criare Consulting');
    // O token sai da barra de endereço assim que é lido
    await expect(convidada).toHaveURL(/\/convite$/);

    await convidada.getByLabel('Crie uma senha').fill('senhaE2E123');
    await convidada.getByRole('button', { name: 'Entrar na equipe' }).click();
    await expect(convidada).toHaveURL(/\/pages\/dashboard$/);

    await convidada.goto('/pages/consultar-cotacao');
    await expect(convidada.getByRole('heading', { name: 'Cotações', level: 1 })).toBeVisible();
  });

  await test.step('o mesmo link não serve de novo', async () => {
    const outra = await (await browser.newContext({ baseURL })).newPage();
    await outra.goto(link);
    await expect(outra.getByRole('heading', { name: 'Convite indisponível' })).toBeVisible();
    await expect(outra.getByRole('alert')).toContainText('já foi usado');
  });

  await test.step('removida pela proprietária, a pessoa perde o acesso', async () => {
    await proprietaria.reload();
    const linha = proprietaria.getByRole('listitem').filter({ hasText: nome });
    await linha.getByRole('button', { name: `Remover ${nome}` }).click();
    await proprietaria.getByRole('alertdialog').getByRole('button', { name: 'Remover' }).click();
    await expect(linha).toHaveCount(0);

    // A sessão dela não renova mais: ao recarregar, volta para o login
    await convidada.reload();
    await expect(convidada).toHaveURL(/\/pages\/login/);
  });
});
