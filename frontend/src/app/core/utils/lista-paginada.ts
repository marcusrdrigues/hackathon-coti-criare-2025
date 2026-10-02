import { computed, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { Pagina } from '../models';
import { mensagemDeErro } from './erros';

/** Itens por página nas telas. Duas páginas cabem numa requisição (a API aceita até 50; ver ADR 0020). */
export const TAMANHO_DA_PAGINA = 25;

/** Espera a pessoa parar de digitar antes de buscar na API. */
export const ESPERA_DA_BUSCA_MS = 300;

/** Uma recarga traz no máximo duas páginas numa requisição só. */
const PAGINAS_NUMA_RECARGA = 2;

/**
 * Estado de uma lista paginada da API para as telas: os itens já carregados, o total,
 * "carregando" e os erros. Trocar o filtro recomeça do zero; "Carregar mais" junta a
 * próxima página. Uma resposta antiga nunca sobrescreve a de um filtro mais novo, e um
 * item que já está na tela não aparece duas vezes (a lista pode mudar entre uma página e outra).
 *
 * Uso: `lista = new ListaPaginada((pagina, tamanho) => this.servico.minhas(pagina, this.filtro(), tamanho))`
 */
export class ListaPaginada<T extends { id: string }> {
  readonly itens = signal<T[] | null>(null);
  readonly total = signal(0);
  readonly carregando = signal(false);
  /** A primeira página não veio: a tela mostra o erro no lugar da lista */
  readonly erro = signal<string | null>(null);
  /** Uma página seguinte ou uma atualização falhou: a lista continua na tela, com o aviso */
  readonly falhaAoAtualizar = signal<string | null>(null);
  readonly temMais = computed(() => (this.itens()?.length ?? 0) < this.total());

  private proxima = 0;
  private pedido?: Subscription;

  constructor(private readonly buscar: (pagina: number, tamanho: number) => Observable<Pagina<T>>) {}

  /** Volta para a primeira página (por exemplo, quando o filtro muda). */
  recomecar(): void {
    this.itens.set(null);
    this.proxima = 0;
    this.carregar(0, TAMANHO_DA_PAGINA, false);
  }

  /** Junta a próxima página aos itens que já estão na tela. */
  carregarMais(): void {
    if (this.carregando() || !this.temMais()) {
      return;
    }
    this.carregar(this.proxima, TAMANHO_DA_PAGINA, true);
  }

  /**
   * Atualiza o que já está na tela sem piscar o "carregando" (depois de uma ação ou de um
   * aviso ao vivo): busca de novo, numa requisição só, as páginas que já estavam carregadas
   * (até duas). O tamanho é sempre um múltiplo da página, para o "Carregar mais" seguir dali.
   */
  recarregar(): void {
    const paginas = Math.ceil((this.itens()?.length ?? 0) / TAMANHO_DA_PAGINA);
    const tamanho = Math.min(PAGINAS_NUMA_RECARGA, Math.max(1, paginas)) * TAMANHO_DA_PAGINA;
    this.carregar(0, tamanho, false);
  }

  /**
   * Tira um item da tela na hora (o total acompanha) e depois realinha com a API: sem isso,
   * a próxima página começaria um item adiante e pularia um.
   */
  remover(manter: (item: T) => boolean): void {
    const antes = this.itens() ?? [];
    const depois = antes.filter(manter);
    this.itens.set(depois);
    this.total.update((total) => Math.max(0, total - (antes.length - depois.length)));
    this.recarregar();
  }

  private carregar(pagina: number, tamanho: number, juntar: boolean): void {
    this.pedido?.unsubscribe();
    this.carregando.set(true);
    this.pedido = this.buscar(pagina, tamanho).subscribe({
      next: (resposta) => {
        this.itens.update((atuais) => (juntar ? juntarSemRepetir(atuais ?? [], resposta.content) : resposta.content));
        this.total.set(resposta.page.totalElements);
        // Depois de uma recarga com duas páginas numa só, a próxima é a terceira
        this.proxima = juntar ? pagina + 1 : tamanho / TAMANHO_DA_PAGINA;
        this.erro.set(null);
        this.falhaAoAtualizar.set(null);
        this.carregando.set(false);
      },
      error: (e) => {
        if (this.itens() === null) {
          this.erro.set(mensagemDeErro(e));
        } else {
          this.falhaAoAtualizar.set(mensagemDeErro(e));
        }
        this.carregando.set(false);
      },
    });
  }
}

/** Uma cotação nova no topo empurra a lista: o último item de uma página volta na seguinte. */
function juntarSemRepetir<T extends { id: string }>(atuais: T[], novos: T[]): T[] {
  const vistos = new Set(atuais.map((item) => item.id));
  return [...atuais, ...novos.filter((item) => !vistos.has(item.id))];
}
