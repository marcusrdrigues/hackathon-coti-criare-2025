import { TestBed } from '@angular/core/testing';
import { ConfirmacaoService } from './confirmacao';

describe('ConfirmacaoService', () => {
  let servico: ConfirmacaoService;

  beforeEach(() => {
    servico = TestBed.inject(ConfirmacaoService);
  });

  it('resolve com a resposta da pessoa e fecha o pedido', async () => {
    const resposta = servico.confirmar({ titulo: 'Recusar?', confirmar: 'Recusar', destrutivo: true });
    expect(servico.pedido()?.titulo).toBe('Recusar?');

    servico.pedido()!.responder(true);

    expect(await resposta).toBe(true);
    expect(servico.pedido()).toBeNull();
  });

  it('um pedido novo responde "não" ao anterior', async () => {
    const primeiro = servico.confirmar({ titulo: 'Primeiro', confirmar: 'Ok' });
    const segundo = servico.confirmar({ titulo: 'Segundo', confirmar: 'Ok' });

    expect(await primeiro).toBe(false);
    expect(servico.pedido()?.titulo).toBe('Segundo');

    servico.pedido()!.responder(true);
    expect(await segundo).toBe(true);
  });
});
