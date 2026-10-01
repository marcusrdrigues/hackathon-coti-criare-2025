import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cotacao, StatusCotacao } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { STATUS_COTACAO } from '../../../core/utils/formatos';
import { Icone } from '../../../ui/icone';
import { OpcaoSegmento, Segmentado } from '../../../ui/segmentado';
import { Status } from '../../../ui/status';

type Filtro = 'TODAS' | StatusCotacao;

/** Cotações da empresa, com filtro por situação e busca. */
@Component({
  selector: 'app-consultar-cotacao',
  imports: [RouterLink, DatePipe, CurrencyPipe, Icone, Segmentado, Status],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './consultar-cotacao.html',
  styleUrl: './consultar-cotacao.css',
})
export class ConsultarCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);

  protected readonly status = STATUS_COTACAO;
  protected readonly cotacoes = signal<Cotacao[] | null>(null);
  protected readonly filtro = signal<Filtro>('TODAS');
  protected readonly busca = signal('');
  protected readonly erro = signal<string | null>(null);

  protected readonly filtros = computed<OpcaoSegmento<Filtro>[]>(() => {
    const lista = this.cotacoes() ?? [];
    const contar = (f: Filtro) => (f === 'TODAS' ? lista.length : lista.filter((c) => c.status === f).length);
    return [
      { valor: 'TODAS', rotulo: 'Todas', contagem: contar('TODAS') },
      { valor: 'ABERTA', rotulo: 'Abertas', contagem: contar('ABERTA') },
      { valor: 'EM_NEGOCIACAO', rotulo: 'Em negociação', contagem: contar('EM_NEGOCIACAO') },
      { valor: 'FECHADA', rotulo: 'Fechadas', contagem: contar('FECHADA') },
      { valor: 'CANCELADA', rotulo: 'Canceladas', contagem: contar('CANCELADA') },
    ];
  });

  protected readonly filtradas = computed(() => {
    const termo = this.busca().trim().toLocaleLowerCase('pt-BR');
    const f = this.filtro();
    return (this.cotacoes() ?? []).filter(
      (c) =>
        (f === 'TODAS' || c.status === f) &&
        (!termo ||
          c.nomeServico.toLocaleLowerCase('pt-BR').includes(termo) ||
          (c.categoriaDescricao ?? '').toLocaleLowerCase('pt-BR').includes(termo)),
    );
  });

  ngOnInit(): void {
    this.cotacaoService.listarMinhas().subscribe({
      next: (lista) => this.cotacoes.set(lista),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
