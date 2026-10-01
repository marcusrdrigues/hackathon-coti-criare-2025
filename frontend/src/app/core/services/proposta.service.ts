import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { Proposta, PropostaRequest } from '../models';

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

  listarPorFornecedor(fornecedorId: string): Observable<Proposta[]> {
    return this.http.get<Proposta[]>(`${this.url}/fornecedor/${fornecedorId}`);
  }

  recusar(id: string): Observable<Proposta> {
    return this.http.patch<Proposta>(`${this.url}/${id}/recusar`, {});
  }

  retirar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
