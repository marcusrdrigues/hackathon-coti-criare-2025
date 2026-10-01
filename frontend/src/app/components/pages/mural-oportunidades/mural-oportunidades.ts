import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CategoriaCotacao, Cotacao, Proposta } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { PropostaService } from '../../../core/services/proposta.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { diasRestantes } from '../../../core/utils/formatos';
import { Navbar } from '../../shared/navbar/navbar';

interface RascunhoProposta {
  valor: number | null;
  descricao: string;
}

@Component({
  selector: 'app-mural-oportunidades',
  imports: [Navbar, FormsModule, RouterLink, AsyncPipe, CurrencyPipe, DatePipe],
  templateUrl: './mural-oportunidades.html',
  styleUrl: './mural-oportunidades.css',
})
export class MuralOportunidades implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);
  private readonly propostaService = inject(PropostaService);
  private readonly notificacao = inject(NotificacaoService);

  protected readonly categorias$ = this.cotacaoService.categorias();
  protected readonly diasRestantes = diasRestantes;

  protected readonly oportunidades = signal<Cotacao[] | null>(null);
  /** cotacaoId -> proposta que este fornecedor já enviou */
  protected readonly minhasPropostas = signal<Map<string, Proposta>>(new Map());
  protected readonly erro = signal<string | null>(null);
  protected readonly busca = signal('');
  protected readonly categoria = signal<CategoriaCotacao | ''>('');
  protected readonly expandida = signal<string | null>(null);
  protected readonly enviando = signal(false);

  rascunho: RascunhoProposta = { valor: null, descricao: '' };

  protected readonly filtradas = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const categoria = this.categoria();
    return (this.oportunidades() ?? []).filter(
      (c) =>
        (!categoria || c.categoria === categoria) &&
        (!termo ||
          c.nomeServico.toLowerCase().includes(termo) ||
          c.requisitos.toLowerCase().includes(termo) ||
          c.empresaNome.toLowerCase().includes(termo)),
    );
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    forkJoin({
      abertas: this.cotacaoService.listarAbertas(),
      minhas: this.propostaService.listarMinhas(),
    }).subscribe({
      next: ({ abertas, minhas }) => {
        this.oportunidades.set(abertas);
        this.minhasPropostas.set(new Map(minhas.map((p) => [p.cotacaoId, p])));
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  /** Publicada nas últimas 48 horas. */
  ehNova(c: Cotacao): boolean {
    return Date.now() - new Date(c.dataCriacao).getTime() < 48 * 60 * 60 * 1000;
  }

  toggleProposta(c: Cotacao): void {
    if (this.expandida() === c.id) {
      this.expandida.set(null);
      return;
    }
    this.rascunho = { valor: null, descricao: '' };
    this.expandida.set(c.id);
  }

  confirmarEnvio(c: Cotacao): void {
    if (!this.rascunho.valor || this.rascunho.valor <= 0) {
      this.notificacao.erro('Informe o valor da proposta.');
      return;
    }

    this.enviando.set(true);
    this.propostaService
      .enviar({
        valor: this.rascunho.valor,
        descricao: this.rascunho.descricao.trim() || 'Sem condições adicionais.',
        cotacaoId: c.id,
      })
      .subscribe({
        next: (proposta) => {
          this.notificacao.sucesso(`Proposta enviada para ${c.empresaNome}!`);
          this.minhasPropostas.update((mapa) => new Map(mapa).set(c.id, proposta));
          this.expandida.set(null);
          this.enviando.set(false);
        },
        error: (e) => {
          this.notificacao.erro(mensagemDeErro(e));
          this.enviando.set(false);
        },
      });
  }
}
