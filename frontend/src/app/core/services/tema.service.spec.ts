import { TestBed } from '@angular/core/testing';
import { TemaService } from './tema.service';

describe('TemaService', () => {
  const tema = () => document.documentElement.getAttribute('data-bs-theme');

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-bs-theme');
    TestBed.resetTestingModule();
  });

  it('segue o sistema quando não há preferência salva', () => {
    const servico = TestBed.inject(TemaService);
    expect(servico.preferencia()).toBe('sistema');
  });

  it('fixa o escuro, aplica no <html> e guarda a escolha', () => {
    const servico = TestBed.inject(TemaService);
    servico.definir('escuro');
    TestBed.tick();
    expect(servico.escuro()).toBe(true);
    expect(tema()).toBe('dark');
    expect(localStorage.getItem('tema')).toBe('escuro');
  });

  it('fixa o claro', () => {
    const servico = TestBed.inject(TemaService);
    servico.definir('claro');
    TestBed.tick();
    expect(servico.escuro()).toBe(false);
    expect(tema()).toBe('light');
  });

  it('volta a seguir o sistema e esquece a escolha', () => {
    localStorage.setItem('tema', 'escuro');
    const servico = TestBed.inject(TemaService);
    expect(servico.preferencia()).toBe('escuro');
    servico.definir('sistema');
    expect(localStorage.getItem('tema')).toBeNull();
  });
});
