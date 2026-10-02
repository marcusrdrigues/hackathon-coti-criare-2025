import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { mensagemDeErro } from './erros';

function erro(status: number, corpo: unknown, cabecalhos?: Record<string, string>): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: corpo, headers: new HttpHeaders(cabecalhos ?? {}) });
}

describe('mensagemDeErro', () => {
  it('mostra a mensagem da API', () => {
    expect(mensagemDeErro(erro(400, { message: 'CNPJ inválido!' }))).toBe('CNPJ inválido!');
  });

  it('junta os erros de validação por campo', () => {
    const corpo = { message: 'Erro de validação', errors: { email: 'Email inválido', senha: 'Senha é obrigatória' } };
    expect(mensagemDeErro(erro(400, corpo))).toBe('Email inválido • Senha é obrigatória');
  });

  it('em falha do servidor, não mostra detalhes e dá o código de rastreio', () => {
    const corpo = { message: 'Erro interno do servidor.', traceId: '4bf92f3577b34da6a3ce929d0e0e4736' };
    expect(mensagemDeErro(erro(500, corpo))).toBe(
      'Ocorreu um erro inesperado. Tente novamente. Código para o suporte: 4bf92f35.',
    );
  });

  it('sem corpo, usa o código do cabeçalho', () => {
    expect(mensagemDeErro(erro(502, null, { 'X-Trace-Id': 'abcdef0123456789abcdef0123456789' }))).toContain(
      'abcdef01',
    );
  });

  it('sem conexão ou erro desconhecido', () => {
    expect(mensagemDeErro(erro(0, null))).toContain('Não foi possível conectar');
    expect(mensagemDeErro(new Error('x'))).toBe('Ocorreu um erro inesperado. Tente novamente.');
  });
});
