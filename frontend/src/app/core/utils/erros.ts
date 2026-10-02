import { HttpErrorResponse } from '@angular/common/http';

/** Corpo de erro da API: Problem Details (RFC 9457), com o rastreio e os erros por campo (ver ADR 0020). */
interface Problema {
  title?: string;
  status?: number;
  detail?: string;
  traceId?: string | null;
  erros?: Record<string, string>;
}

const ERRO_INESPERADO = 'Ocorreu um erro inesperado. Tente novamente.';

/**
 * Transforma o erro da API em uma mensagem legível para o usuário. Em falhas do
 * servidor, acrescenta o começo do código de rastreio: é por ele que se acha a
 * requisição nos logs quando alguém pede ajuda.
 */
export function mensagemDeErro(erro: unknown): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return ERRO_INESPERADO;
  }
  if (erro.status === 0) {
    return 'Não foi possível conectar à API. Verifique se o back-end está rodando na porta 8085.';
  }
  const corpo = erro.error as Problema | null;
  if (erro.status >= 500) {
    const codigo = corpo?.traceId ?? erro.headers?.get('X-Trace-Id');
    return codigo ? `${ERRO_INESPERADO} Código para o suporte: ${codigo.slice(0, 8)}.` : ERRO_INESPERADO;
  }
  if (corpo?.erros && Object.keys(corpo.erros).length > 0) {
    return Object.values(corpo.erros).join(' • ');
  }
  return corpo?.detail ?? ERRO_INESPERADO;
}
