import { StatusCotacao, StatusNegociacao, StatusProposta } from '../models';

/** Aplica a máscara 00.000.000/0000-00 enquanto o usuário digita. */
export function mascararCnpj(valor: string): string {
  const d = (valor ?? '').replace(/\D/g, '').slice(0, 14);
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2');
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
  classe: string;
}

export const STATUS_COTACAO: Record<StatusCotacao, InfoStatus> = {
  ABERTA: { texto: 'Aberta', classe: 'text-bg-success' },
  EM_NEGOCIACAO: { texto: 'Em negociação', classe: 'text-bg-warning' },
  FECHADA: { texto: 'Fechada', classe: 'text-bg-primary' },
  CANCELADA: { texto: 'Cancelada', classe: 'text-bg-secondary' },
};

export const STATUS_PROPOSTA: Record<StatusProposta, InfoStatus> = {
  ENVIADA: { texto: 'Nova proposta', classe: 'text-bg-info' },
  EM_ANALISE: { texto: 'Em análise', classe: 'text-bg-info' },
  ACEITA: { texto: 'Em negociação', classe: 'text-bg-warning' },
  RECUSADA: { texto: 'Recusada', classe: 'text-bg-secondary' },
};

export const STATUS_NEGOCIACAO: Record<StatusNegociacao, InfoStatus> = {
  EM_ANDAMENTO: { texto: 'Em andamento', classe: 'text-bg-warning' },
  FINALIZADA: { texto: 'Negócio fechado', classe: 'text-bg-success' },
  CANCELADA: { texto: 'Encerrada sem acordo', classe: 'text-bg-secondary' },
};
