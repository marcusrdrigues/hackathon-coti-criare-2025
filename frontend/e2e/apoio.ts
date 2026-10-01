import { expect, Page } from '@playwright/test';

export const API = 'http://localhost:8085/api/v1';

/** Entra com uma das contas de demonstração pelo botão da tela de login. */
export async function entrarComoDemo(pagina: Page, perfil: 'empresa' | 'fornecedor'): Promise<void> {
  await pagina.goto('/pages/login');
  await pagina.getByRole('button', { name: new RegExp(`^Entrar como ${perfil}`) }).click();
  await expect(pagina).toHaveURL(perfil === 'empresa' ? /\/pages\/dashboard$/ : /\/pages\/dashboard-fornecedor$/);
}

/** Gera um CNPJ válido (dígitos verificadores corretos) e único o bastante para um teste. */
export function cnpjValido(): string {
  const base = Array.from({ length: 8 }, () => Math.floor(Math.random() * 10)).concat([0, 0, 0, 1]);
  const digito = (numeros: number[]) => {
    const pesos = numeros.length === 12 ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    const resto = numeros.reduce((soma, n, i) => soma + n * pesos[i], 0) % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  const d1 = digito(base);
  const d2 = digito([...base, d1]);
  return [...base, d1, d2].join('');
}

/** Sufixo para nomes e e-mails não colidirem entre execuções. */
export function sufixo(): string {
  return `${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;
}
