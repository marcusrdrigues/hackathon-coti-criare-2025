import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  Injector,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { Icone } from './icone';

export interface OpcaoSeletor<T> {
  valor: T;
  rotulo: string;
}

let instancias = 0;

/**
 * Seletor (o "select" do sistema): botão que abre uma lista de opções.
 * Segue o padrão de listbox da WAI-ARIA: setas, Home/End, Enter/Espaço,
 * Esc, busca pela primeira letra; fecha ao clicar fora.
 *
 * Uso:
 *   <label class="rotulo" id="rotulo-categoria">Categoria</label>
 *   <ui-seletor idRotulo="rotulo-categoria" [opcoes]="categorias" [(valor)]="categoria" />
 */
@Component({
  selector: 'ui-seletor',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:pointerdown)': 'cliqueNoDocumento($event)',
  },
  template: `
    <button
      #gatilho
      type="button"
      class="seletor-gatilho"
      [id]="id"
      aria-haspopup="listbox"
      [attr.aria-expanded]="aberto()"
      [attr.aria-controls]="aberto() ? id + '-lista' : null"
      [attr.aria-labelledby]="idRotulo() ? idRotulo() + ' ' + id : null"
      [attr.aria-label]="idRotulo() ? null : rotulo()"
      [disabled]="desabilitado()"
      (click)="alternar()"
      (keydown)="teclaNoGatilho($event)"
    >
      <span class="seletor-valor" [class.vazio]="!selecionada()">{{ selecionada()?.rotulo ?? placeholder() }}</span>
      <ui-icone nome="acima-abaixo" [tamanho]="16" />
    </button>

    @if (aberto()) {
      <ul
        #lista
        class="seletor-lista"
        role="listbox"
        tabindex="-1"
        [id]="id + '-lista'"
        [attr.aria-labelledby]="idRotulo() || null"
        [attr.aria-label]="idRotulo() ? null : rotulo()"
        [attr.aria-activedescendant]="id + '-opcao-' + ativo()"
        (keydown)="teclaNaLista($event)"
      >
        @for (opcao of opcoes(); track opcao.valor; let i = $index) {
          <li
            role="option"
            class="seletor-opcao"
            [id]="id + '-opcao-' + i"
            [class.ativa]="i === ativo()"
            [attr.aria-selected]="opcao.valor === valor()"
            (click)="escolher(opcao)"
            (pointermove)="ativo.set(i)"
          >
            <span>{{ opcao.rotulo }}</span>
            @if (opcao.valor === valor()) {
              <ui-icone nome="check" [tamanho]="16" />
            }
          </li>
        }
      </ul>
    }
  `,
  styles: `
    :host {
      position: relative;
      display: block;
      min-width: 0;
    }

    .seletor-gatilho {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      width: 100%;
      height: var(--altura-controle);
      padding: 0 0.625rem 0 0.75rem;
      font-size: 1rem;
      text-align: left;
      color: var(--cor-texto);
      background: var(--cor-superficie);
      border: 1px solid var(--cor-borda-campo);
      border-radius: var(--raio);
      transition:
        border-color var(--duracao-rapida) var(--curva),
        box-shadow var(--duracao-rapida) var(--curva);
    }

    .seletor-gatilho:focus-visible,
    .seletor-gatilho[aria-expanded='true'] {
      outline: none;
      border-color: var(--cor-marca-texto);
      box-shadow: 0 0 0 3px var(--cor-marca-foco);
    }

    .seletor-gatilho:disabled {
      opacity: 0.5;
    }

    .seletor-gatilho ui-icone {
      color: var(--cor-texto-2);
    }

    .seletor-valor {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .seletor-valor.vazio {
      color: var(--cor-texto-2);
    }

    .seletor-lista {
      position: absolute;
      z-index: 60;
      top: calc(100% + 0.375rem);
      left: 0;
      min-width: 100%;
      max-height: 18rem;
      margin: 0;
      padding: 0.25rem;
      overflow-y: auto;
      list-style: none;
      background: var(--vidro-menu);
      -webkit-backdrop-filter: var(--vidro-filtro);
      backdrop-filter: var(--vidro-filtro);
      border-radius: var(--raio);
      box-shadow: inset 0 0.5px 0 var(--vidro-brilho), 0 0 0 0.5px var(--vidro-contorno), var(--vidro-sombra);
      animation: surgir var(--duracao-rapida) var(--curva);
    }

    .seletor-lista:focus {
      outline: none;
    }

    .seletor-opcao {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.5rem 0.625rem;
      border-radius: 6px;
      font-size: var(--texto-corpo);
      white-space: nowrap;
      cursor: pointer;
    }

    .seletor-opcao.ativa {
      background: var(--cor-selecao);
    }

    .seletor-opcao ui-icone {
      color: var(--cor-marca-texto);
    }
  `,
})
export class Seletor<T> {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  readonly opcoes = input.required<OpcaoSeletor<T>[]>();
  readonly valor = model<T | null>(null);
  readonly placeholder = input('Selecione');
  /** id do <label> que nomeia o seletor (preferível a rotulo) */
  readonly idRotulo = input<string>();
  /** Nome acessível quando não há <label> visível */
  readonly rotulo = input<string>();
  readonly desabilitado = input(false);

