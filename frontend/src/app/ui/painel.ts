import { ChangeDetectionStrategy, Component, ElementRef, effect, input, model, viewChild } from '@angular/core';
import { Icone } from './icone';

let instancias = 0;

/**
 * Painel lateral (no computador) que vira folha de baixo para cima (no celular).
 * Usa o <dialog> nativo em modo modal: o foco fica preso dentro, Esc fecha e o
 * resto da página fica inerte. O rodapé recebe o conteúdo marcado com [rodape].
 *
 *   <ui-painel titulo="Enviar proposta" [(aberto)]="aberto">
 *     ...campos...
 *     <div rodape>...botões...</div>
 *   </ui-painel>
 */
@Component({
  selector: 'ui-painel',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialogo
      class="painel"
      [attr.aria-labelledby]="id + '-titulo'"
      (close)="aberto.set(false)"
      (click)="cliqueNoFundo($event)"
    >
      <div class="painel-caixa">
        <header class="painel-cabecalho">
          <h2 class="painel-titulo" [id]="id + '-titulo'">{{ titulo() }}</h2>
          <button type="button" class="botao botao-secundario botao-icone botao-pequeno fechar" aria-label="Fechar" (click)="aberto.set(false)">
            <ui-icone nome="fechar" [tamanho]="16" />
          </button>
        </header>
        <div class="painel-corpo">
          <ng-content />
        </div>
        <footer class="painel-rodape">
          <ng-content select="[rodape]" />
        </footer>
      </div>
    </dialog>
  `,
  styles: `
    .painel {
      position: fixed;
      inset: 0 0 0 auto;
      width: min(28rem, 100%);
      height: 100dvh;
      max-width: 100%;
      max-height: 100dvh;
      margin: 0;
      padding: 0;
      border: 0;
      color: var(--cor-texto);
      background: var(--cor-flutuante);
      box-shadow: var(--sombra-flutuante);
    }

    .painel[open] {
      animation: entrar-lateral var(--duracao) var(--curva);
    }

    .painel::backdrop {
      background: var(--cor-veu);
    }

    .painel-caixa {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .painel-cabecalho {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1.25rem 1.25rem 0.75rem 1.5rem;
    }

    .painel-titulo {
      font-size: var(--texto-titulo-3);
      font-weight: 600;
    }

    .fechar {
      border-radius: 50%;
      color: var(--cor-texto-2);
    }

    .painel-corpo {
      flex: 1;
      overflow-y: auto;
      padding: 0.5rem 1.5rem 1.5rem;
    }

    .painel-rodape {
      padding: 1rem 1.5rem calc(1rem + env(safe-area-inset-bottom));
      border-top: 1px solid var(--cor-divisoria);
    }

    .painel-rodape:empty {
      display: none;
    }

    @keyframes entrar-lateral {
      from {
        transform: translateX(2rem);
        opacity: 0;
      }
    }

    @keyframes entrar-de-baixo {
      from {
        transform: translateY(2rem);
        opacity: 0;
      }
    }

    @media (max-width: 639.98px) {
      .painel {
        inset: auto 0 0 0;
        width: 100%;
        height: auto;
        max-height: 92dvh;
        border-radius: var(--raio-grande) var(--raio-grande) 0 0;
      }

      .painel[open] {
        animation-name: entrar-de-baixo;
      }

      .painel-cabecalho {
        padding: 1rem 1rem 0.5rem 1.25rem;
      }

      .painel-corpo {
        padding: 0.5rem 1.25rem 1.25rem;
      }

      .painel-rodape {
        padding-inline: 1.25rem;
      }
    }
  `,
})
export class Painel {
  readonly titulo = input.required<string>();
  readonly aberto = model(false);

  protected readonly id = `painel-${++instancias}`;
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  constructor() {
    effect(() => {
      const dialogo = this.dialogo().nativeElement;
      if (this.aberto() && !dialogo.open) {
        dialogo.showModal();
      } else if (!this.aberto() && dialogo.open) {
        dialogo.close();
      }
    });
  }

  /** Clique no véu (fora da caixa) fecha o painel */
  protected cliqueNoFundo(evento: MouseEvent): void {
    if (evento.target === this.dialogo().nativeElement) {
      this.aberto.set(false);
    }
  }
}
