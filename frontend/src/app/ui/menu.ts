import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  ElementRef,
  afterNextRender,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';

/** Marca um botão ou link como item do menu (papel, foco e estilo). */
@Directive({
  selector: '[uiMenuItem]',
  host: { role: 'menuitem', class: 'menu-item', tabindex: '-1' },
})
export class MenuItem {}

/**
 * Menu suspenso, aberto por um botão.
 *
 * tipo="menu" (padrão): lista de ações com [uiMenuItem]; setas, Home/End e Esc.
 * tipo="painel": conteúdo livre (ex.: conta + aparência); Tab navega normalmente.
 *
 * Fecha com Esc, ao clicar fora, ao escolher um item ou ao clicar em algo
 * marcado com data-fechar. Conteúdo do botão vai em [gatilho].
 *
 *   <ui-menu rotulo="Mais ações">
 *     <ui-icone gatilho nome="reticencias" />
 *     <button uiMenuItem (click)="editar()">Editar</button>
 *   </ui-menu>
 */
@Component({
  selector: 'ui-menu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:pointerdown)': 'cliqueNoDocumento($event)',
  },
  template: `
    <button
      #gatilho
      type="button"
      [class]="classeGatilho()"
      [attr.aria-label]="rotulo()"
      [attr.aria-haspopup]="tipo() === 'menu' ? 'menu' : 'dialog'"
      [attr.aria-expanded]="aberto()"
      (click)="alternar($event)"
      (keydown)="teclaNoGatilho($event)"
    >
      <ng-content select="[gatilho]" />
    </button>
    <div
      #painel
      class="menu-painel"
      [class.para-cima]="direcao() === 'acima'"
      [class.no-inicio]="alinhar() === 'inicio'"
      [attr.role]="tipo() === 'menu' ? 'menu' : 'dialog'"
      [attr.aria-label]="rotulo()"
      [hidden]="!aberto()"
      (keydown)="teclaNoPainel($event)"
      (click)="cliqueNoPainel($event)"
    >
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      position: relative;
      display: inline-flex;
    }

    .menu-painel {
      position: absolute;
      z-index: 70;
      top: calc(100% + 0.375rem);
      right: 0;
      min-width: 13rem;
      padding: 0.25rem;
      background: var(--cor-flutuante);
      border-radius: var(--raio-grande);
      box-shadow: var(--sombra-flutuante);
      animation: surgir var(--duracao-rapida) var(--curva);
    }

    .menu-painel[hidden] {
      display: none;
    }

    .menu-painel.no-inicio {
      right: auto;
      left: 0;
    }

    .menu-painel.para-cima {
      top: auto;
      bottom: calc(100% + 0.375rem);
    }

    .menu-painel:focus {
      outline: none;
    }
  `,
})
export class Menu {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  /** Nome acessível do botão e do menu */
  readonly rotulo = input.required<string>();
  readonly tipo = input<'menu' | 'painel'>('menu');
  readonly alinhar = input<'inicio' | 'fim'>('fim');
  readonly direcao = input<'abaixo' | 'acima'>('abaixo');
  readonly classeGatilho = input('botao botao-secundario botao-icone');

  protected readonly aberto = signal(false);

  private readonly gatilho = viewChild.required<ElementRef<HTMLButtonElement>>('gatilho');
  private readonly painel = viewChild.required<ElementRef<HTMLElement>>('painel');

  fechar(devolverFoco = false): void {
    this.aberto.set(false);
    if (devolverFoco) {
      this.gatilho().nativeElement.focus();
    }
  }

  protected alternar(evento: MouseEvent): void {
    if (this.aberto()) {
      this.fechar();
    } else {
      // detail 0: aberto pelo teclado (Enter/Espaço), então o foco entra no menu
      this.abrir(evento.detail === 0);
    }
  }

  protected teclaNoGatilho(evento: KeyboardEvent): void {
    if (this.tipo() === 'menu' && (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') && !this.aberto()) {
      evento.preventDefault();
      this.abrir(true);
    }
  }

  protected teclaNoPainel(evento: KeyboardEvent): void {
    if (evento.key === 'Escape') {
      evento.preventDefault();
      this.fechar(true);
      return;
    }
    if (this.tipo() !== 'menu') {
      return;
    }
    const itens = this.itens();
    const atual = itens.indexOf(document.activeElement as HTMLElement);
    let proximo: number | null = null;
    switch (evento.key) {
      case 'ArrowDown':
        proximo = (atual + 1) % itens.length;
        break;
      case 'ArrowUp':
        proximo = (atual - 1 + itens.length) % itens.length;
        break;
      case 'Home':
        proximo = 0;
        break;
      case 'End':
        proximo = itens.length - 1;
        break;
      case 'Tab':
        this.fechar();
        return;
    }
    if (proximo !== null) {
      evento.preventDefault();
      itens[proximo]?.focus();
    }
  }

  protected cliqueNoPainel(evento: Event): void {
    const alvo = evento.target as HTMLElement;
    if (alvo.closest('[role="menuitem"], [data-fechar]')) {
      this.fechar();
    }
  }

  protected cliqueNoDocumento(evento: Event): void {
    if (this.aberto() && !this.host.nativeElement.contains(evento.target as Node)) {
      this.fechar();
    }
  }

  private abrir(focarPrimeiro: boolean): void {
    this.aberto.set(true);
    afterNextRender(
      () => {
        if (this.tipo() === 'menu') {
          // Num menu de ações o foco sempre entra no primeiro item, como no sistema
          this.itens()[0]?.focus();
        } else if (focarPrimeiro) {
          this.painel().nativeElement.querySelector<HTMLElement>('button, a, input, [tabindex]')?.focus();
        }
      },
      { injector: this.injector },
    );
  }

  private itens(): HTMLElement[] {
    return Array.from(this.painel().nativeElement.querySelectorAll<HTMLElement>('[role="menuitem"]:not([disabled])'));
  }
}
