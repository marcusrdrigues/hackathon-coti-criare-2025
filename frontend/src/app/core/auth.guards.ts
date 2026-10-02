import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TipoOrganizacao } from './models';
import { AuthService } from './services/auth.service';

/** Exige login. Se o perfil for informado, também exige que o usuário seja daquele tipo. */
export function perfilGuard(perfil?: TipoOrganizacao): CanActivateFn {
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

/** Telas das organizações (os dois tipos): o superadmin, que não tem organização, volta para a área dele. */
export const organizacaoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const usuario = auth.usuario();
  if (!usuario) {
    return inject(Router).parseUrl('/pages/login');
  }
  return usuario.tipo ? true : inject(Router).parseUrl(auth.rotaInicial());
};

/** Área administrativa: só o superadmin. */
export const superadminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const usuario = auth.usuario();
  if (!usuario) {
    return inject(Router).parseUrl('/pages/login');
  }
  return usuario.papel === 'SUPERADMIN' ? true : inject(Router).parseUrl(auth.rotaInicial());
};

/** Login e cadastro: quem já está logado vai direto para o seu painel. */
export const visitanteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.logado() ? inject(Router).parseUrl(auth.rotaInicial()) : true;
};
