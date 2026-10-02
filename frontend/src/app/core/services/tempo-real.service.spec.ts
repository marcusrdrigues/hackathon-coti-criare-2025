import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { TempoRealService, expiraEm } from './tempo-real.service';

function jwt(corpo: object): string {
  const base64url = (texto: string) => btoa(texto).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url('{"alg":"HS256"}')}.${base64url(JSON.stringify(corpo))}.assinatura`;
}

describe('TempoRealService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
  });

  it('começa desligado e só liga quando a estrutura das telas internas pede', () => {
    const servico = TestBed.inject(TempoRealService);
    expect(servico.estado()).toBe('desligado');
    expect(servico.conectado()).toBe(false);
  });

  it('assinar sem conexão não falha e pode ser cancelado', () => {
    const servico = TestBed.inject(TempoRealService);
    const recebidos: unknown[] = [];
    const inscricao = servico.negociacao('n1').subscribe((e) => recebidos.push(e));
    expect(() => inscricao.unsubscribe()).not.toThrow();
    expect(recebidos).toEqual([]);
  });

  it('"digitando" sem conexão é ignorado em silêncio', () => {
    const servico = TestBed.inject(TempoRealService);
    expect(() => servico.avisarDigitando('n1')).not.toThrow();
  });
});

describe('expiraEm', () => {
  it('lê o vencimento do token em milissegundos', () => {
    expect(expiraEm(jwt({ sub: 'u1', exp: 1_900_000_000 }))).toBe(1_900_000_000_000);
  });

  it('token inválido conta como vencido', () => {
    expect(expiraEm('nao-e-um-jwt')).toBe(0);
    expect(expiraEm(jwt({ sub: 'u1' }))).toBe(0);
  });
});
