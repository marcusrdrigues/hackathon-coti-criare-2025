import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Cotacao, DashboardFornecedor as DashboardFornecedorModel, Negociacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { AvisosService, aoReceberAviso, mudouNegociacao } from '../../../core/services/avisos.service';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { diasRestantes } from '../../../core/utils/formatos';
import { Icone } from '../../../ui/icone';

@Component({
  selector: 'app-dashboard-fornecedor',
  imports: [RouterLink, CurrencyPipe, Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard-fornecedor.html',
})
export class DashboardFornecedor implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly cotacaoService = inject(CotacaoService);
  private readonly negociacaoService = inject(NegociacaoService);
  protected readonly auth = inject(AuthService);
  private readonly avisos = inject(AvisosService);

  protected readonly numeros = signal<DashboardFornecedorModel | null>(null);
  protected readonly oportunidades = signal<Cotacao[]>([]);
  private readonly negociacoes = signal<Negociacao[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly diasRestantes = diasRestantes;

  protected readonly negociacoesAtivas = computed(() => this.negociacoes().filter((n) => n.status === 'EM_ANDAMENTO'));

  constructor() {
    aoReceberAviso(mudouNegociacao, () => this.carregar());
  }

  ngOnInit(): void {
    this.carregar();
  }

  private carregar(): void {
    forkJoin({
      numeros: this.dashboardService.fornecedor(),
      abertas: this.cotacaoService.listarAbertas(),
      negociacoes: this.negociacaoService.listarMinhas(),
    }).subscribe({
      next: ({ numeros, abertas, negociacoes }) => {
        this.oportunidades.set(abertas.slice(0, 5));
        this.negociacoes.set(negociacoes);
        this.numeros.set(numeros);
        this.avisos.atualizar(negociacoes);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
