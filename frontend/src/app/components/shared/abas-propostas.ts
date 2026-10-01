import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { OpcaoSegmento, Segmentado } from '../../ui/segmentado';

type Aba = 'andamento' | 'historico';

/** Alterna entre "Em andamento" e "Histórico" das propostas do fornecedor. */
@Component({
  selector: 'app-abas-propostas',
  imports: [Segmentado],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ui-segmentado rotulo="Propostas" [opcoes]="opcoes" [valor]="atual()" (valorChange)="ir($event)" />`,
  styles: `
    :host {
      display: block;
      margin-bottom: var(--esp-4);
    }
  `,
})
export class AbasPropostas {
  private readonly router = inject(Router);
  readonly atual = input.required<Aba>();

  protected readonly opcoes: OpcaoSegmento<Aba>[] = [
    { valor: 'andamento', rotulo: 'Em andamento' },
    { valor: 'historico', rotulo: 'Histórico' },
  ];

  protected ir(aba: Aba | undefined): void {
    this.router.navigateByUrl(aba === 'historico' ? '/pages/historico-propostas' : '/pages/propostas-enviadas');
  }
}
