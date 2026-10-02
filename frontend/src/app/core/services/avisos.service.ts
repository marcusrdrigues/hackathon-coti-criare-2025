import { DestroyRef, Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, Subscription, filter } from 'rxjs';
import { NaoLidas } from '../models';
import { NegociacaoService } from './negociacao.service';
import { NotificacaoService } from './notificacao.service';
import { TempoRealService } from './tempo-real.service';

/** O que chega na fila pessoal /user/queue/avisos (ver AvisoTempoReal.java). */
export interface AvisoTempoReal {
  tipo: 'MENSAGEM' | 'PROPOSTA_RECEBIDA' | 'NEGOCIACAO_INICIADA' | 'NEGOCIACAO_FINALIZADA' | 'NEGOCIACAO_CANCELADA';
  negociacaoId: string | null;
  cotacaoId: string | null;
  titulo: string;
  texto: string;
}

export const FILA_AVISOS = '/user/queue/avisos';

/** Junta várias leituras seguidas (mensagens chegando em sequência) num pedido só. */
const ESPERA_LEITURA_MS = 800;

/**
 * Mensagens não lidas e avisos ao vivo, em qualquer tela.
 *
 * - O total por negociação vem da API (/negociacoes/nao-lidas) e é mantido ao vivo pela fila de avisos
 * - A negociação aberta na tela, com a aba visível, não acumula: o que chega já é visto
 * - Cada aviso vira um toast com atalho e é repassado às telas (recebidos$) para se atualizarem
 */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  private readonly tempoReal = inject(TempoRealService);
  private readonly negociacaoService = inject(NegociacaoService);
  private readonly notificacao = inject(NotificacaoService);

  private readonly contagem = signal<Readonly<Partial<Record<string, number>>>>({});
  private readonly avisos = new Subject<AvisoTempoReal>();
  private readonly leituras = new Map<string, ReturnType<typeof setTimeout>>();

  private assinatura: Subscription | null = null;
  private aberta: string | null = null;
  private conexoes = 0;

  /** Não lidas por negociação (id → total) */
  readonly naoLidas = this.contagem.asReadonly();
  readonly total = computed(() => Object.values(this.contagem()).reduce<number>((soma, n) => soma + (n ?? 0), 0));
  /** Avisos recebidos, para as telas recarregarem o que mostram */
  readonly recebidos$: Observable<AvisoTempoReal> = this.avisos.asObservable();

  constructor() {
    // Depois de uma queda, o que chegou nesse meio-tempo só a API sabe
    effect(() => {
      if (this.tempoReal.conectado() && this.conexoes++ > 0) {
        untracked(() => this.sincronizar());
      }
    });

    if (typeof document !== 'undefined') {
      const aoMudarVisibilidade = () => {
        if (this.aberta && this.vendo(this.aberta) && this.contagem()[this.aberta]) {
          this.marcarComoLida(this.aberta);
        }
      };
      document.addEventListener('visibilitychange', aoMudarVisibilidade);
      inject(DestroyRef).onDestroy(() => document.removeEventListener('visibilitychange', aoMudarVisibilidade));
    }
  }

  iniciar(): void {
    if (this.assinatura) {
      return;
    }
    this.assinatura = this.tempoReal.assinar<AvisoTempoReal>(FILA_AVISOS).subscribe((aviso) => this.receber(aviso));
    this.sincronizar();
  }

  parar(): void {
    this.assinatura?.unsubscribe();
    this.assinatura = null;
    this.aberta = null;
    this.conexoes = 0;
    this.leituras.forEach((espera) => clearTimeout(espera));
    this.leituras.clear();
    this.contagem.set({});
  }

  /** Troca os totais pelos da API. A negociação aberta na tela não acumula. */
  aplicar(naoLidas: NaoLidas): void {
    const contagem: Record<string, number> = {};
    for (const [id, total] of Object.entries(naoLidas.porNegociacao)) {
      if (total > 0 && !(id === this.aberta && this.vendo(id))) {
        contagem[id] = total;
      }
    }
    this.contagem.set(contagem);
  }

  /** A tela da negociação abriu: o que já chegou está visto, e o que chegar também. */
  abrir(negociacaoId: string): void {
    this.aberta = negociacaoId;
    this.marcarComoLida(negociacaoId);
  }

  fechar(negociacaoId: string): void {
    if (this.aberta === negociacaoId) {
      this.aberta = null;
    }
  }

  /** Chegou mensagem por outro caminho (consulta periódica): conta como vista se a tela está à vista. */
  vistaNaTela(negociacaoId: string): void {
    if (this.vendo(negociacaoId)) {
      this.marcarComoLida(negociacaoId);
    }
  }

  marcarComoLida(negociacaoId: string): void {
    if (this.contagem()[negociacaoId]) {
      this.contagem.update((c) => {
        const resto = { ...c };
        delete resto[negociacaoId];
        return resto;
      });
    }
    clearTimeout(this.leituras.get(negociacaoId));
    this.leituras.set(
      negociacaoId,
      setTimeout(() => {
        this.leituras.delete(negociacaoId);
        // Se falhar, a contagem volta na próxima sincronização; não vale incomodar ninguém
        this.negociacaoService.marcarComoLida(negociacaoId).subscribe({ error: () => undefined });
      }, ESPERA_LEITURA_MS),
    );
  }

  private receber(aviso: AvisoTempoReal): void {
    const id = aviso.negociacaoId;
    const naTela = id !== null && this.vendo(id);

    if (aviso.tipo === 'MENSAGEM' && id) {
      if (naTela) {
        this.marcarComoLida(id);
      } else {
        this.contagem.update((c) => ({ ...c, [id]: (c[id] ?? 0) + 1 }));
      }
    }

    // Quem está com a negociação aberta já vê a mudança acontecer
    if (!naTela) {
      this.notificacao.aviso(aviso.titulo, aviso.texto, destinoDoAviso(aviso));
    }
    this.avisos.next(aviso);
  }

  /** Busca os totais na API (ao entrar, depois de uma queda da conexão ou quando uma tela pede). */
  sincronizar(): void {
    this.negociacaoService.naoLidas().subscribe({
      next: (naoLidas) => this.aplicar(naoLidas),
      error: () => undefined,
    });
  }

  private vendo(negociacaoId: string): boolean {
    return this.aberta === negociacaoId && (typeof document === 'undefined' || document.visibilityState !== 'hidden');
  }
}

/** Para onde o atalho do aviso leva. */
export function destinoDoAviso(aviso: AvisoTempoReal): string | null {
  if (aviso.negociacaoId) {
    return `/pages/negociacao/${aviso.negociacaoId}`;
  }
  return aviso.cotacaoId ? `/pages/detalhe-cotacao/${aviso.cotacaoId}` : null;
}

/** Negociação aberta, fechada ou encerrada pela empresa. */
export const mudouNegociacao = (aviso: AvisoTempoReal): boolean => aviso.tipo.startsWith('NEGOCIACAO_');

/** Proposta nova numa cotação da empresa. */
export const chegouProposta = (aviso: AvisoTempoReal): boolean => aviso.tipo === 'PROPOSTA_RECEBIDA';

/**
 * Para as telas: executa a ação quando chegar um aviso que interessa, enquanto a tela existir.
 * Chamar no construtor (ou na declaração de um campo) do componente.
 */
export function aoReceberAviso(interessa: (aviso: AvisoTempoReal) => boolean, acao: (aviso: AvisoTempoReal) => void): void {
  inject(AvisosService).recebidos$.pipe(filter(interessa), takeUntilDestroyed()).subscribe(acao);
}
