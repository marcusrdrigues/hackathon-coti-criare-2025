import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Cotacao, DashboardEmpresa, Negociacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { aoReceberAviso, chegouProposta, mudouNegociacao } from '../../../core/services/avisos.service';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';
import { mensagemDeErro } from '../../../core/utils/erros';
import { diasRestantes, iniciais } from '../../../core/utils/formatos';
import { Icone } from '../../../ui/icone';

/** Quantas pendências o painel mostra. */
const LIMITE_DE_PENDENCIAS = 6;

/** Algo que pede uma ação da empresa agora. */
interface Pendencia {
  titulo: string;
  detalhe: string;
  rota: string[];
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe, Icone, CnpjPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly cotacaoService = inject(CotacaoService);
  private readonly negociacaoService = inject(NegociacaoService);
  protected readonly auth = inject(AuthService);

  protected readonly dados = signal<DashboardEmpresa | null>(null);
  private readonly cotacoes = signal<Cotacao[]>([]);
  private readonly negociacoes = signal<Negociacao[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly iniciais = iniciais;

  /** Primeiro as negociações em andamento, depois propostas a analisar e prazos curtos. */
  protected readonly pendencias = computed<Pendencia[]>(() => {
    const lista: Pendencia[] = [];
    for (const n of this.negociacoes().filter((n) => n.status === 'EM_ANDAMENTO')) {
      lista.push({
        titulo: `Negociação com ${n.fornecedorNome}`,
        detalhe: `${n.cotacaoNome} · última oferta ${this.moeda(n.ultimaOferta)}`,
        rota: ['/pages/negociacao', n.id],
      });
    }
    for (const c of this.cotacoes().filter((c) => c.status === 'ABERTA')) {
      const dias = diasRestantes(c.dataLimite);
      if (c.quantidadePropostas > 0) {
        lista.push({
          titulo: c.quantidadePropostas === 1 ? '1 proposta para analisar' : `${c.quantidadePropostas} propostas para analisar`,
          detalhe: c.melhorOferta !== null ? `${c.nomeServico} · menor valor ${this.moeda(c.melhorOferta)}` : c.nomeServico,
          rota: ['/pages/detalhe-cotacao', c.id],
        });
      } else if (dias !== null && dias >= 0 && dias <= 3) {
        lista.push({
          titulo: dias === 0 ? 'Prazo termina hoje' : dias === 1 ? 'Prazo termina amanhã' : `Prazo termina em ${dias} dias`,
          detalhe: `${c.nomeServico} · ainda sem propostas`,
          rota: ['/pages/detalhe-cotacao', c.id],
        });
      }
    }
    return lista.slice(0, LIMITE_DE_PENDENCIAS);
  });

  protected readonly maiorCategoria = computed(() => Math.max(1, ...(this.dados()?.categorias ?? []).map((c) => c.total)));

  constructor() {
    // Números e atividade ao vivo: proposta nova ou negociação que mudou
    aoReceberAviso((a) => chegouProposta(a) || mudouNegociacao(a), () => this.carregar());
  }

  ngOnInit(): void {
    this.carregar();
  }

  private carregar(): void {
    // Só o que a lista "Precisa da sua atenção" mostra: as negociações em andamento e as
    // cotações abertas com o prazo mais perto do fim
    forkJoin({
      dados: this.dashboardService.empresa(),
      cotacoes: this.cotacaoService.minhas(0, { status: 'ABERTA', sort: 'dataLimite,asc' }),
      negociacoes: this.negociacaoService.minhas('ANDAMENTO', 0, LIMITE_DE_PENDENCIAS),
    }).subscribe({
      next: ({ dados, cotacoes, negociacoes }) => {
        this.cotacoes.set(cotacoes.content);
        this.negociacoes.set(negociacoes.content);
        this.dados.set(dados);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private moeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(valor);
  }
}
