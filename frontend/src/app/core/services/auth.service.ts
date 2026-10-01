import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay } from 'rxjs';
import { API_URL } from '../api.config';
import { LoginRequest, TokenResponse, Usuario } from '../models';
import { NotificacaoService } from './notificacao.service';

/**
 * Só um indicador de que existe uma sessão para tentar restaurar no F5.
 * Nenhum token é guardado no localStorage.
 */
const CHAVE_SESSAO_ATIVA = 'sessao_ativa';

/**
 * Sessão do usuário com JWT:
 * - o access token (curto) fica só em memória, longe do localStorage
 * - o refresh token fica num cookie HttpOnly que o JavaScript não consegue ler;
 *   o navegador o envia sozinho para /auth/refresh
 * - ao recarregar a página, a sessão é restaurada chamando /auth/refresh
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notificacao = inject(NotificacaoService);

  private readonly usuarioAtual = signal<Usuario | null>(null);
  private accessToken: string | null = null;
  /** Evita várias renovações simultâneas quando muitas requisições recebem 401 juntas. */
  private renovacaoEmAndamento$: Observable<string> | null = null;

  readonly usuario = this.usuarioAtual.asReadonly();
  readonly logado = computed(() => this.usuarioAtual() !== null);
  readonly ehEmpresa = computed(() => this.usuarioAtual()?.tipo === 'EMPRESA');
  readonly ehFornecedor = computed(() => this.usuarioAtual()?.tipo === 'FORNECEDOR');

  get token(): string | null {
    return this.accessToken;
  }

  login(credenciais: LoginRequest): Observable<Usuario> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/login`, credenciais, { withCredentials: true })
      .pipe(map((resposta) => this.iniciarSessao(resposta)));
  }

  /** Troca o refresh token (cookie) por um access token novo. */
  renovarSessao(): Observable<string> {
    if (!this.renovacaoEmAndamento$) {
      this.renovacaoEmAndamento$ = this.http
        .post<TokenResponse>(`${API_URL}/auth/refresh`, {}, { withCredentials: true })
        .pipe(
          map((resposta) => {
            this.iniciarSessao(resposta);
            return resposta.accessToken;
          }),
          finalize(() => (this.renovacaoEmAndamento$ = null)),
          shareReplay(1),
        );
    }
    return this.renovacaoEmAndamento$;
  }

  /** Chamado na inicialização do app: recupera a sessão depois de um F5. */
  restaurarSessao(): Observable<unknown> {
    if (!this.temSessaoSalva()) {
      return of(null);
    }
    return this.renovarSessao().pipe(
      catchError(() => {
        this.limparSessao();
        return of(null);
      }),
    );
  }

  logout(): void {
    // Revoga o refresh token no servidor; mesmo se falhar, a sessão local é encerrada
    this.http
      .post(`${API_URL}/auth/logout`, {}, { withCredentials: true })
      .pipe(catchError(() => of(null)))
      .subscribe();
    this.limparSessao();
    this.router.navigate(['/pages/login']);
  }

  /** O refresh token também expirou ou foi revogado: volta para o login. */
  encerrarPorExpiracao(): void {
    if (!this.logado()) {
      return;
    }
    this.limparSessao();
    this.notificacao.info('Sua sessão expirou. Entre novamente.');
    this.router.navigate(['/pages/login']);
  }

  iniciarSessao(resposta: TokenResponse): Usuario {
    this.accessToken = resposta.accessToken;
    this.usuarioAtual.set(resposta.usuario);
    this.marcarSessao(true);
    return resposta.usuario;
  }

  /** Usuário logado; as telas protegidas por guard podem confiar que existe. */
  get usuarioLogado(): Usuario {
    const usuario = this.usuarioAtual();
    if (!usuario) {
      throw new Error('Nenhum usuário logado');
    }
    return usuario;
  }

  /** Tela inicial de acordo com o perfil. */
  rotaInicial(): string {
    const usuario = this.usuarioAtual();
    if (!usuario) {
      return '/pages/login';
    }
    return usuario.tipo === 'EMPRESA' ? '/pages/dashboard' : '/pages/dashboard-fornecedor';
  }

  private limparSessao(): void {
    this.accessToken = null;
    this.usuarioAtual.set(null);
    this.marcarSessao(false);
  }

  private temSessaoSalva(): boolean {
    try {
      return localStorage.getItem(CHAVE_SESSAO_ATIVA) === '1';
    } catch {
      return false;
    }
  }

  private marcarSessao(ativa: boolean): void {
    try {
      if (ativa) {
        localStorage.setItem(CHAVE_SESSAO_ATIVA, '1');
      } else {
        localStorage.removeItem(CHAVE_SESSAO_ATIVA);
      }
    } catch {
      // Navegação privada sem storage: a sessão só não sobrevive ao F5
    }
  }
}
