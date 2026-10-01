import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Cotacao, Proposta } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';
import { mensagemDeErro } from '../../../core/utils/erros';
import { STATUS_COTACAO, STATUS_NEGOCIACAO, STATUS_PROPOSTA, InfoStatus } from '../../../core/utils/formatos';
import { Navbar } from '../../shared/navbar/navbar';

@Component({
  selector: 'app-detalhe-cotacao',
  imports: [Navbar, RouterLink, CurrencyPipe, DatePipe, CnpjPipe],
  templateUrl: './detalhe-cotacao.html',
  styleUrl: './detalhe-cotacao.css',
})
export class DetalheCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);
  private readonly propostaService = inject(PropostaService);
  private readonly negociacaoService = inject(NegociacaoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly statusCotacao = STATUS_COTACAO;
  protected readonly cotacao = signal<Cotacao | null>(null);
  protected readonly propostas = signal<Proposta[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly processando = signal<string | null>(null);

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

  negociar(p: Proposta): void {
    if (p.negociacaoId) {
      this.router.navigate(['/pages/negociacao', p.negociacaoId]);
      return;
    }
    if (!confirm(`Iniciar a negociação com ${p.fornecedorNome}? Enquanto ela durar, as outras propostas ficam em espera.`)) {
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

  recusar(p: Proposta): void {
    if (!confirm(`Recusar a proposta de ${p.fornecedorNome}?`)) {
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

  cancelarCotacao(): void {
    const c = this.cotacao();
    if (!c || !confirm('Cancelar esta cotação? Ela sai do mural e as propostas pendentes serão recusadas.')) {
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
