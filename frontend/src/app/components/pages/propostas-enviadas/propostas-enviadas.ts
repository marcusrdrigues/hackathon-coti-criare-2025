import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Proposta } from '../../../core/models';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { InfoStatus } from '../../../core/utils/formatos';
import { Navbar } from '../../shared/navbar/navbar';

/** Propostas do fornecedor que ainda não tiveram desfecho. */
@Component({
  selector: 'app-propostas-enviadas',
  imports: [Navbar, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './propostas-enviadas.html',
  styleUrl: './propostas-enviadas.css',
})
export class PropostasEnviadas implements OnInit {
  private readonly propostaService = inject(PropostaService);
  private readonly notificacao = inject(NotificacaoService);

  protected readonly propostas = signal<Proposta[] | null>(null);
  protected readonly erro = signal<string | null>(null);

  protected readonly enviadas = computed(() =>
    (this.propostas() ?? [])
      .filter((p) => this.emAndamento(p))
      // Negociações ativas primeiro: são as que pedem resposta
      .sort((a, b) => Number(!!b.negociacaoId) - Number(!!a.negociacaoId)),
  );

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.propostaService.listarMinhas().subscribe({
      next: (lista) => this.propostas.set(lista),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  situacao(p: Proposta): InfoStatus {
    if (p.negociacaoStatus === 'EM_ANDAMENTO') {
      return { texto: 'Em negociação', classe: 'bg-warning text-dark' };
    }
    if (p.cotacaoStatus === 'EM_NEGOCIACAO') {
      return { texto: 'Empresa negociando com outro fornecedor', classe: 'bg-light text-dark border' };
    }
    return p.status === 'EM_ANALISE'
      ? { texto: 'Em análise', classe: 'bg-info text-dark' }
      : { texto: 'Aguardando análise', classe: 'bg-info text-dark' };
  }

  retirar(p: Proposta): void {
    if (!confirm(`Retirar sua proposta para "${p.cotacaoNome}"?`)) {
      return;
    }
    this.propostaService.retirar(p.id).subscribe({
      next: () => {
        this.notificacao.info('Proposta retirada.');
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }

  private emAndamento(p: Proposta): boolean {
    if (p.negociacaoStatus) {
      return p.negociacaoStatus === 'EM_ANDAMENTO';
    }
    return p.status === 'ENVIADA' || p.status === 'EM_ANALISE';
  }
}
