import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../api.config';
import { LoginRequest, Usuario } from '../models';

const CHAVE_SESSAO = 'sessao_usuario';

/**
 * Guarda o usuário logado (empresa ou fornecedor) num signal e no localStorage,
 * para a sessão sobreviver ao F5.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly usuarioAtual = signal<Usuario | null>(this.lerSessao());

  readonly usuario = this.usuarioAtual.asReadonly();
  readonly logado = computed(() => this.usuarioAtual() !== null);
  readonly ehEmpresa = computed(() => this.usuarioAtual()?.tipo === 'EMPRESA');
  readonly ehFornecedor = computed(() => this.usuarioAtual()?.tipo === 'FORNECEDOR');

  login(credenciais: LoginRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API_URL}/auth/login`, credenciais).pipe(
      tap((usuario) => {
        localStorage.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
        this.usuarioAtual.set(usuario);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(CHAVE_SESSAO);
    this.usuarioAtual.set(null);
    this.router.navigate(['/pages/login']);
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

  private lerSessao(): Usuario | null {
    try {
      const salvo = localStorage.getItem(CHAVE_SESSAO);
      return salvo ? (JSON.parse(salvo) as Usuario) : null;
    } catch {
      return null;
    }
  }
}
