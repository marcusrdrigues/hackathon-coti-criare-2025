import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_URL } from '../api.config';
import { Categoria, Cotacao, CotacaoRequest } from '../models';

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

  /** Cotações da empresa logada. */
  listarMinhas(): Observable<Cotacao[]> {
    return this.http.get<Cotacao[]>(`${this.url}/minhas`);
  }

  listarAbertas(): Observable<Cotacao[]> {
    return this.http.get<Cotacao[]>(`${this.url}/abertas`);
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
