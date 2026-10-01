import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Uma forma do desenho: caminho, círculo (cx, cy, r) ou retângulo (x, y, largura, altura, raio). */
interface Forma {
  d?: string;
  circulo?: [number, number, number];
  retangulo?: [number, number, number, number, number];
  /** Preenchida em vez de só contornada */
  cheio?: boolean;
}

/**
 * Conjunto próprio de ícones, em traço fino e uniforme (24×24, traço 1,75).
 * Ícone só onde ajuda a reconhecer uma ação ou um destino, sempre com texto
 * visível ou um aria-label no controle que o contém. O SVG em si é decorativo.
 * Os desenhos são dados, montados pelo template: nada de HTML injetado.
 */
const ICONES = {
  voltar: [{ d: 'M15 18l-6-6 6-6' }],
  avancar: [{ d: 'M9 6l6 6-6 6' }],
  abaixo: [{ d: 'M6 9l6 6 6-6' }],
  'acima-abaixo': [{ d: 'M8 9.5l4-4 4 4M8 14.5l4 4 4-4' }],
  mais: [{ d: 'M12 5v14M5 12h14' }],
  buscar: [{ circulo: [11, 11, 6.5] }, { d: 'M20 20l-4.4-4.4' }],
  enviar: [{ d: 'M12 19V5M6 11l6-6 6 6' }],
  fechar: [{ d: 'M6.5 6.5l11 11M17.5 6.5l-11 11' }],
  check: [{ d: 'M5 12.5l4.5 4.5L19 7' }],
  reticencias: [{ circulo: [5.5, 12, 1.5], cheio: true }, { circulo: [12, 12, 1.5], cheio: true }, { circulo: [18.5, 12, 1.5], cheio: true }],
  inicio: [{ d: 'M4 10.5L12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z' }],
  cotacoes: [{ retangulo: [5, 3.5, 14, 17, 2.5] }, { d: 'M9 8.5h6M9 12h6M9 15.5h3.5' }],
  negociacoes: [{ d: 'M4 6.5A2.5 2.5 0 0 1 6.5 4h7A2.5 2.5 0 0 1 16 6.5v4a2.5 2.5 0 0 1-2.5 2.5H9.5L6 16v-3.1A2.5 2.5 0 0 1 4 10.5z' }, { d: 'M18.5 9.2A2 2 0 0 1 20 11.1v3.4a2 2 0 0 1-1.5 1.9V19l-3.2-2.5H12a2 2 0 0 1-1.7-.9' }],
  mural: [{ retangulo: [4, 4, 7, 7, 1.75] }, { retangulo: [13, 4, 7, 7, 1.75] }, { retangulo: [4, 13, 7, 7, 1.75] }, { retangulo: [13, 13, 7, 7, 1.75] }],
  propostas: [{ d: 'M20.5 3.5l-17 7 6.5 2.5 2.5 6.5z' }, { d: 'M10 13l4.5-4.5' }],
  historico: [{ circulo: [12, 12, 8] }, { d: 'M12 7.5V12l3 2' }],
  sair: [{ d: 'M14.5 4H18a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3.5M10 16l-4-4 4-4M6 12h10' }],
  sol: [{ circulo: [12, 12, 3.75] }, { d: 'M12 3v1.75M12 19.25V21M3 12h1.75M19.25 12H21M5.6 5.6l1.25 1.25M17.15 17.15l1.25 1.25M5.6 18.4l1.25-1.25M17.15 6.85l1.25-1.25' }],
  lua: [{ d: 'M19.5 14.6A7.75 7.75 0 0 1 9.4 4.5 7.75 7.75 0 1 0 19.5 14.6z' }],
  automatico: [{ circulo: [12, 12, 8] }, { d: 'M12 4a8 8 0 0 1 0 16z', cheio: true }],
  alerta: [{ circulo: [12, 12, 8.5] }, { d: 'M12 7.75v5' }, { circulo: [12, 16.25, 0.6], cheio: true }],
  info: [{ circulo: [12, 12, 8.5] }, { d: 'M12 11v5.25' }, { circulo: [12, 7.9, 0.6], cheio: true }],
  sucesso: [{ circulo: [12, 12, 8.5] }, { d: 'M8.25 12.25l2.5 2.5 5-5' }],
  editar: [{ d: 'M4.5 19.5h3.75L18.5 9.25 14.75 5.5 4.5 15.75z' }, { d: 'M13 7.25l3.75 3.75' }],
  pessoa: [{ circulo: [12, 8.5, 3.5] }, { d: 'M5 19.5a7 7 0 0 1 14 0' }],
  painel: [{ retangulo: [3.5, 5, 17, 14, 2.5] }, { d: 'M14.5 5v14' }],
} satisfies Record<string, Forma[]>;

export type NomeIcone = keyof typeof ICONES;

@Component({
  selector: 'ui-icone',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      [attr.width]="tamanho()"
      [attr.height]="tamanho()"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @for (forma of formas(); track $index) {
        @if (forma.d) {
          <svg:path [attr.d]="forma.d" [attr.fill]="forma.cheio ? 'currentColor' : null" [attr.stroke]="forma.cheio ? 'none' : null" />
        } @else if (forma.circulo; as c) {
          <svg:circle [attr.cx]="c[0]" [attr.cy]="c[1]" [attr.r]="c[2]" [attr.fill]="forma.cheio ? 'currentColor' : null" />
        } @else if (forma.retangulo; as r) {
          <svg:rect [attr.x]="r[0]" [attr.y]="r[1]" [attr.width]="r[2]" [attr.height]="r[3]" [attr.rx]="r[4]" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
      line-height: 0;
    }
  `,
})
export class Icone {
  readonly nome = input.required<NomeIcone>();
  readonly tamanho = input(20);

  protected readonly formas = computed<Forma[]>(() => ICONES[this.nome()]);
}
