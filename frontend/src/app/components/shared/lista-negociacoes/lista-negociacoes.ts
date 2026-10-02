import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Negociacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { AvisosService } from '../../../core/services/avisos.service';
import { STATUS_NEGOCIACAO, iniciais } from '../../../core/utils/formatos';
import { Status } from '../../../ui/status';

/** Lista de negociações, como a caixa de entrada do Mail. Usada na página e ao lado da conversa. */
@Component({
  selector: 'app-lista-negociacoes',
  imports: [RouterLink, CurrencyPipe, DatePipe, Status],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="lista" role="list" [class.compacta]="compacta()">
      @for (n of negociacoes(); track n.id) {
        @let naoLidas = n.id === selecionada() ? 0 : (avisos.naoLidas()[n.id] ?? 0);
        <li>
          <a
            class="linha"
            [routerLink]="['/pages/negociacao', n.id]"
            [class.nao-lida]="naoLidas > 0"
            [class.selecionada]="n.id === selecionada()"
            [attr.aria-current]="n.id === selecionada() ? 'page' : null"
          >
            <span class="inicial" aria-hidden="true">{{ iniciais(outraParte(n)) }}</span>
            <span class="linha-principal">
              <span class="topo-linha">
                <span class="linha-titulo">{{ outraParte(n) }}</span>
                @if (naoLidas > 0) {
                  <span class="contador numeros" aria-hidden="true">{{ naoLidas > 99 ? '99+' : naoLidas }}</span>
                  <span class="visually-hidden">, {{ naoLidas }} {{ naoLidas === 1 ? 'mensagem não lida' : 'mensagens não lidas' }}</span>
                } @else {
                  <span class="data numeros">{{ n.dataInicio | date: 'dd/MM' }}</span>
                }
              </span>
              <span class="linha-detalhe">{{ n.cotacaoNome }}</span>
              <span class="rodape-linha">
                <ui-status [tom]="status[n.status].tom">{{ status[n.status].texto }}</ui-status>
                <span class="valor numeros">
                  {{ (n.status === 'FINALIZADA' && n.valorFinal !== null ? n.valorFinal : n.ultimaOferta) | currency: 'BRL' : 'symbol' : '1.0-0' }}
                </span>
              </span>
            </span>
          </a>
        </li>
      }
    </ul>
  `,
  styles: `
    .linha {
      align-items: flex-start;
    }

    .linha.selecionada,
    .linha.selecionada:hover {
      background: var(--cor-selecao);
    }

    .topo-linha,
    .rodape-linha {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .rodape-linha {
      margin-top: 0.375rem;
    }

    .data {
      flex-shrink: 0;
      font-size: var(--texto-legenda);
      color: var(--cor-texto-2);
    }

    .nao-lida .linha-titulo {
      font-weight: 700;
    }

    .contador {
      align-self: center;
    }

    .valor {
      flex-shrink: 0;
      font-size: var(--texto-pequeno);
      font-weight: 600;
    }

    .rodape-linha ui-status {
      min-width: 0;
      overflow: hidden;
    }

    .compacta {
      border-radius: 0;
      background: transparent;
    }

    .compacta .linha {
      border-radius: var(--raio);
    }

    .compacta > * + *::before {
      left: calc(2.25rem + 1.75rem);
    }
  `,
})
export class ListaNegociacoes {
  private readonly auth = inject(AuthService);
  protected readonly avisos = inject(AvisosService);

  readonly negociacoes = input.required<Negociacao[]>();
  readonly selecionada = input<string | null>(null);
  /** Sem o fundo da lista agrupada, para ficar dentro de uma coluna */
  readonly compacta = input(false);

  protected readonly status = STATUS_NEGOCIACAO;
  protected readonly iniciais = iniciais;

  protected outraParte(n: Negociacao): string {
    return this.auth.ehEmpresa() ? n.fornecedorNome : n.empresaNome;
  }
}
