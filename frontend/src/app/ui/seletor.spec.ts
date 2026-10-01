import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { OpcaoSeletor, Seletor } from './seletor';

@Component({
  imports: [Seletor],
  template: `<span id="rotulo">Categoria</span><ui-seletor idRotulo="rotulo" [opcoes]="opcoes" [(valor)]="valor" />`,
})
class Hospedeiro {
  readonly opcoes: OpcaoSeletor<string>[] = [
    { valor: 'TI', rotulo: 'Tecnologia' },
    { valor: 'LIMPEZA', rotulo: 'Limpeza' },
    { valor: 'OUTROS', rotulo: 'Outros' },
  ];
  readonly valor = signal<string | null>('TI');
}

describe('Seletor', () => {
  async function montar() {
    const fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const gatilho = el.querySelector<HTMLButtonElement>('.seletor-gatilho')!;
    return { fixture, el, gatilho };
  }

  function tecla(alvo: Element, key: string): void {
    alvo.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
  }

  it('mostra a opção escolhida e é nomeado pelo rótulo', async () => {
    const { gatilho } = await montar();
    expect(gatilho.textContent).toContain('Tecnologia');
    expect(gatilho.getAttribute('aria-labelledby')).toContain('rotulo');
    expect(gatilho.getAttribute('aria-expanded')).toBe('false');
  });

  it('abre pelo teclado, anda com as setas e escolhe com Enter', async () => {
    const { fixture, el, gatilho } = await montar();

    tecla(gatilho, 'ArrowDown');
    await fixture.whenStable();
    const lista = el.querySelector('[role="listbox"]')!;
    expect(lista).not.toBeNull();
    expect(lista.querySelector('[aria-selected="true"]')?.textContent).toContain('Tecnologia');

    tecla(lista, 'ArrowDown');
    tecla(lista, 'Enter');
    await fixture.whenStable();

    expect(fixture.componentInstance.valor()).toBe('LIMPEZA');
    expect(el.querySelector('[role="listbox"]')).toBeNull();
  });

  it('Esc fecha sem mudar o valor', async () => {
    const { fixture, el, gatilho } = await montar();
    gatilho.click();
    await fixture.whenStable();

    tecla(el.querySelector('[role="listbox"]')!, 'End');
    tecla(el.querySelector('[role="listbox"]')!, 'Escape');
    await fixture.whenStable();

    expect(fixture.componentInstance.valor()).toBe('TI');
    expect(el.querySelector('[role="listbox"]')).toBeNull();
  });

  it('busca pela primeira letra', async () => {
    const { fixture, el, gatilho } = await montar();
    gatilho.click();
    await fixture.whenStable();

    tecla(el.querySelector('[role="listbox"]')!, 'o');
    tecla(el.querySelector('[role="listbox"]')!, 'Enter');
    await fixture.whenStable();

    expect(fixture.componentInstance.valor()).toBe('OUTROS');
  });
});
