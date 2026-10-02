import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, map, skip } from 'rxjs';
import { CategoriaCotacao, Cotacao } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { diasRestantes } from '../../../core/utils/formatos';
import { ESPERA_DA_BUSCA_MS, ListaPaginada } from '../../../core/utils/lista-paginada';
import { CarregarMais } from '../../../ui/carregar-mais';
import { Icone } from '../../../ui/icone';
import { Painel } from '../../../ui/painel';
import { OpcaoSeletor, Seletor } from '../../../ui/seletor';

@Component({
  selector: 'app-mural-oportunidades',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, CarregarMais, Icone, Painel, Seletor],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mural-oportunidades.html',
  styleUrl: './mural-oportunidades.css',
})
export class MuralOportunidades implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);
  private readonly propostaService = inject(PropostaService);
  private readonly notificacao = inject(NotificacaoService);

  protected readonly categorias = toSignal(
    this.cotacaoService.categorias().pipe(
      map((lista): OpcaoSeletor<CategoriaCotacao | ''>[] => [
        { valor: '', rotulo: 'Todas as categorias' },
        ...lista.map((c) => ({ valor: c.codigo, rotulo: c.descricao })),
      ]),
    ),
    { initialValue: [{ valor: '', rotulo: 'Todas as categorias' }] as OpcaoSeletor<CategoriaCotacao | ''>[] },
  );
  protected readonly diasRestantes = diasRestantes;

  // A busca pode vir pela URL (ex.: atalho do painel inicial)
  protected readonly busca = signal(inject(ActivatedRoute).snapshot.queryParamMap.get('busca') ?? '');
  protected readonly categoria = signal<CategoriaCotacao | ''>('');
  protected readonly comFiltro = computed(() => !!this.busca().trim() || !!this.categoria());

  /** Cada item já traz a proposta que este fornecedor enviou (minhaProposta) */
  protected readonly lista = new ListaPaginada<Cotacao>((pagina, tamanho) =>
    this.cotacaoService.mural(pagina, { busca: this.busca(), categoria: this.categoria() || null }, tamanho),
  );
  protected readonly enviando = signal(false);

  /** Cotação para a qual o painel de proposta está aberto */
  protected readonly selecionada = signal<Cotacao | null>(null);
  protected readonly painelAberto = signal(false);
  protected readonly valor = signal<number | null>(null);
  protected readonly condicoes = signal('');

  constructor() {
    // Categoria muda na hora; o texto espera a pessoa parar de digitar
    toObservable(this.categoria)
      .pipe(skip(1), takeUntilDestroyed())
      .subscribe(() => this.lista.recomecar());
    toObservable(this.busca)
      .pipe(skip(1), debounceTime(ESPERA_DA_BUSCA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.lista.recomecar());
  }

  ngOnInit(): void {
    this.lista.recomecar();
  }

  /** Publicada nas últimas 48 horas. */
  ehNova(c: Cotacao): boolean {
    return Date.now() - new Date(c.dataCriacao).getTime() < 48 * 60 * 60 * 1000;
  }

  abrirProposta(c: Cotacao): void {
    this.selecionada.set(c);
    this.valor.set(null);
    this.condicoes.set('');
    this.painelAberto.set(true);
  }

  confirmarEnvio(): void {
    const c = this.selecionada();
    const valor = this.valor();
    if (!c) {
      return;
    }
    if (!valor || valor <= 0) {
      this.notificacao.erro('Informe o valor da proposta.');
      return;
    }

    this.enviando.set(true);
    this.propostaService
      .enviar({
        valor,
        descricao: this.condicoes().trim() || 'Sem condições adicionais.',
        cotacaoId: c.id,
      })
      .subscribe({
        next: (proposta) => {
          this.notificacao.sucesso(`Proposta enviada para ${c.empresaNome}.`);
          // A linha passa a mostrar "Você ofertou" sem buscar a lista de novo
          this.lista.itens.update((itens) =>
            (itens ?? []).map((item) =>
              item.id === c.id
                ? {
                    ...item,
                    quantidadePropostas: item.quantidadePropostas + 1,
                    minhaProposta: { id: proposta.id, valor: proposta.valor, status: proposta.status },
                  }
                : item,
            ),
          );
          this.painelAberto.set(false);
          this.enviando.set(false);
        },
        error: (e) => {
          this.notificacao.erro(mensagemDeErro(e));
          this.enviando.set(false);
        },
      });
  }
}
