import { Component, inject } from '@angular/core';
import { PreferenciaTema, TemaService } from '../../../core/services/tema.service';

let instancias = 0;

/** Controle segmentado Sistema / Claro / Escuro, feito com rádios nativos. */
@Component({
  selector: 'app-seletor-tema',
  templateUrl: './seletor-tema.html',
  styleUrl: './seletor-tema.css',
})
export class SeletorTema {
  protected readonly tema = inject(TemaService);
  protected readonly nome = `tema-${++instancias}`;

  protected readonly opcoes: { valor: PreferenciaTema; rotulo: string; icone: string }[] = [
    { valor: 'sistema', rotulo: 'Igual ao sistema', icone: 'bi-circle-half' },
    { valor: 'claro', rotulo: 'Claro', icone: 'bi-sun' },
    { valor: 'escuro', rotulo: 'Escuro', icone: 'bi-moon-stars' },
  ];
}
