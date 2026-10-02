import { compararOferta, formatarMoeda } from './oferta';

describe('compararOferta', () => {
  it('diz quanto e em que direção a oferta nova se afasta da que está na mesa', () => {
    expect(compararOferta(54000, 55000)).toBe(`${formatarMoeda(1000)} abaixo da oferta na mesa (−1,8%).`);
    expect(compararOferta(57750, 55000)).toBe(`${formatarMoeda(2750)} acima da oferta na mesa (+5%).`);
  });

  it('reconhece o mesmo valor', () => {
    expect(compararOferta(55000, 55000)).toBe('Igual à oferta na mesa.');
  });

  it('não compara sem um valor válido', () => {
    expect(compararOferta(null, 55000)).toBeNull();
    expect(compararOferta(0, 55000)).toBeNull();
    expect(compararOferta(-10, 55000)).toBeNull();
    expect(compararOferta(Number.NaN, 55000)).toBeNull();
  });
});
