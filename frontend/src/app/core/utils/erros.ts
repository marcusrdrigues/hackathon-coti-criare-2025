import { HttpErrorResponse } from '@angular/common/http';

/**
 * Transforma o erro da API (ErrorResponse / ValidationErrorResponse do
 * GlobalExceptionHandler) em uma mensagem legível para o usuário.
 */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Não foi possível conectar à API. Verifique se o back-end está rodando na porta 8085.';
    }
    const corpo = erro.error as { message?: string; errors?: Record<string, string> } | null;
    if (corpo?.errors && Object.keys(corpo.errors).length > 0) {
      return Object.values(corpo.errors).join(' • ');
    }
    if (corpo?.message) {
      return corpo.message;
    }
  }
  return 'Ocorreu um erro inesperado. Tente novamente.';
}
