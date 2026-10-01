import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cotacao, StatusCotacao } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { STATUS_COTACAO } from '../../../core/utils/formatos';
import { Navbar } from '../../shared/navbar/navbar';

type Filtro = 'TODAS' | StatusCotacao;

@Component({
  selector: 'app-consultar-cotacao',
  imports: [Navbar, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './consultar-cotacao.html',
  styleUrl: './consultar-cotacao.css',
})
export class ConsultarCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);

  protected readonly status = STATUS_COTACAO;
  protected readonly filtros: { valor: Filtro; texto: string }[] = [
    { valor: 'TODAS', texto: 'Todas' },
    { valor: 'ABERTA', texto: 'Abertas' },
    { valor: 'EM_NEGOCIACAO', texto: 'Em negociação' },
    { valor: 'FECHADA', texto: 'Fechadas' },
    { valor: 'CANCELADA', texto: 'Canceladas' },
  ];

  protected readonly solicitacoes = signal<Cotacao[] | null>(null);
  protected readonly filtro = signal<Filtro>('TODAS');
  protected readonly erro = signal<string | null>(null);

  protected readonly filtradas = computed(() => {
    const lista = this.solicitacoes() ?? [];
    const f = this.filtro();
    return f === 'TODAS' ? lista : lista.filter((c) => c.status === f);
  });

  ngOnInit(): void {
    this.cotacaoService.listarMinhas().subscribe({
      next: (lista) => this.solicitacoes.set(lista),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  contar(f: Filtro): number {
    const lista = this.solicitacoes() ?? [];
    return f === 'TODAS' ? lista.length : lista.filter((c) => c.status === f).length;
  }
}
