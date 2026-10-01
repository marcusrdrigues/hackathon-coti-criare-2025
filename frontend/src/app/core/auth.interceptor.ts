import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_URL } from './api.config';
import { AuthService } from './services/auth.service';

/** Rotas de autenticação cuidam do próprio cookie e não levam o access token. */
const ROTAS_DE_SESSAO = ['/auth/login', '/auth/refresh', '/auth/logout', '/auth/demo'].map((r) => `${API_URL}${r}`);

/**
 * Coloca o access token em toda chamada à API. Se a API responder 401
 * (token vencido), renova a sessão pelo cookie uma vez e repete a chamada.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API_URL) || ROTAS_DE_SESSAO.some((rota) => req.url.startsWith(rota))) {
    return next(req);
  }

  const auth = inject(AuthService);
  const comToken = (requisicao: HttpRequest<unknown>, token: string | null) =>
    token ? requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : requisicao;

  return next(comToken(req, auth.token)).pipe(
    catchError((erro: unknown) => {
      const tokenVencido = erro instanceof HttpErrorResponse && erro.status === 401 && auth.logado();
      if (!tokenVencido) {
        return throwError(() => erro);
      }

      return auth.renovarSessao().pipe(
        catchError(() => {
          auth.encerrarPorExpiracao();
          return throwError(() => erro);
        }),
        switchMap((novoToken) => next(comToken(req, novoToken))),
      );
    }),
  );
};
