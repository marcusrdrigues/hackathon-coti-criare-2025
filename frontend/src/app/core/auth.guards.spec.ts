import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { perfilGuard } from './auth.guards';
import { Usuario } from './models';

describe('perfilGuard', () => {
  const executar = (guard: ReturnType<typeof perfilGuard>) =>
    TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  const logarComo = (tipo: Usuario['tipo']) =>
    localStorage.setItem(
      'sessao_usuario',
      JSON.stringify({ id: '1', nome: 'Teste', email: 't@t.com', cnpj: '11222333000181', tipo }),
    );

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
