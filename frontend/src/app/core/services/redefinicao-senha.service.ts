import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';

/**
 * "Esqueci minha senha" (spec 004). O token do link vai sempre no corpo, nunca na URL:
 * assim ele não fica em log de acesso nem em histórico.
 */
@Injectable({ providedIn: 'root' })
export class RedefinicaoSenhaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/auth/redefinicao`;

  /** A resposta é a mesma com conta ou sem: a tela nunca sabe se o e-mail existe. */
  pedir(email: string): Observable<void> {
    return this.http.post<void>(this.url, { email });
  }

  /** Confere se o link ainda vale (404 quando não vale, por qualquer motivo). */
  consultar(token: string): Observable<void> {
    return this.http.post<void>(`${this.url}/consulta`, { token });
  }

  /** Troca a senha; todas as sessões da pessoa caem no servidor. */
  confirmar(token: string, senha: string): Observable<void> {
    return this.http.post<void>(`${this.url}/confirmacao`, { token, senha });
  }
}
