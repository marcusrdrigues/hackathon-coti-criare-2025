import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardEmpresa } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { Navbar } from '../../shared/navbar/navbar';

@Component({
  selector: 'app-dashboard',
  imports: [Navbar, RouterLink, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  protected readonly auth = inject(AuthService);

  protected readonly dados = signal<DashboardEmpresa | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly atualizadoEm = signal<Date | null>(null);

  /** Uma cor por posição no ranking de categorias. */
  protected readonly cores = ['bg-primary', 'bg-success', 'bg-info', 'bg-warning', 'bg-secondary', 'bg-dark', 'bg-danger'];

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.erro.set(null);
    this.dashboardService.empresa().subscribe({
      next: (dados) => {
        this.dados.set(dados);
        this.atualizadoEm.set(new Date());
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
