import { Subject, of } from 'rxjs';
import { Pagina } from '../models';
import { ListaPaginada } from './lista-paginada';

function pagina(itens: number[], numero: number, total: number, tamanho = 20): Pagina<number> {
  return { content: itens, page: { size: tamanho, number: numero, totalElements: total, totalPages: Math.ceil(total / tamanho) } };
}

describe('ListaPaginada', () => {
  it('junta as páginas e sabe quando a lista acabou', () => {
    const pedidos: [number, number][] = [];
    const lista = new ListaPaginada((p, t) => {
      pedidos.push([p, t]);
      return of(p === 0 ? pagina([1, 2], 0, 3) : pagina([3], 1, 3));
    });

    lista.recomecar();
    expect(lista.itens()).toEqual([1, 2]);
    expect(lista.temMais()).toBe(true);

    lista.carregarMais();
    expect(lista.itens()).toEqual([1, 2, 3]);
    expect(lista.temMais()).toBe(false);
    lista.carregarMais();
    expect(pedidos).toEqual([
      [0, 20],
      [1, 20],
    ]);
  });

  it('recarrega as páginas já vistas numa requisição e continua dali', () => {
    const pedidos: [number, number][] = [];
    const lista = new ListaPaginada((p, t) => {
      pedidos.push([p, t]);
      const inicio = p * t;
      return of(pagina(Array.from({ length: t }, (_, i) => inicio + i), p, 100, t));
    });

    lista.recomecar();
    lista.carregarMais();
    lista.recarregar();
    expect(lista.itens()?.length).toBe(40);
    lista.carregarMais();
    expect(pedidos.at(-2)).toEqual([0, 40]);
    expect(pedidos.at(-1)).toEqual([2, 20]);
    expect(lista.itens()?.[40]).toBe(40);
  });

  it('descarta a resposta de um filtro antigo', () => {
    const respostas: Subject<Pagina<number>>[] = [];
    const lista = new ListaPaginada(() => {
      const resposta = new Subject<Pagina<number>>();
      respostas.push(resposta);
      return resposta;
    });

    lista.recomecar();
    lista.recomecar();
    respostas[0].next(pagina([9], 0, 1));
    respostas[1].next(pagina([1], 0, 1));
    expect(lista.itens()).toEqual([1]);
  });

  it('tira um item na hora e ajusta o total', () => {
    const lista = new ListaPaginada(() => of(pagina([1, 2, 3], 0, 3)));
    lista.recomecar();
    lista.remover((n) => n !== 2);
    expect(lista.itens()).toEqual([1, 3]);
    expect(lista.total()).toBe(2);
  });
});
