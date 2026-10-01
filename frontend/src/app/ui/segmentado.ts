import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { Icone, NomeIcone } from './icone';

export interface OpcaoSegmento<T> {
  valor: T;
  rotulo: string;
  icone?: NomeIcone;
  /** Número opcional ao lado do rótulo (ex.: contagem do filtro) */
  contagem?: number | null;
}

let instancias = 0;

/**
 * Controle segmentado: uma escolha entre poucas opções, feito com rádios
 * nativos (setas do teclado e leitor de tela funcionam sem código extra).
 * Uso: <ui-segmentado rotulo="Situação" [opcoes]="..." [(valor)]="filtro" />
 */
@Component({
  selector: 'ui-segmentado',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fieldset class="segmentado" [class.largo]="largo()" [class.so-icones]="somenteIcones()">
      <legend class="visually-hidden">{{ rotulo() }}</legend>
      @for (opcao of opcoes(); track opcao.valor) {
        <input
          class="segmento-entrada"
          type="radio"
          [name]="nome"
          [id]="nome + '-' + $index"
          [checked]="valor() === opcao.valor"
          (change)="valor.set(opcao.valor)"
        />
        <label class="segmento" [for]="nome + '-' + $index" [attr.title]="somenteIcones() ? opcao.rotulo : null">
          @if (opcao.icone) {
            <ui-icone [nome]="opcao.icone" [tamanho]="somenteIcones() ? 17 : 16" />
          }
          <span [class.visually-hidden]="somenteIcones()">{{ opcao.rotulo }}</span>
          @if (opcao.contagem !== undefined && opcao.contagem !== null) {
            <span class="segmento-contagem numeros">{{ opcao.contagem }}</span>
          }
        </label>
      }
    </fieldset>
  `,
  styles: `
    :host {
      display: block;
      max-width: 100%;
      overflow-x: auto;
      scrollbar-width: none;
    }

    :host::-webkit-scrollbar {
      display: none;
    }

    .segmentado {
      display: inline-grid;
      grid-auto-flow: column;
      grid-auto-columns: minmax(max-content, 1fr);
      gap: 2px;
      padding: 2px;
      border-radius: var(--raio);
      background: var(--cor-trilho);
    }

    .segmentado.largo {
      display: grid;
      width: 100%;
    }

    .segmento-entrada {
      position: absolute;
      opacity: 0;
      pointer-events: none;
    }

    .segmento {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.375rem;
      height: calc(var(--altura-controle-pequeno) - 4px);
      padding: 0 0.875rem;
      border-radius: var(--raio-pequeno);
      font-size: var(--texto-pequeno);
      font-weight: 500;
      color: var(--cor-texto);
      white-space: nowrap;
      cursor: pointer;
      transition:
        background-color var(--duracao-rapida) var(--curva),
        box-shadow var(--duracao-rapida) var(--curva);
    }

    .so-icones .segmento {
      width: 2.5rem;
      padding: 0;
      color: var(--cor-texto-2);
    }

    .segmento-entrada:checked + .segmento {
      background: var(--cor-segmento-ativo);
      box-shadow: var(--sombra-leve);
      font-weight: 600;
      color: var(--cor-texto);
    }

    .segmento-entrada:not(:checked) + .segmento:hover {
      color: var(--cor-texto);
      background: color-mix(in srgb, var(--cor-segmento-ativo) 45%, transparent);
    }

    .segmento-entrada:focus-visible + .segmento {
      outline: 2px solid var(--cor-marca-texto);
      outline-offset: 2px;
    }

    .segmento-contagem {
      color: var(--cor-texto-2);
      font-weight: 500;
    }
  `,
})
export class Segmentado<T> {
  readonly rotulo = input.required<string>();
  readonly opcoes = input.required<OpcaoSegmento<T>[]>();
  readonly valor = model<T>();
  /** Ocupa a largura toda, com os segmentos divididos por igual */
  readonly largo = input(false);
  /** Mostra só os ícones; o rótulo fica para o leitor de tela e o title */
  readonly somenteIcones = input(false);

  protected readonly nome = `segmentado-${++instancias}`;
}
