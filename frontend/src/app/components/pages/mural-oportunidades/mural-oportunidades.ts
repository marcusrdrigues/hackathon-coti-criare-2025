import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, map } from 'rxjs';
import { CategoriaCotacao, Cotacao, Proposta } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { diasRestantes } from '../../../core/utils/formatos';
import { Icone } from '../../../ui/icone';
import { Painel } from '../../../ui/painel';
import { OpcaoSeletor, Seletor } from '../../../ui/seletor';

@Component({
  selector: 'app-mural-oportunidades',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, Icone, Painel, Seletor],
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

  protected readonly oportunidades = signal<Cotacao[] | null>(null);
  /** cotacaoId -> proposta que este fornecedor já enviou */
  protected readonly minhasPropostas = signal<Map<string, Proposta>>(new Map());
  protected readonly erro = signal<string | null>(null);
  // A busca pode vir pela URL (ex.: atalho do painel inicial)
  protected readonly busca = signal(inject(ActivatedRoute).snapshot.queryParamMap.get('busca') ?? '');
  protected readonly categoria = signal<CategoriaCotacao | ''>('');
  protected readonly enviando = signal(false);

  /** Cotação para a qual o painel de proposta está aberto */
  protected readonly selecionada = signal<Cotacao | null>(null);
  protected readonly painelAberto = signal(false);
  protected readonly valor = signal<number | null>(null);
  protected readonly condicoes = signal('');

  protected readonly filtradas = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const categoria = this.categoria();
    return (this.oportunidades() ?? []).filter(
      (c) =>
        (!categoria || c.categoria === categoria) &&
        (!termo ||
          c.nomeServico.toLowerCase().includes(termo) ||
          c.requisitos.toLowerCase().includes(termo) ||
          c.empresaNome.toLowerCase().includes(termo)),
    );
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    forkJoin({
      abertas: this.cotacaoService.listarAbertas(),
      minhas: this.propostaService.listarMinhas(),
    }).subscribe({
      next: ({ abertas, minhas }) => {
        this.oportunidades.set(abertas);
        this.minhasPropostas.set(new Map(minhas.map((p) => [p.cotacaoId, p])));
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
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
          this.minhasPropostas.update((mapa) => new Map(mapa).set(c.id, proposta));
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
