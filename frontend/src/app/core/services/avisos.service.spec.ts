import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { Negociacao } from '../models';
import { AvisoTempoReal, AvisosService, FILA_AVISOS, destinoDoAviso } from './avisos.service';
import { NegociacaoService } from './negociacao.service';
import { NotificacaoService } from './notificacao.service';
import { TempoRealService } from './tempo-real.service';

function aviso(parcial: Partial<AvisoTempoReal>): AvisoTempoReal {
  return { tipo: 'MENSAGEM', negociacaoId: 'n1', cotacaoId: 'c1', titulo: 'Hospital Santa Vida', texto: 'Olá', ...parcial };
}

function negociacao(id: string, naoLidas: number): Negociacao {
  return { id, naoLidas } as Negociacao;
}

describe('AvisosService', () => {
  let fila: Subject<AvisoTempoReal>;
  let destinoAssinado: string | null;
  let lidas: string[];
  let servidor: Negociacao[];

  beforeEach(() => {
    vi.useFakeTimers();
    fila = new Subject<AvisoTempoReal>();
    destinoAssinado = null;
    lidas = [];
    servidor = [negociacao('n1', 2), negociacao('n2', 0), negociacao('n3', 1)];

    TestBed.configureTestingModule({
      providers: [
        {
          provide: TempoRealService,
          useValue: {
            conectado: signal(false),
            assinar: (destino: string): Observable<AvisoTempoReal> => {
              destinoAssinado = destino;
              return fila;
            },
          },
        },
        {
          provide: NegociacaoService,
          useValue: {
            listarMinhas: () => of(servidor),
            marcarComoLida: (id: string) => {
              lidas.push(id);
              return of(undefined);
            },
          },
        },
      ],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('ao iniciar assina a fila pessoal e busca os totais na API', () => {
    const avisos = TestBed.inject(AvisosService);
    avisos.iniciar();

    expect(destinoAssinado).toBe(FILA_AVISOS);
    expect(avisos.naoLidas()).toEqual({ n1: 2, n3: 1 });
    expect(avisos.total()).toBe(3);
  });

  it('mensagem de outra negociação soma no contador e vira toast com atalho', () => {
    const avisos = TestBed.inject(AvisosService);
    const toasts = TestBed.inject(NotificacaoService);
    avisos.iniciar();

    fila.next(aviso({ negociacaoId: 'n2', texto: 'Nova oferta de R$ 9.000,00' }));

    expect(avisos.naoLidas()['n2']).toBe(1);
    expect(avisos.total()).toBe(4);
    expect(toasts.notificacoes().at(-1)).toMatchObject({
      tipo: 'aviso',
      titulo: 'Hospital Santa Vida',
      texto: 'Nova oferta de R$ 9.000,00',
      link: '/pages/negociacao/n2',
    });
  });

  it('a negociação aberta não acumula: zera ao abrir e marca como lida na API uma vez só', () => {
    const avisos = TestBed.inject(AvisosService);
    const toasts = TestBed.inject(NotificacaoService);
    avisos.iniciar();

    avisos.abrir('n1');
    fila.next(aviso({ negociacaoId: 'n1' }));
    fila.next(aviso({ negociacaoId: 'n1' }));

    expect(avisos.naoLidas()['n1']).toBeUndefined();
    expect(toasts.notificacoes()).toEqual([]);
    expect(lidas).toEqual([]);

    vi.advanceTimersByTime(1_000);
    expect(lidas).toEqual(['n1']);
  });

  it('depois de sair da negociação, as mensagens dela voltam a contar', () => {
    const avisos = TestBed.inject(AvisosService);
    avisos.iniciar();

    avisos.abrir('n1');
    avisos.fechar('n1');
    fila.next(aviso({ negociacaoId: 'n1' }));

    expect(avisos.naoLidas()['n1']).toBe(1);
  });

  it('proposta nova e mudança de negociação avisam sem mexer no contador e chegam às telas', () => {
    const avisos = TestBed.inject(AvisosService);
    const recebidos: AvisoTempoReal[] = [];
    avisos.recebidos$.subscribe((a) => recebidos.push(a));
    avisos.iniciar();

    fila.next(aviso({ tipo: 'PROPOSTA_RECEBIDA', negociacaoId: null }));
    fila.next(aviso({ tipo: 'NEGOCIACAO_FINALIZADA', negociacaoId: 'n2' }));

    expect(avisos.total()).toBe(3);
    expect(recebidos.map((a) => a.tipo)).toEqual(['PROPOSTA_RECEBIDA', 'NEGOCIACAO_FINALIZADA']);
  });

  it('ao sair da conta esquece tudo', () => {
    const avisos = TestBed.inject(AvisosService);
    avisos.iniciar();
    avisos.parar();

    expect(avisos.total()).toBe(0);
    fila.next(aviso({ negociacaoId: 'n2' }));
    expect(avisos.total()).toBe(0);
  });
});

describe('destinoDoAviso', () => {
  it('leva à negociação quando há uma, senão à cotação', () => {
    expect(destinoDoAviso(aviso({ negociacaoId: 'n9' }))).toBe('/pages/negociacao/n9');
    expect(destinoDoAviso(aviso({ tipo: 'PROPOSTA_RECEBIDA', negociacaoId: null, cotacaoId: 'c7' }))).toBe(
      '/pages/detalhe-cotacao/c7',
    );
    expect(destinoDoAviso(aviso({ negociacaoId: null, cotacaoId: null }))).toBeNull();
  });
});
