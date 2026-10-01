import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PreferenciaTema, TemaService } from '../../../core/services/tema.service';
import { OpcaoSegmento, Segmentado } from '../../../ui/segmentado';

/** Aparência: Sistema / Claro / Escuro, como controle segmentado de ícones. */
@Component({
  selector: 'app-seletor-tema',
  imports: [Segmentado],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ui-segmentado
      rotulo="Aparência"
      [somenteIcones]="true"
      [opcoes]="opcoes"
      [valor]="tema.preferencia()"
      (valorChange)="tema.definir($event!)"
    />
  `,
  styles: `
    :host {
      display: inline-block;
    }
  `,
})
export class SeletorTema {
  protected readonly tema = inject(TemaService);

  protected readonly opcoes: OpcaoSegmento<PreferenciaTema>[] = [
    { valor: 'sistema', rotulo: 'Igual ao sistema', icone: 'automatico' },
    { valor: 'claro', rotulo: 'Claro', icone: 'sol' },
    { valor: 'escuro', rotulo: 'Escuro', icone: 'lua' },
  ];
}
