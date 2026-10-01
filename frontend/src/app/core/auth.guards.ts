import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TipoUsuario } from './models';
import { AuthService } from './services/auth.service';

/** Exige login. Se o perfil for informado, também exige que o usuário seja daquele tipo. */
export function perfilGuard(perfil?: TipoUsuario): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const usuario = auth.usuario();

    if (!usuario) {
      return router.parseUrl('/pages/login');
    }
    if (perfil && usuario.tipo !== perfil) {
      return router.parseUrl(auth.rotaInicial());
    }
    return true;
  };
}

/** Login e cadastro: quem já está logado vai direto para o seu painel. */
export const visitanteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.logado() ? inject(Router).parseUrl(auth.rotaInicial()) : true;
};
