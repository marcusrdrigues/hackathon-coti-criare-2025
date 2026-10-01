import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Segmentado } from './segmentado';

@Component({
  imports: [Segmentado],
  template: `<ui-segmentado rotulo="Situação" [opcoes]="opcoes" [(valor)]="valor" />`,
})
class Hospedeiro {
  readonly opcoes = [
    { valor: 'todas', rotulo: 'Todas', contagem: 3 },
    { valor: 'abertas', rotulo: 'Abertas', contagem: 1 },
  ];
  readonly valor = signal('todas');
}

describe('Segmentado', () => {
  it('usa rádios nativos agrupados pela legenda e atualiza o valor', async () => {
    const fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('legend')?.textContent).toBe('Situação');
    const radios = el.querySelectorAll<HTMLInputElement>('input[type="radio"]');
    expect(radios.length).toBe(2);
    expect(radios[0].checked).toBe(true);

    radios[1].click();
    await fixture.whenStable();

    expect(fixture.componentInstance.valor()).toBe('abertas');
    expect(el.textContent).toContain('Abertas');
  });
});
