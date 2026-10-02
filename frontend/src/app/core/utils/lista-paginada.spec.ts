import { Subject, of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Pagina } from '../models';
import { ListaPaginada, TAMANHO_DA_PAGINA } from './lista-paginada';

interface Item {
  id: string;
}

const itens = (de: number, ate: number): Item[] => Array.from({ length: ate - de }, (_, i) => ({ id: String(de + i) }));

function pagina(lista: Item[], numero: number, total: number, tamanho = TAMANHO_DA_PAGINA): Pagina<Item> {
  return { content: lista, page: { size: tamanho, number: numero, totalElements: total, totalPages: Math.ceil(total / tamanho) } };
}

/** Uma API de mentira com `total` itens, de "0" em diante. */
function api(total: number, pedidos: [number, number][] = []) {
  return (p: number, t: number) => {
    pedidos.push([p, t]);
    return of(pagina(itens(p * t, Math.min(total, p * t + t)), p, total, t));
  };
}

describe('ListaPaginada', () => {
  it('junta as páginas e sabe quando a lista acabou', () => {
    const pedidos: [number, number][] = [];
    const lista = new ListaPaginada(api(30, pedidos));

    lista.recomecar();
    expect(lista.itens()?.length).toBe(25);
    expect(lista.temMais()).toBe(true);

    lista.carregarMais();
    expect(lista.itens()?.length).toBe(30);
    expect(lista.temMais()).toBe(false);
    lista.carregarMais();
    expect(pedidos).toEqual([
      [0, 25],
      [1, 25],
    ]);
  });

  it('recarrega as páginas já vistas numa requisição e continua dali', () => {
    const pedidos: [number, number][] = [];
    const lista = new ListaPaginada(api(100, pedidos));

    lista.recomecar();
    lista.carregarMais();
    lista.recarregar();
    expect(lista.itens()?.length).toBe(50);
    lista.carregarMais();
    expect(pedidos.at(-2)).toEqual([0, 50]);
    expect(pedidos.at(-1)).toEqual([2, 25]);
    expect(lista.itens()?.[50].id).toBe('50');
  });

  it('não repete um item que a lista empurrou para a página seguinte', () => {
    const lista = new ListaPaginada((p) => of(p === 0 ? pagina(itens(0, 25), 0, 30) : pagina(itens(24, 30), 1, 30)));
    lista.recomecar();
    lista.carregarMais();
    expect(lista.itens()?.map((i) => i.id)).toEqual(itens(0, 30).map((i) => i.id));
  });

  it('descarta a resposta de um filtro antigo', () => {
    const respostas: Subject<Pagina<Item>>[] = [];
    const lista = new ListaPaginada<Item>(() => {
      const resposta = new Subject<Pagina<Item>>();
      respostas.push(resposta);
      return resposta;
    });

    lista.recomecar();
    lista.recomecar();
    respostas[0].next(pagina([{ id: 'velho' }], 0, 1));
    respostas[1].next(pagina([{ id: 'novo' }], 0, 1));
    expect(lista.itens()).toEqual([{ id: 'novo' }]);
  });

  it('remove um item na hora e realinha com a API', () => {
    const pedidos: [number, number][] = [];
    const lista = new ListaPaginada(api(3, pedidos));
    lista.recomecar();
    lista.remover((item) => item.id !== '1');
    expect(lista.total()).toBe(3); // a API de mentira ainda tem os 3: a recarga traz o que ela diz
    expect(pedidos.at(-1)).toEqual([0, 25]);
  });

  it('falha na primeira página vira erro; depois, só um aviso, e some quando der certo', () => {
    let falhar = true;
    const lista = new ListaPaginada((p, t) =>
      falhar ? throwError(() => new HttpErrorResponse({ status: 503 })) : api(60)(p, t),
    );

    lista.recomecar();
    expect(lista.erro()).not.toBeNull();

    falhar = false;
    lista.recomecar();
    expect(lista.erro()).toBeNull();

    falhar = true;
    lista.carregarMais();
    expect(lista.erro()).toBeNull();
    expect(lista.falhaAoAtualizar()).not.toBeNull();
    expect(lista.itens()?.length).toBe(25);

    falhar = false;
    lista.carregarMais();
    expect(lista.falhaAoAtualizar()).toBeNull();
  });
});
