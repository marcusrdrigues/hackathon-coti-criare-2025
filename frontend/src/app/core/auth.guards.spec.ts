import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { CanActivateFn } from '@angular/router';
import { organizacaoGuard, perfilGuard, superadminGuard } from './auth.guards';
import { TipoOrganizacao } from './models';
import { AuthService } from './services/auth.service';

describe('perfilGuard', () => {
  const executar = (guard: CanActivateFn) =>
    TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  const logarComo = (tipo: TipoOrganizacao) =>
    TestBed.inject(AuthService).iniciarSessao({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 900,
      usuario: {
        id: '1',
        nome: 'Teste',
        email: 't@t.com',
        tipo,
        papel: 'PROPRIETARIO',
        organizacao: { id: 'o1', razaoSocial: 'Org Teste', cnpj: '11222333000181' },
      },
    });

  const logarComoSuperadmin = () =>
    TestBed.inject(AuthService).iniciarSessao({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 900,
      usuario: { id: '9', nome: 'Admin', email: 'a@a.com', tipo: null, papel: 'SUPERADMIN', organizacao: null },
    });

  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
  });

  it('manda para o login quando não há sessão', () => {
    const resultado = executar(perfilGuard('EMPRESA'));
    expect((resultado as UrlTree).toString()).toBe('/pages/login');
  });

  it('libera o perfil correto', () => {
    logarComo('EMPRESA');
    expect(executar(perfilGuard('EMPRESA'))).toBe(true);
  });

  it('redireciona o fornecedor que tenta abrir uma tela da empresa', () => {
    logarComo('FORNECEDOR');
    const resultado = executar(perfilGuard('EMPRESA'));
    expect((resultado as UrlTree).toString()).toBe('/pages/dashboard-fornecedor');
  });

  it('leva o superadmin para a área administrativa, longe das telas das organizações', () => {
    logarComoSuperadmin();
    expect(TestBed.inject(AuthService).rotaInicial()).toBe('/pages/admin');
    expect((executar(perfilGuard('EMPRESA')) as UrlTree).toString()).toBe('/pages/admin');
    expect((executar(organizacaoGuard) as UrlTree).toString()).toBe('/pages/admin');
    expect(executar(superadminGuard)).toBe(true);
  });

  it('não deixa uma pessoa de organização entrar na área administrativa', () => {
    logarComo('EMPRESA');
    expect(executar(organizacaoGuard)).toBe(true);
    expect((executar(superadminGuard) as UrlTree).toString()).toBe('/pages/dashboard');
  });
});
