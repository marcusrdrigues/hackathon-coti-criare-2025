import { cnpjValido, diasRestantes, mascararCnpj } from './formatos';

describe('mascararCnpj', () => {
  it('formata um CNPJ completo', () => {
    expect(mascararCnpj('11222333000181')).toBe('11.222.333/0001-81');
  });

  it('formata enquanto o usuário digita', () => {
    expect(mascararCnpj('112')).toBe('11.2');
    expect(mascararCnpj('11222333')).toBe('11.222.333');
  });

  it('ignora letras e corta excesso de dígitos', () => {
    expect(mascararCnpj('11.222.333/0001-81999abc')).toBe('11.222.333/0001-81');
  });
});

describe('diasRestantes', () => {
  it('retorna null quando não há data', () => {
    expect(diasRestantes(null)).toBeNull();
  });

  it('conta os dias até a data informada', () => {
    const daqui3Dias = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000 - 60_000).toISOString();
    expect(diasRestantes(daqui3Dias)).toBe(3);
  });
});

describe('cnpjValido', () => {
  it('aceita CNPJ com dígitos verificadores corretos, com ou sem máscara', () => {
    expect(cnpjValido('11.222.333/0001-81')).toBe(true);
    expect(cnpjValido('11222333000181')).toBe(true);
  });

  it('recusa dígito errado, tamanho errado e números repetidos', () => {
    expect(cnpjValido('11.222.333/0001-82')).toBe(false);
    expect(cnpjValido('1122233300018')).toBe(false);
    expect(cnpjValido('11111111111111')).toBe(false);
  });
});
