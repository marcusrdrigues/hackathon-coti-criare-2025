import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Proposta } from '../../../core/models';
import { aoReceberAviso, mudouNegociacao } from '../../../core/services/avisos.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { InfoStatus, iniciais } from '../../../core/utils/formatos';
import { ListaPaginada } from '../../../core/utils/lista-paginada';
import { CarregarMais } from '../../../ui/carregar-mais';
import { ConfirmacaoService } from '../../../ui/confirmacao';
import { Icone } from '../../../ui/icone';
import { Status } from '../../../ui/status';
import { AbasPropostas } from '../../shared/abas-propostas';

/** Propostas do fornecedor que ainda não tiveram desfecho. */
@Component({
  selector: 'app-propostas-enviadas',
  imports: [RouterLink, CurrencyPipe, DatePipe, CarregarMais, Icone, Status, AbasPropostas],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './propostas-enviadas.html',
  styleUrl: './propostas-enviadas.css',
})
export class PropostasEnviadas implements OnInit {
  private readonly propostaService = inject(PropostaService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly confirmacao = inject(ConfirmacaoService);

  protected readonly iniciais = iniciais;

  /** Negociações ativas primeiro (são as que pedem resposta), na ordem da API */
  protected readonly lista = new ListaPaginada<Proposta>((pagina, tamanho) =>
    this.propostaService.minhas('ANDAMENTO', pagina, tamanho),
  );

  constructor() {
    // A situação de cada proposta muda quando a empresa abre, fecha ou encerra a negociação
    aoReceberAviso(mudouNegociacao, () => this.lista.recarregar());
  }

  ngOnInit(): void {
    this.lista.recomecar();
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
      confirmar: 'Retirar proposta',
      destrutivo: true,
    });
    if (!confirmou) {
      return;
    }
    this.propostaService.retirar(p.id).subscribe({
      next: () => {
        this.notificacao.info('Proposta retirada.');
        this.lista.remover((outra) => outra.id !== p.id);
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }
}
