import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from './api.config';
import { authInterceptor } from './auth.interceptor';
import { TokenResponse } from './models';
import { AuthService } from './services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let api: HttpTestingController;
  let auth: AuthService;

  const sessao = (accessToken: string): TokenResponse => ({
    accessToken,
    tokenType: 'Bearer',
    expiresIn: 900,
    usuario: {
      id: '1',
      nome: 'Ana',
      email: 'e@e.com',
      tipo: 'EMPRESA',
      papel: 'PROPRIETARIO',
      organizacao: { id: 'o1', razaoSocial: 'Criare', cnpj: '11222333000181' },
    },
  });

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    api = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => api.verify());

  it('envia o access token nas chamadas à API', () => {
    auth.iniciarSessao(sessao('token-1'));

    http.get(`${API_URL}/cotacoes/minhas`).subscribe();

    const req = api.expectOne(`${API_URL}/cotacoes/minhas`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-1');
    req.flush([]);
  });

  it('não envia token para outros domínios', () => {
    auth.iniciarSessao(sessao('token-1'));

    http.get('https://exemplo.com/dados').subscribe();

    const req = api.expectOne('https://exemplo.com/dados');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('renova a sessão e repete a chamada quando o token vence', () => {
    auth.iniciarSessao(sessao('token-vencido'));
    let resposta: unknown;

    http.get(`${API_URL}/dashboard/empresa`).subscribe((r) => (resposta = r));

    api.expectOne(`${API_URL}/dashboard/empresa`).flush({}, { status: 401, statusText: 'Unauthorized' });

    const refresh = api.expectOne(`${API_URL}/auth/refresh`);
    expect(refresh.request.withCredentials).toBe(true);
    refresh.flush(sessao('token-novo'));

    const repetida = api.expectOne(`${API_URL}/dashboard/empresa`);
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer token-novo');
    repetida.flush({ ok: true });

    expect(resposta).toEqual({ ok: true });
    expect(auth.token).toBe('token-novo');
  });

  it('encerra a sessão quando a renovação também falha', () => {
    auth.iniciarSessao(sessao('token-vencido'));
    let erro: unknown;

    http.get(`${API_URL}/cotacoes/minhas`).subscribe({ error: (e) => (erro = e) });

    api.expectOne(`${API_URL}/cotacoes/minhas`).flush({}, { status: 401, statusText: 'Unauthorized' });
    api.expectOne(`${API_URL}/auth/refresh`).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(erro).toBeTruthy();
    expect(auth.logado()).toBe(false);
    expect(auth.token).toBeNull();
  });
});
