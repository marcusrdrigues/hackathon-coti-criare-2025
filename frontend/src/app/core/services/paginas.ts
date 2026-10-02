import { HttpParams } from '@angular/common/http';
import { TAMANHO_DA_PAGINA } from '../utils/lista-paginada';

/** Parâmetros de paginação da API; sem tamanho, vale o das telas (a API aceita até 50). */
export function parametrosDePagina(pagina: number, tamanho = TAMANHO_DA_PAGINA): HttpParams {
  return new HttpParams().set('page', pagina).set('size', tamanho);
}
