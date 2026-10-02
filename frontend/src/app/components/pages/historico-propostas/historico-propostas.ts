import { CurrencyPipe, DatePipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Proposta } from '../../../core/models';
import { DashboardService } from '../../../core/services/dashboard.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { ListaPaginada } from '../../../core/utils/lista-paginada';
import { CarregarMais } from '../../../ui/carregar-mais';
import { Icone } from '../../../ui/icone';
import { Status } from '../../../ui/status';
import { AbasPropostas } from '../../shared/abas-propostas';

/** Propostas que já tiveram desfecho: vencidas ou perdidas. */
@Component({
  selector: 'app-historico-propostas',
  imports: [RouterLink, CurrencyPipe, DatePipe, NgTemplateOutlet, CarregarMais, Icone, Status, AbasPropostas],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './historico-propostas.html',
  styleUrl: './historico-propostas.css',
})
export class HistoricoPropostas implements OnInit {
  private readonly propostaService = inject(PropostaService);
  private readonly dashboardService = inject(DashboardService);

  /** As mais recentes primeiro, na ordem da API */
  protected readonly lista = new ListaPaginada<Proposta>((pagina, tamanho) =>
    this.propostaService.minhas('HISTORICO', pagina, tamanho),
  );
  /** Soma de todos os negócios fechados (o painel já calcula, sem depender das páginas carregadas) */
  protected readonly totalGanho = signal<number | null>(null);

  ngOnInit(): void {
    this.lista.recomecar();
    this.dashboardService.fornecedor().subscribe({
      next: (numeros) => this.totalGanho.set(numeros.valorTotalGanho),
      error: () => undefined,
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
