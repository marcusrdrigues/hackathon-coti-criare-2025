import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, skip } from 'rxjs';
import { Cotacao, StatusCotacao } from '../../../core/models';
import { aoReceberAviso, chegouProposta } from '../../../core/services/avisos.service';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { STATUS_COTACAO } from '../../../core/utils/formatos';
import { ESPERA_DA_BUSCA_MS, ListaPaginada } from '../../../core/utils/lista-paginada';
import { CarregarMais } from '../../../ui/carregar-mais';
import { Icone } from '../../../ui/icone';
import { OpcaoSegmento, Segmentado } from '../../../ui/segmentado';
import { Status } from '../../../ui/status';

type Filtro = 'TODAS' | StatusCotacao;

/** Cotações da empresa, com filtro por situação e busca, carregadas aos poucos. */
@Component({
  selector: 'app-consultar-cotacao',
  imports: [RouterLink, DatePipe, CurrencyPipe, CarregarMais, Icone, Segmentado, Status],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './consultar-cotacao.html',
  styleUrl: './consultar-cotacao.css',
})
export class ConsultarCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);

  protected readonly status = STATUS_COTACAO;
  protected readonly filtro = signal<Filtro>('TODAS');
  protected readonly busca = signal('');
  /** Quantas cotações há em cada situação (para os números dos filtros e o estado vazio). */
  private readonly contagem = signal<Record<StatusCotacao, number> | null>(null);

  protected readonly lista = new ListaPaginada<Cotacao>((pagina, tamanho) =>
    this.cotacaoService.minhas(
      pagina,
      { status: this.filtro() === 'TODAS' ? null : (this.filtro() as StatusCotacao), busca: this.busca() },
      tamanho,
    ),
  );

  protected readonly total = computed(() => {
    const c = this.contagem();
    return c ? Object.values(c).reduce((soma, n) => soma + n, 0) : null;
  });

  protected readonly filtros = computed<OpcaoSegmento<Filtro>[]>(() => {
    const c = this.contagem();
    return [
      { valor: 'TODAS', rotulo: 'Todas', contagem: this.total() },
      { valor: 'ABERTA', rotulo: 'Abertas', contagem: c?.ABERTA ?? null },
      { valor: 'EM_NEGOCIACAO', rotulo: 'Em negociação', contagem: c?.EM_NEGOCIACAO ?? null },
      { valor: 'FECHADA', rotulo: 'Fechadas', contagem: c?.FECHADA ?? null },
      { valor: 'CANCELADA', rotulo: 'Canceladas', contagem: c?.CANCELADA ?? null },
    ];
  });

  constructor() {
    // O total de propostas de cada cotação muda quando chega uma nova
    aoReceberAviso(chegouProposta, () => this.lista.recarregar());
    toObservable(this.busca)
      .pipe(skip(1), debounceTime(ESPERA_DA_BUSCA_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.lista.recomecar());
  }

  ngOnInit(): void {
    this.lista.recomecar();
    this.cotacaoService.contagem().subscribe({ next: (c) => this.contagem.set(c), error: () => undefined });
  }

  protected filtrar(filtro: Filtro | undefined): void {
    if (filtro && filtro !== this.filtro()) {
      this.filtro.set(filtro);
      this.lista.recomecar();
    }
  }
}
