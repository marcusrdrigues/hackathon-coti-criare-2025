const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/**
 * Como uma oferta nova se compara à que está na mesa, em palavras:
 * "R$ 1.000,00 abaixo da oferta na mesa (−1,8%)". Sem valor válido, null.
 */
export function compararOferta(nova: number | null, naMesa: number): string | null {
  if (nova === null || !Number.isFinite(nova) || nova <= 0) {
    return null;
  }
  const diferenca = nova - naMesa;
  if (Math.abs(diferenca) < 0.005) {
    return 'Igual à oferta na mesa.';
  }
  const percentual = Math.abs((diferenca / naMesa) * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 });
  const sentido = diferenca < 0 ? 'abaixo' : 'acima';
  const sinal = diferenca < 0 ? '−' : '+';
  return `${MOEDA.format(Math.abs(diferenca))} ${sentido} da oferta na mesa (${sinal}${percentual}%).`;
}

export function formatarMoeda(valor: number): string {
  return MOEDA.format(valor);
}
