import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { perfilGuard } from './auth.guards';
import { TipoOrganizacao } from './models';
import { AuthService } from './services/auth.service';

describe('perfilGuard', () => {
  const executar = (guard: ReturnType<typeof perfilGuard>) =>
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
});
