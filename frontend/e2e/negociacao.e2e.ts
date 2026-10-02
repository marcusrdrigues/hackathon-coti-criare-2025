import { Page, expect, test } from '@playwright/test';
import { entrarComoDemo, sufixo } from './apoio';

/** Link "Negociações" da barra lateral, onde aparece o total de não lidas. */
function linkNegociacoes(pagina: Page) {
  return pagina.getByRole('navigation', { name: 'Navegação principal' }).getByRole('link', { name: /^Negociações/ });
}

async function naoLidas(pagina: Page): Promise<number> {
  const contador = linkNegociacoes(pagina).locator('.contador');
  return (await contador.count()) ? Number(await contador.textContent()) : 0;
}

/** Aviso ao vivo (toast) com este texto. */
function aviso(pagina: Page, texto: string | RegExp) {
  return pagina.getByRole('status').filter({ hasText: texto });
}

/**
 * O fluxo principal com as duas partes ao mesmo tempo, cada uma no seu
 * navegador: a empresa publica, o fornecedor propõe, as duas negociam e a
 * empresa fecha o negócio. Ninguém recarrega a página: propostas, avisos,
 * contador de não lidas e mensagens chegam pelo tempo real (WebSocket).
 */
test('empresa e fornecedor negociam até fechar o negócio', async ({ browser, baseURL }) => {
  const titulo = `Cotação E2E ${sufixo()}`;
  const empresa = await (await browser.newContext({ baseURL })).newPage();
  const fornecedor = await (await browser.newContext({ baseURL })).newPage();

  await test.step('empresa publica a cotação', async () => {
    await entrarComoDemo(empresa, 'empresa');
    await empresa.goto('/pages/cadastro-cotacao');
    await empresa.getByLabel('Título').fill(titulo);
    await empresa.getByLabel('Requisitos').fill('20 cadeiras ergonômicas com regulagem de altura e apoio lombar.');
    await empresa.getByLabel('Orçamento estimado').fill('12000');
    await empresa.getByRole('button', { name: 'Publicar cotação' }).click();
    await expect(empresa).toHaveURL(/\/pages\/detalhe-cotacao\//);
    await expect(empresa.getByRole('heading', { name: titulo })).toBeVisible();
  });

  await test.step('fornecedor encontra no mural e envia a proposta pelo painel', async () => {
    await entrarComoDemo(fornecedor, 'fornecedor');
    await fornecedor.goto('/pages/mural-oportunidades');
    await fornecedor.getByRole('searchbox', { name: 'Buscar' }).fill(titulo);

    await fornecedor.getByRole('button', { name: `Enviar proposta para ${titulo}` }).click();
    const painel = fornecedor.getByRole('dialog', { name: 'Enviar proposta' });
    await painel.getByLabel('Valor total').fill('11500');
    await painel.getByLabel('Condições').fill('Entrega em 10 dias, frete incluso.');
    await painel.getByRole('button', { name: 'Enviar proposta' }).click();

    await expect(painel).toBeHidden();
    const linha = fornecedor.getByRole('listitem').filter({ hasText: titulo });
    await expect(linha.getByText('Você ofertou')).toBeVisible();
  });

  await test.step('empresa recebe o aviso e a proposta entra na lista sem recarregar', async () => {
    await expect(aviso(empresa, /Nova proposta de R\$\s?11\.500,00/)).toBeVisible();
    await expect(empresa.getByRole('listitem').filter({ hasText: 'Tech Soluções' })).toBeVisible();
  });

  let antes = 0;
  await test.step('empresa abre a negociação e o fornecedor é avisado', async () => {
    antes = await naoLidas(fornecedor);
    const proposta = empresa.getByRole('listitem').filter({ hasText: 'Tech Soluções' });
    await proposta.getByRole('button', { name: 'Negociar' }).click();
    // Confirmação no diálogo do próprio sistema, não no confirm() do navegador
    await empresa.getByRole('alertdialog').getByRole('button', { name: 'Negociar' }).click();
    await expect(empresa).toHaveURL(/\/pages\/negociacao\//);
    await expect(empresa.getByText('Ao vivo')).toBeVisible();
    await expect(aviso(fornecedor, 'Quer negociar a sua proposta')).toBeVisible();
  });

  await test.step('contraproposta soma no contador do fornecedor, que abre pelo aviso', async () => {
    await empresa.getByRole('button', { name: 'Fazer contraproposta' }).click();
    const oferta = empresa.getByRole('form', { name: 'Nova oferta' });
    // O campo de valor já abre focado
    await expect(oferta.getByLabel('Valor total')).toBeFocused();
    await oferta.getByLabel('Valor total').fill('10800');
    await expect(oferta.getByText(/abaixo da oferta na mesa/)).toBeVisible();
    await oferta.getByLabel('Mensagem (opcional)').fill('Fechamos em 10.800 com o mesmo prazo?');
    // O botão diz o que acontece, com o valor
    await oferta.getByRole('button', { name: /^Enviar oferta de R\$\s?10\.800,00$/ }).click();
    await expect(oferta).toBeHidden();
    await expect(empresa.getByText('Fechamos em 10.800 com o mesmo prazo?')).toBeVisible();

    const novaOferta = aviso(fornecedor, /Nova oferta de R\$\s?10\.800,00/);
    await expect(novaOferta).toBeVisible();
    const esperado = antes + 1;
    await expect(linkNegociacoes(fornecedor)).toHaveAccessibleName(new RegExp(`Negociações\\s*,\\s*${esperado} não lidas?`));

    await novaOferta.getByRole('link', { name: 'Abrir' }).click();
    await expect(fornecedor).toHaveURL(empresa.url());
    await expect(fornecedor.getByText('Fechamos em 10.800 com o mesmo prazo?')).toBeVisible();
    // Aberta, a conversa deixa de contar como não lida
    await expect.poll(() => naoLidas(fornecedor)).toBe(antes);
  });

  await test.step('fornecedor escreve e a empresa vê que ele está digitando', async () => {
    await expect(fornecedor.getByText('Ao vivo')).toBeVisible();
    await fornecedor.getByLabel('Mensagem', { exact: true }).pressSequentially('Deixa eu ver', { delay: 50 });
    await expect(empresa.getByText(/está digitando/)).toBeVisible();
  });

  await test.step('fornecedor aceita e a empresa recebe na hora', async () => {
    await fornecedor.getByRole('button', { name: /Aceitar R\$\s?10\.800,00/ }).click();
    await expect(fornecedor.getByText('Aceito a sua oferta')).toBeVisible();
    // Sem recarregar: chegou pelo WebSocket
    await expect(empresa.getByText('Aceito a sua oferta')).toBeVisible();
  });

  await test.step('empresa fecha o negócio e o fornecedor vê na hora', async () => {
    await empresa.getByRole('button', { name: /Fechar por R\$\s?10\.800,00/ }).click();
    await empresa.getByRole('alertdialog').getByRole('button', { name: 'Fechar negócio' }).click();
    await expect(empresa.getByText(/Negócio fechado em/)).toBeVisible();
    await expect(fornecedor.getByText(/Negócio fechado em/)).toBeVisible();
  });
});
