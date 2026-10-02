import { HttpErrorResponse } from '@angular/common/http';

/** Corpo de erro da API (GlobalExceptionHandler e respostas do Spring Security). */
interface CorpoDeErro {
  message?: string;
  errors?: Record<string, string>;
  traceId?: string | null;
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
  const corpo = erro.error as CorpoDeErro | null;
  if (erro.status >= 500) {
    const codigo = corpo?.traceId ?? erro.headers?.get('X-Trace-Id');
    return codigo ? `${ERRO_INESPERADO} Código para o suporte: ${codigo.slice(0, 8)}.` : ERRO_INESPERADO;
  }
  if (corpo?.errors && Object.keys(corpo.errors).length > 0) {
    return Object.values(corpo.errors).join(' • ');
  }
  return corpo?.message ?? ERRO_INESPERADO;
}