  protected readonly id = `seletor-${++instancias}`;
  protected readonly aberto = signal(false);
  protected readonly ativo = signal(0);
  protected readonly selecionada = computed(() => this.opcoes().find((o) => o.valor === this.valor()) ?? null);

  private readonly gatilho = viewChild.required<ElementRef<HTMLButtonElement>>('gatilho');
  private readonly lista = viewChild<ElementRef<HTMLUListElement>>('lista');

  protected alternar(): void {
    if (this.aberto()) {
      this.fechar();
    } else {
      this.abrir();
    }
  }

  protected teclaNoGatilho(evento: KeyboardEvent): void {
    if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(evento.key) && !this.aberto()) {
      evento.preventDefault();
      this.abrir();
    }
  }

  protected teclaNaLista(evento: KeyboardEvent): void {
    const total = this.opcoes().length;
    switch (evento.key) {
      case 'ArrowDown':
        this.ativo.update((i) => Math.min(i + 1, total - 1));
        break;
      case 'ArrowUp':
        this.ativo.update((i) => Math.max(i - 1, 0));
        break;
      case 'Home':
        this.ativo.set(0);
        break;
      case 'End':
        this.ativo.set(total - 1);
        break;
      case 'Enter':
      case ' ':
        this.escolher(this.opcoes()[this.ativo()]);
        break;
      case 'Escape':
        this.fechar(true);
        break;
      case 'Tab':
        this.fechar();
        return;
      default:
        this.buscarPelaLetra(evento.key);
        return;
    }
    evento.preventDefault();
    this.mostrarAtiva();
  }

  protected escolher(opcao: OpcaoSeletor<T> | undefined): void {
    if (opcao) {
      this.valor.set(opcao.valor);
    }
    this.fechar(true);
  }

  protected cliqueNoDocumento(evento: Event): void {
    if (this.aberto() && !this.host.nativeElement.contains(evento.target as Node)) {
      this.fechar();
    }
  }

  private abrir(): void {
    const indice = this.opcoes().findIndex((o) => o.valor === this.valor());
    this.ativo.set(Math.max(indice, 0));
    this.aberto.set(true);
    afterNextRender(
      () => {
        this.lista()?.nativeElement.focus();
        this.mostrarAtiva();
      },
      { injector: this.injector },
    );
  }

  private fechar(devolverFoco = false): void {
    this.aberto.set(false);
    if (devolverFoco) {
      this.gatilho().nativeElement.focus();
    }
  }

  private buscarPelaLetra(tecla: string): void {
    if (tecla.length !== 1) {
      return;
    }
    const letra = tecla.toLocaleLowerCase('pt-BR');
    const indice = this.opcoes().findIndex((o) => o.rotulo.toLocaleLowerCase('pt-BR').startsWith(letra));
    if (indice >= 0) {
      this.ativo.set(indice);
      this.mostrarAtiva();
    }
  }

  private mostrarAtiva(): void {
    this.lista()
      ?.nativeElement.querySelector(`#${this.id}-opcao-${this.ativo()}`)
      ?.scrollIntoView?.({ block: 'nearest' });
  }
}
