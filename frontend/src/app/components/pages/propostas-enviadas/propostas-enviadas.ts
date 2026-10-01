import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Proposta } from '../../../core/models';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { InfoStatus, iniciais } from '../../../core/utils/formatos';
import { ConfirmacaoService } from '../../../ui/confirmacao';
import { Icone } from '../../../ui/icone';
import { Status } from '../../../ui/status';
import { AbasPropostas } from '../../shared/abas-propostas';

/** Propostas do fornecedor que ainda não tiveram desfecho. */
@Component({
  selector: 'app-propostas-enviadas',
  imports: [RouterLink, CurrencyPipe, DatePipe, Icone, Status, AbasPropostas],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './propostas-enviadas.html',
  styleUrl: './propostas-enviadas.css',
})
export class PropostasEnviadas implements OnInit {
  private readonly propostaService = inject(PropostaService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly confirmacao = inject(ConfirmacaoService);

  protected readonly iniciais = iniciais;

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
      return { texto: 'Em negociação', tom: 'atencao' };
    }
    if (p.cotacaoStatus === 'EM_NEGOCIACAO') {
      return { texto: 'Empresa negociando com outro fornecedor', tom: 'neutro' };
    }
    return p.status === 'EM_ANALISE' ? { texto: 'Em análise', tom: 'neutro' } : { texto: 'Aguardando análise', tom: 'neutro' };
  }

  async retirar(p: Proposta): Promise<void> {
    const confirmou = await this.confirmacao.confirmar({
      titulo: 'Retirar a proposta?',
      mensagem: `A proposta para "${p.cotacaoNome}" deixa de aparecer para ${p.empresaNome}.`,
      confirmar: 'Retirar',
      destrutivo: true,
    });
    if (!confirmou) {
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
