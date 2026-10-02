import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_URL } from '../api.config';
import { Categoria, Cotacao, CotacaoRequest, FiltroCotacoes, Pagina, StatusCotacao } from '../models';
import { parametrosDePagina } from './paginas';

@Injectable({ providedIn: 'root' })
export class CotacaoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/cotacoes`;

  /** As categorias não mudam durante o uso, então ficam em cache. */
  private readonly categorias$ = this.http
    .get<Categoria[]>(`${this.url}/categorias`)
    .pipe(shareReplay(1));

  categorias(): Observable<Categoria[]> {
    return this.categorias$;
  }

  buscar(id: string): Observable<Cotacao> {
    return this.http.get<Cotacao>(`${this.url}/${id}`);
  }

  /** Uma página das cotações da empresa logada (mais recentes primeiro, salvo outra ordem). */
  minhas(pagina: number, filtro: FiltroCotacoes = {}, tamanho?: number): Observable<Pagina<Cotacao>> {
    return this.http.get<Pagina<Cotacao>>(`${this.url}/minhas`, { params: filtros(pagina, filtro, tamanho) });
  }

  /** Quantas cotações a empresa tem em cada situação. */
  contagem(): Observable<Record<StatusCotacao, number>> {
    return this.http.get<Record<StatusCotacao, number>>(`${this.url}/minhas/contagem`);
  }

  /** Uma página do mural: abertas e dentro do prazo. */
  mural(pagina: number, filtro: FiltroCotacoes = {}, tamanho?: number): Observable<Pagina<Cotacao>> {
    return this.http.get<Pagina<Cotacao>>(`${this.url}/abertas`, { params: filtros(pagina, filtro, tamanho) });
  }

  criar(dados: CotacaoRequest): Observable<Cotacao> {
    return this.http.post<Cotacao>(this.url, dados);
  }

  atualizar(id: string, dados: CotacaoRequest): Observable<Cotacao> {
    return this.http.put<Cotacao>(`${this.url}/${id}`, dados);
  }

  cancelar(id: string): Observable<Cotacao> {
    return this.http.patch<Cotacao>(`${this.url}/${id}/cancelar`, {});
  }
}

function filtros(pagina: number, filtro: FiltroCotacoes, tamanho?: number): HttpParams {
  let params = parametrosDePagina(pagina, tamanho);
  if (filtro.status) params = params.set('status', filtro.status);
  if (filtro.categoria) params = params.set('categoria', filtro.categoria);
  if (filtro.busca?.trim()) params = params.set('busca', filtro.busca.trim());
  if (filtro.sort) params = params.set('sort', filtro.sort);
  return params;
}
