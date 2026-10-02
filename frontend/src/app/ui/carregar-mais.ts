import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Rodapé de uma lista paginada: quantos itens já estão na tela, de quantos, e o
 * botão para trazer a próxima página. Some quando a lista já está inteira.
 * Uso: <ui-carregar-mais [mostrando]="itens().length" [total]="total()" [carregando]="..."
 *        singular="organização" plural="organizações" (carregar)="maisUma()" />
 */
@Component({
  selector: 'ui-carregar-mais',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (total() > 0) {
      <div class="carregar-mais">
        <p class="carregar-mais-contagem" aria-live="polite">
          {{ mostrando() }} de {{ total() }} {{ total() === 1 ? singular() : plural() }}
        </p>
        @if (mostrando() < total()) {
          <button type="button" class="botao botao-secundario" [disabled]="carregando()" (click)="carregar.emit()">
            @if (carregando()) {
              <span class="girando" aria-hidden="true"></span>
            }
            Carregar mais {{ plural() }}
          </button>
        }
      </div>
    }
  `,
  styles: `
    .carregar-mais {
      display: grid;
      justify-items: center;
      gap: var(--esp-3);
      margin-top: var(--esp-4);
    }

    .carregar-mais-contagem {
      margin: 0;
      font-size: var(--texto-legenda);
      color: var(--cor-texto-2);
    }
  `,
})
export class CarregarMais {
  readonly mostrando = input.required<number>();
  readonly total = input.required<number>();
  readonly carregando = input(false);
  readonly singular = input('item');
  readonly plural = input('itens');
  readonly carregar = output<void>();
}
