import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_URL } from '../api.config';
import { Convite, ConviteAberto, ConviteRequest, Membro, TokenResponse, Usuario } from '../models';
import { AuthService } from './auth.service';

/** A equipe da organização e os convites para ela. */
@Injectable({ providedIn: 'root' })
export class EquipeService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly url = `${API_URL}/equipe`;

  membros(): Observable<Membro[]> {
    return this.http.get<Membro[]>(`${this.url}/membros`);
  }

  remover(membroId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/membros/${membroId}`);
  }

  convites(): Observable<Convite[]> {
    return this.http.get<Convite[]>(`${this.url}/convites`);
  }

  convidar(dados: ConviteRequest): Observable<Convite> {
    return this.http.post<Convite>(`${this.url}/convites`, dados);
  }

  cancelarConvite(conviteId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/convites/${conviteId}`);
  }

  /**
   * O link que a pessoa recebe. O token vai depois do "#": essa parte da URL nunca é
   * enviada ao servidor, então não fica em nenhum log de acesso.
   */
  link(token: string): string {
    return `${globalThis.location.origin}/convite#${token}`;
  }

  /** O token também vai no corpo, e não na URL, pelo mesmo motivo. */
  consultar(token: string): Observable<ConviteAberto> {
    return this.http.post<ConviteAberto>(`${API_URL}/convites/consulta`, { token });
  }

  /** Cria a conta como membro e já abre a sessão. */
  aceitar(token: string, nome: string, senha: string): Observable<Usuario> {
    return this.http
      .post<TokenResponse>(
        `${API_URL}/convites/aceite`,
        { token, nome, senha },
        { withCredentials: true },
      )
      .pipe(map((resposta) => this.auth.iniciarSessao(resposta)));
  }
}
