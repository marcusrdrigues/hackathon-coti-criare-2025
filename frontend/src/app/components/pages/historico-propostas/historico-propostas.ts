import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Proposta } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { Navbar } from '../../shared/navbar/navbar';

/** Propostas que já tiveram desfecho: vencidas ou perdidas. */
@Component({
  selector: 'app-historico-propostas',
  imports: [Navbar, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './historico-propostas.html',
  styleUrl: './historico-propostas.css',
})
export class HistoricoPropostas implements OnInit {
  private readonly propostaService = inject(PropostaService);
  private readonly auth = inject(AuthService);

  protected readonly propostas = signal<Proposta[] | null>(null);
  protected readonly erro = signal<string | null>(null);

  protected readonly historico = computed(() =>
    (this.propostas() ?? [])
      .filter((p) => p.negociacaoStatus === 'FINALIZADA' || p.status === 'RECUSADA')
      .sort((a, b) => (b.dataEnvio ?? '').localeCompare(a.dataEnvio ?? '')),
  );

  protected readonly totalGanho = computed(() =>
    this.historico()
      .filter((p) => this.ganhou(p))
      .reduce((soma, p) => soma + (p.valorFinal ?? 0), 0),
  );

  ngOnInit(): void {
    this.propostaService.listarPorFornecedor(this.auth.usuarioLogado.id).subscribe({
      next: (lista) => this.propostas.set(lista),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  ganhou(p: Proposta): boolean {
    return p.negociacaoStatus === 'FINALIZADA';
  }

  motivo(p: Proposta): string {
    if (this.ganhou(p)) return 'Negócio fechado';
    if (p.cotacaoStatus === 'CANCELADA') return 'Cotação cancelada pela empresa';
    if (p.negociacaoStatus === 'CANCELADA') return 'Negociação encerrada sem acordo';
    if (p.cotacaoStatus === 'FECHADA') return 'Empresa fechou com outro fornecedor';
    return 'Proposta recusada';
  }
}
