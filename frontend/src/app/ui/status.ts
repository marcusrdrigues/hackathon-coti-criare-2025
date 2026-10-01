import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TomStatus = 'neutro' | 'sucesso' | 'atencao' | 'marca' | 'erro';

/** Status como ponto colorido + texto (a cor nunca vai sozinha). Estilo em controles.css. */
@Component({
  selector: 'ui-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'status', '[attr.data-tom]': 'tom()' },
  template: `<ng-content />`,
})
export class Status {
  readonly tom = input<TomStatus>('neutro');
}
