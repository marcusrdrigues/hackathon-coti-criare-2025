import { HttpParams } from '@angular/common/http';

/** Itens por página quando a tela não pede outro tamanho (a API aceita até 50). */
export const TAMANHO_DA_PAGINA = 20;

export function parametrosDePagina(pagina: number, tamanho = TAMANHO_DA_PAGINA): HttpParams {
  return new HttpParams().set('page', pagina).set('size', tamanho);
}
