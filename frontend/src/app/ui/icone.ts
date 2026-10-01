import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Conjunto próprio de ícones, em traço fino e uniforme (24×24, traço 1,75).
 * Ícone só onde ajuda a reconhecer uma ação ou um destino, sempre com texto
 * visível ou um aria-label no controle que o contém. O SVG em si é decorativo.
 */
const ICONES = {
  voltar: '<path d="M15 18l-6-6 6-6"/>',
  avancar: '<path d="M9 6l6 6-6 6"/>',
  abaixo: '<path d="M6 9l6 6 6-6"/>',
  'acima-abaixo': '<path d="M8 9.5l4-4 4 4M8 14.5l4 4 4-4"/>',
  mais: '<path d="M12 5v14M5 12h14"/>',
  buscar: '<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.4-4.4"/>',
  enviar: '<path d="M12 19V5M6 11l6-6 6 6"/>',
  fechar: '<path d="M6.5 6.5l11 11M17.5 6.5l-11 11"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7"/>',
  reticencias:
    '<circle cx="5.5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="18.5" cy="12" r="1.5" fill="currentColor" stroke="none"/>',
  inicio: '<path d="M4 10.5L12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z"/>',
  cotacoes: '<rect x="5" y="3.5" width="14" height="17" rx="2.5"/><path d="M9 8.5h6M9 12h6M9 15.5h3.5"/>',
  negociacoes:
    '<path d="M4 6.5A2.5 2.5 0 0 1 6.5 4h7A2.5 2.5 0 0 1 16 6.5v4a2.5 2.5 0 0 1-2.5 2.5H9.5L6 16v-3.1A2.5 2.5 0 0 1 4 10.5z"/><path d="M18.5 9.2A2 2 0 0 1 20 11.1v3.4a2 2 0 0 1-1.5 1.9V19l-3.2-2.5H12a2 2 0 0 1-1.7-.9"/>',
  mural: '<rect x="4" y="4" width="7" height="7" rx="1.75"/><rect x="13" y="4" width="7" height="7" rx="1.75"/><rect x="4" y="13" width="7" height="7" rx="1.75"/><rect x="13" y="13" width="7" height="7" rx="1.75"/>',
  propostas: '<path d="M20.5 3.5l-17 7 6.5 2.5 2.5 6.5z"/><path d="M10 13l4.5-4.5"/>',
  historico: '<circle cx="12" cy="12" r="8"/><path d="M12 7.5V12l3 2"/>',
  sair: '<path d="M14.5 4H18a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3.5M10 16l-4-4 4-4M6 12h10"/>',
  sol: '<circle cx="12" cy="12" r="3.75"/><path d="M12 3v1.75M12 19.25V21M3 12h1.75M19.25 12H21M5.6 5.6l1.25 1.25M17.15 17.15l1.25 1.25M5.6 18.4l1.25-1.25M17.15 6.85l1.25-1.25"/>',
  lua: '<path d="M19.5 14.6A7.75 7.75 0 0 1 9.4 4.5 7.75 7.75 0 1 0 19.5 14.6z"/>',
  automatico: '<circle cx="12" cy="12" r="8"/><path d="M12 4a8 8 0 0 1 0 16z" fill="currentColor" stroke="none"/>',
  alerta: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.75v5"/><circle cx="12" cy="16.25" r="0.6" fill="currentColor"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.25"/><circle cx="12" cy="7.9" r="0.6" fill="currentColor"/>',
  sucesso: '<circle cx="12" cy="12" r="8.5"/><path d="M8.25 12.25l2.5 2.5 5-5"/>',
  editar: '<path d="M4.5 19.5h3.75L18.5 9.25 14.75 5.5 4.5 15.75z"/><path d="M13 7.25l3.75 3.75"/>',
  pessoa: '<circle cx="12" cy="8.5" r="3.5"/><path d="M5 19.5a7 7 0 0 1 14 0"/>',
  painel: '<rect x="3.5" y="5" width="17" height="14" rx="2.5"/><path d="M14.5 5v14"/>',
} as const;

export type NomeIcone = keyof typeof ICONES;

@Component({
  selector: 'ui-icone',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<svg
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
    [innerHTML]="conteudo()"
  ></svg>`,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
      line-height: 0;
    }
  `,
})
export class Icone {
  private readonly sanitizer = inject(DomSanitizer);

  readonly nome = input.required<NomeIcone>();
  readonly tamanho = input(20);

  // Os desenhos são constantes deste arquivo, nunca dados de fora
  protected readonly conteudo = computed<SafeHtml>(() =>
    this.sanitizer.bypassSecurityTrustHtml(ICONES[this.nome()]),
  );
}
