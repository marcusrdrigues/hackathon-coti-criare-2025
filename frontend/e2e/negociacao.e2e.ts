import { expect, test } from '@playwright/test';
import { entrarComoDemo, sufixo } from './apoio';

/**
 * O fluxo principal com as duas partes ao mesmo tempo, cada uma no seu
 * navegador: a empresa publica, o fornecedor propõe, as duas negociam e a
 * empresa fecha o negócio.
 */
test('empresa e fornecedor negociam até fechar o negócio', async ({ browser, baseURL }) => {
  const titulo = `Cotação E2E ${sufixo()}`;
  const empresa = await (await browser.newContext({ baseURL })).newPage();
  const fornecedor = await (await browser.newContext({ baseURL })).newPage();
  // Confirmações (iniciar negociação, fechar negócio) são aceitas
  empresa.on('dialog', (dialogo) => dialogo.accept());

  await test.step('empresa publica a cotação', async () => {
    await entrarComoDemo(empresa, 'empresa');
    await empresa.goto('/pages/cadastro-cotacao');
    await empresa.getByLabel('Título do serviço/produto').fill(titulo);
    await empresa.getByLabel('Requisitos detalhados').fill('20 cadeiras ergonômicas com regulagem de altura e apoio lombar.');
    await empresa.getByLabel('Orçamento estimado').fill('12000');
    await empresa.getByRole('button', { name: 'Publicar Cotação' }).click();
    await expect(empresa).toHaveURL(/\/pages\/detalhe-cotacao\//);
    await expect(empresa.getByRole('heading', { name: titulo })).toBeVisible();
  });

  await test.step('fornecedor encontra no mural e envia a proposta', async () => {
    await entrarComoDemo(fornecedor, 'fornecedor');
    await fornecedor.goto('/pages/mural-oportunidades');
    await fornecedor.getByRole('searchbox', { name: 'Buscar' }).fill(titulo);

    const cartao = fornecedor.locator('.card').filter({ hasText: titulo });
    await cartao.getByRole('button', { name: 'Enviar Proposta' }).click();
    await cartao.getByLabel('Condições da proposta').fill('Entrega em 10 dias, frete incluso.');
    await cartao.getByLabel('Valor total (R$)').fill('11500');
    await cartao.getByRole('button', { name: 'Confirmar' }).click();
    await expect(cartao.getByText('Você ofertou')).toBeVisible();
  });

  await test.step('empresa abre a negociação e faz uma contraproposta', async () => {
    await empresa.reload();
    const linha = empresa.getByRole('row').filter({ hasText: 'Tech Soluções' });
    await linha.getByRole('button', { name: 'Negociar' }).click();
    await expect(empresa).toHaveURL(/\/pages\/negociacao\//);

    await empresa.getByLabel('Mensagem').fill('Fechamos em 10.800 com o mesmo prazo?');
    await empresa.getByLabel('Valor da contraproposta (opcional)').fill('10800');
    await empresa.getByRole('button', { name: 'Enviar', exact: true }).click();
    await expect(empresa.getByText('Fechamos em 10.800 com o mesmo prazo?')).toBeVisible();
  });

  await test.step('fornecedor aceita a oferta', async () => {
    await fornecedor.goto(empresa.url().replace(/^https?:\/\/[^/]+/, ''));
    await fornecedor.getByRole('button', { name: /^Aceitar R\$\s?10\.800,00$/ }).click();
    await expect(fornecedor.getByText('Aceito a sua oferta')).toBeVisible();
  });

  await test.step('empresa fecha o negócio', async () => {
    await empresa.reload();
    await empresa.getByRole('button', { name: /^Fechar por R\$\s?10\.800,00$/ }).click();
    await expect(empresa.getByText(/Negócio fechado em/)).toBeVisible();
  });
});
