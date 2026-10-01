import { StatusCotacao, StatusNegociacao, StatusProposta } from '../models';
import { TomStatus } from '../../ui/status';

/** Aplica a máscara 00.000.000/0000-00 enquanto o usuário digita. */
export function mascararCnpj(valor: string): string {
  const d = (valor ?? '').replace(/\D/g, '').slice(0, 14);
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2');
}

/** Confere os dígitos verificadores do CNPJ (a mesma regra da API). */
export function cnpjValido(valor: string): boolean {
  const d = (valor ?? '').replace(/\D/g, '');
  if (d.length !== 14 || /^(\d)\1+$/.test(d)) {
    return false;
  }
  const digito = (base: string, pesos: number[]) => {
    const soma = pesos.reduce((total, peso, i) => total + Number(base[i]) * peso, 0);
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  const dv1 = digito(d, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const dv2 = digito(d, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  return Number(d[12]) === dv1 && Number(d[13]) === dv2;
}

/** Dias inteiros até a data informada (negativo se já passou). */
export function diasRestantes(data: string | null): number | null {
  if (!data) {
    return null;
  }
  const diff = new Date(data).getTime() - Date.now();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

export interface InfoStatus {
  texto: string;
  tom: TomStatus;
}

export const STATUS_COTACAO: Record<StatusCotacao, InfoStatus> = {
  ABERTA: { texto: 'Aberta', tom: 'sucesso' },
  EM_NEGOCIACAO: { texto: 'Em negociação', tom: 'atencao' },
  FECHADA: { texto: 'Fechada', tom: 'marca' },
  CANCELADA: { texto: 'Cancelada', tom: 'neutro' },
};

export const STATUS_PROPOSTA: Record<StatusProposta, InfoStatus> = {
  ENVIADA: { texto: 'Nova', tom: 'marca' },
  EM_ANALISE: { texto: 'Em análise', tom: 'neutro' },
  ACEITA: { texto: 'Em negociação', tom: 'atencao' },
  RECUSADA: { texto: 'Recusada', tom: 'neutro' },
};

export const STATUS_NEGOCIACAO: Record<StatusNegociacao, InfoStatus> = {
  EM_ANDAMENTO: { texto: 'Em andamento', tom: 'atencao' },
  FINALIZADA: { texto: 'Negócio fechado', tom: 'sucesso' },
  CANCELADA: { texto: 'Encerrada sem acordo', tom: 'neutro' },
};

/** Iniciais para o círculo de identificação (ex.: "Tech Soluções" → "TS"). */
export function iniciais(nome: string): string {
  const partes = (nome ?? '').trim().split(/\s+/).filter((p) => p.length > 2 || /^[A-ZÀ-Ú]/.test(p));
  return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase() || '?';
}
