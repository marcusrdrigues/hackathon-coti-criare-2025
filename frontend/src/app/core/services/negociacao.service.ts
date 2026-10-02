import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { Mensagem, MensagemRequest, Negociacao } from '../models';

@Injectable({ providedIn: 'root' })
export class NegociacaoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/negociacoes`;

  /** Aceita a proposta e abre a negociação com o fornecedor. */
  iniciar(propostaId: string): Observable<Negociacao> {
    return this.http.post<Negociacao>(this.url, { propostaId });
  }

  /** Negociações em que o usuário logado é uma das partes (mais recentes primeiro). */
  listarMinhas(): Observable<Negociacao[]> {
    return this.http.get<Negociacao[]>(`${this.url}/minhas`);
  }

  buscar(id: string): Observable<Negociacao> {
    return this.http.get<Negociacao>(`${this.url}/${id}`);
  }

  /** Tudo o que a outra parte enviou até agora passa a contar como visto. */
  marcarComoLida(id: string): Observable<void> {
    return this.http.patch<void>(`${this.url}/${id}/leitura`, {});
  }

  finalizar(id: string, valorFinal: number): Observable<Negociacao> {
    return this.http.patch<Negociacao>(`${this.url}/${id}/finalizar`, { valorFinal });
  }

  cancelar(id: string): Observable<Negociacao> {
    return this.http.patch<Negociacao>(`${this.url}/${id}/cancelar`, {});
  }

  mensagens(negociacaoId: string): Observable<Mensagem[]> {
    return this.http.get<Mensagem[]>(`${API_URL}/mensagens/negociacao/${negociacaoId}`);
  }

  enviarMensagem(dados: MensagemRequest): Observable<Mensagem> {
    return this.http.post<Mensagem>(`${API_URL}/mensagens`, dados);
  }
}
