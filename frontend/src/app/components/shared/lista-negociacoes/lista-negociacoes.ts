import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Negociacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
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
        <li>
          <a
            class="linha"
            [routerLink]="['/pages/negociacao', n.id]"
            [class.selecionada]="n.id === selecionada()"
            [attr.aria-current]="n.id === selecionada() ? 'page' : null"
          >
            <span class="inicial" aria-hidden="true">{{ iniciais(outraParte(n)) }}</span>
            <span class="linha-principal">
              <span class="topo-linha">
                <span class="linha-titulo">{{ outraParte(n) }}</span>
                <span class="data numeros">{{ n.dataInicio | date: 'dd/MM' }}</span>
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

/** Em andamento primeiro; depois as mais recentes. */
export function ordenarNegociacoes(lista: Negociacao[]): Negociacao[] {
  return [...lista].sort(
    (a, b) =>
      Number(b.status === 'EM_ANDAMENTO') - Number(a.status === 'EM_ANDAMENTO') ||
      (b.dataInicio ?? '').localeCompare(a.dataInicio ?? ''),
  );
}
