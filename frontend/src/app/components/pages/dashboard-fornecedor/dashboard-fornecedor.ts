import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Cotacao, DashboardFornecedor as DashboardFornecedorModel } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { diasRestantes } from '../../../core/utils/formatos';
import { Navbar } from '../../shared/navbar/navbar';

@Component({
  selector: 'app-dashboard-fornecedor',
  imports: [Navbar, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './dashboard-fornecedor.html',
  styleUrl: './dashboard-fornecedor.css',
})
export class DashboardFornecedor implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly cotacaoService = inject(CotacaoService);
  protected readonly auth = inject(AuthService);

  protected readonly kpis = signal<DashboardFornecedorModel | null>(null);
  protected readonly oportunidadesRecentes = signal<Cotacao[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly diasRestantes = diasRestantes;

  ngOnInit(): void {
    forkJoin({
      kpis: this.dashboardService.fornecedor(this.auth.usuarioLogado.id),
      abertas: this.cotacaoService.listarAbertas(),
    }).subscribe({
      next: ({ kpis, abertas }) => {
        this.kpis.set(kpis);
        this.oportunidadesRecentes.set(abertas.slice(0, 5));
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
