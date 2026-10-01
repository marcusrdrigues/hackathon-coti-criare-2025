import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Cotacao, Proposta } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';
import { mensagemDeErro } from '../../../core/utils/erros';
import { InfoStatus, STATUS_COTACAO, STATUS_NEGOCIACAO, STATUS_PROPOSTA, iniciais } from '../../../core/utils/formatos';
import { ConfirmacaoService } from '../../../ui/confirmacao';
import { Icone } from '../../../ui/icone';
import { Menu, MenuItem } from '../../../ui/menu';
import { Status } from '../../../ui/status';

@Component({
  selector: 'app-detalhe-cotacao',
  imports: [RouterLink, CurrencyPipe, DatePipe, CnpjPipe, Icone, Menu, MenuItem, Status],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detalhe-cotacao.html',
  styleUrl: './detalhe-cotacao.css',
})
export class DetalheCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);
  private readonly propostaService = inject(PropostaService);
  private readonly negociacaoService = inject(NegociacaoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly confirmacao = inject(ConfirmacaoService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly statusCotacao = STATUS_COTACAO;
  protected readonly iniciais = iniciais;
  protected readonly cotacao = signal<Cotacao | null>(null);
  protected readonly propostas = signal<Proposta[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly processando = signal<string | null>(null);

  /** Da mais barata para a mais cara: a comparação que importa para quem compra */
  protected readonly ordenadas = computed(() => [...this.propostas()].sort((a, b) => a.valor - b.valor));

  protected readonly menorValor = computed(() => {
    const validas = this.propostas().filter((p) => p.status !== 'RECUSADA');
    return validas.length > 1 ? Math.min(...validas.map((p) => p.valor)) : null;
  });

  protected readonly negociacaoAtiva = computed(() => this.propostas().find((p) => p.negociacaoStatus === 'EM_ANDAMENTO') ?? null);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    forkJoin({
      cotacao: this.cotacaoService.buscar(this.id()),
      propostas: this.propostaService.listarPorCotacao(this.id()),
    }).subscribe({
      next: ({ cotacao, propostas }) => {
        this.cotacao.set(cotacao);
        this.propostas.set(propostas);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  /** Status exibido: o da negociação, quando existe, é mais informativo. */
  statusDa(p: Proposta): InfoStatus {
    return p.negociacaoStatus ? STATUS_NEGOCIACAO[p.negociacaoStatus] : STATUS_PROPOSTA[p.status];
  }

  podeNegociar(p: Proposta): boolean {
    return this.cotacao()?.status === 'ABERTA' && (p.status === 'ENVIADA' || p.status === 'EM_ANALISE');
  }

  async negociar(p: Proposta): Promise<void> {
    const confirmou = await this.confirmacao.confirmar({
      titulo: `Negociar com ${p.fornecedorNome}?`,
      mensagem: 'Enquanto a negociação durar, as outras propostas desta cotação ficam em espera.',
      confirmar: 'Negociar',
    });
    if (!confirmou) {
      return;
    }
    this.processando.set(p.id);
    this.negociacaoService.iniciar(p.id).subscribe({
      next: (negociacao) => this.router.navigate(['/pages/negociacao', negociacao.id]),
      error: (e) => {
        this.notificacao.erro(mensagemDeErro(e));
        this.processando.set(null);
      },
    });
  }

  async recusar(p: Proposta): Promise<void> {
    const confirmou = await this.confirmacao.confirmar({
      titulo: `Recusar a proposta de ${p.fornecedorNome}?`,
      mensagem: 'O fornecedor verá a proposta como recusada. Não dá para desfazer.',
      confirmar: 'Recusar',
      destrutivo: true,
    });
    if (!confirmou) {
      return;
    }
    this.processando.set(p.id);
    this.propostaService.recusar(p.id).subscribe({
      next: () => {
        this.notificacao.info('Proposta recusada.');
        this.processando.set(null);
        this.carregar();
      },
      error: (e) => {
        this.notificacao.erro(mensagemDeErro(e));
        this.processando.set(null);
      },
    });
  }

  async cancelarCotacao(): Promise<void> {
    const c = this.cotacao();
    if (!c) {
      return;
    }
    const confirmou = await this.confirmacao.confirmar({
      titulo: 'Cancelar esta cotação?',
      mensagem: 'Ela sai do mural e as propostas pendentes são recusadas.',
      confirmar: 'Cancelar cotação',
      cancelar: 'Voltar',
      destrutivo: true,
    });
    if (!confirmou) {
      return;
    }
    this.cotacaoService.cancelar(c.id).subscribe({
      next: () => {
        this.notificacao.info('Cotação cancelada.');
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }
}
