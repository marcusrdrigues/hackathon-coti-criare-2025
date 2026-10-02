import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { Pagina, Proposta, PropostaRequest, SituacaoProposta } from '../models';
import { parametrosDePagina } from './paginas';

@Injectable({ providedIn: 'root' })
export class PropostaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/propostas`;

  enviar(dados: PropostaRequest): Observable<Proposta> {
    return this.http.post<Proposta>(this.url, dados);
  }

  listarPorCotacao(cotacaoId: string): Observable<Proposta[]> {
    return this.http.get<Proposta[]>(`${this.url}/cotacao/${cotacaoId}`);
  }

  /** Uma página das propostas do fornecedor logado: negociações ativas primeiro, depois as mais recentes. */
  minhas(situacao: SituacaoProposta, pagina: number, tamanho?: number): Observable<Pagina<Proposta>> {
    const params = parametrosDePagina(pagina, tamanho).set('situacao', situacao);
    return this.http.get<Pagina<Proposta>>(`${this.url}/minhas`, { params });
  }

  recusar(id: string): Observable<Proposta> {
    return this.http.patch<Proposta>(`${this.url}/${id}/recusar`, {});
  }

  retirar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
