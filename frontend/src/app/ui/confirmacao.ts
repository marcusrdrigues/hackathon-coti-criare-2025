import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injectable,
  Injector,
  afterNextRender,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';

export interface PedidoConfirmacao {
  titulo: string;
  mensagem?: string;
  /** Texto do botão que confirma, dizendo o que ele faz (ex.: "Fechar negócio") */
  confirmar: string;
  cancelar?: string;
  /** Ação que desfaz algo: o botão fica vermelho e o foco começa em "Cancelar" */
  destrutivo?: boolean;
}

interface PedidoEmAndamento extends PedidoConfirmacao {
  responder: (confirmou: boolean) => void;
}

/**
 * Confirmação antes de uma ação importante, no lugar do confirm() do navegador.
 *
 *   if (await this.confirmacao.confirmar({ titulo: 'Recusar a proposta?', confirmar: 'Recusar', destrutivo: true })) { ... }
 */
@Injectable({ providedIn: 'root' })
export class ConfirmacaoService {
  readonly pedido = signal<PedidoEmAndamento | null>(null);

  confirmar(pedido: PedidoConfirmacao): Promise<boolean> {
    // Um pedido novo responde "não" ao anterior, se ainda estiver aberto
    this.pedido()?.responder(false);
    return new Promise((resolve) => {
      this.pedido.set({
        ...pedido,
        responder: (confirmou) => {
          this.pedido.set(null);
          resolve(confirmou);
        },
      });
    });
  }
}

/** Diálogo de alerta no centro da tela. Fica uma vez só, no componente raiz. */
@Component({
  selector: 'ui-confirmacao',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialogo
      class="alerta"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="confirmacao-titulo"
      [attr.aria-describedby]="servico.pedido()?.mensagem ? 'confirmacao-mensagem' : null"
      (cancel)="$event.preventDefault(); responder(false)"
    >
      @if (servico.pedido(); as pedido) {
        <h2 id="confirmacao-titulo" class="alerta-titulo">{{ pedido.titulo }}</h2>
        @if (pedido.mensagem) {
          <p id="confirmacao-mensagem" class="alerta-mensagem">{{ pedido.mensagem }}</p>
        }
        <div class="alerta-acoes">
          <button type="button" class="botao botao-secundario" [attr.autofocus]="pedido.destrutivo ? '' : null" (click)="responder(false)">
            {{ pedido.cancelar ?? 'Cancelar' }}
          </button>
          <button
            type="button"
            class="botao"
            [class.botao-primario]="!pedido.destrutivo"
            [class.botao-perigo]="pedido.destrutivo"
            [attr.autofocus]="pedido.destrutivo ? null : ''"
            (click)="responder(true)"
          >
            {{ pedido.confirmar }}
          </button>
        </div>
      }
    </dialog>
  `,
  styles: `
    .alerta {
      width: min(20rem, calc(100% - 2rem));
      padding: 1.25rem 1.25rem 1rem;
      border: 0;
      border-radius: var(--raio-grande);
      color: var(--cor-texto);
      background: var(--cor-flutuante);
      box-shadow: var(--sombra-flutuante);
      text-align: center;
    }

    .alerta[open] {
      animation: aparecer var(--duracao) var(--curva);
    }

    .alerta::backdrop {
      background: var(--cor-veu);
    }

    .alerta-titulo {
      font-size: var(--texto-destaque);
      font-weight: 600;
      line-height: 1.3;
    }

    .alerta-mensagem {
      margin-top: 0.375rem;
      font-size: var(--texto-pequeno);
      color: var(--cor-texto-2);
      line-height: 1.45;
    }

    .alerta-acoes {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.5rem;
      margin-top: 1.25rem;
    }

    .alerta-acoes .botao {
      padding: 0 0.5rem;
      white-space: normal;
      line-height: 1.2;
    }

    .botao-perigo {
      background: var(--cor-perigo);
      color: var(--cor-sobre-marca);
    }

    .botao-perigo:hover {
      background: var(--cor-perigo-hover);
      color: var(--cor-sobre-marca);
    }

    @keyframes aparecer {
      from {
        transform: scale(1.06);
        opacity: 0;
      }
    }
  `,
})
export class Confirmacao {
  protected readonly servico = inject(ConfirmacaoService);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly injector = inject(Injector);

  constructor() {
    effect(() => {
      const dialogo = this.dialogo().nativeElement;
      if (this.servico.pedido() && !dialogo.open) {
        dialogo.showModal();
        // Foco no botão padrão: "Cancelar" nas ações destrutivas, a ação nas demais
        afterNextRender(() => dialogo.querySelector<HTMLElement>('[autofocus]')?.focus(), {
          injector: this.injector,
        });
      } else if (!this.servico.pedido() && dialogo.open) {
        dialogo.close();
      }
    });
  }

  protected responder(confirmou: boolean): void {
    this.servico.pedido()?.responder(confirmou);
  }
}
