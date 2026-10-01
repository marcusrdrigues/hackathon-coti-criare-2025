import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CategoriaCotacao, CotacaoRequest } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { Navbar } from '../../shared/navbar/navbar';

/** Formulário de nova cotação e também de edição (rota com :id). */
@Component({
  selector: 'app-cadastro-cotacao',
  imports: [Navbar, FormsModule, RouterLink, AsyncPipe, DatePipe],
  templateUrl: './cadastro-cotacao.html',
  styleUrl: './cadastro-cotacao.css',
})
export class CadastroCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  /** Preenchido pela rota /pages/cadastro-cotacao/:id */
  readonly id = input<string>();

  protected readonly categorias$ = this.cotacaoService.categorias();
  protected readonly hoje = new Date();
  protected readonly dataMinima = this.formatarData(new Date(Date.now() + 24 * 60 * 60 * 1000));
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  form = {
    nomeServico: '',
    categoria: 'OUTROS' as CategoriaCotacao,
    orcamentoEstimado: null as number | null,
    dataLimite: '',
    requisitos: '',
  };

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.cotacaoService.buscar(id).subscribe({
      next: (c) => {
        if (c.status !== 'ABERTA') {
          this.notificacao.info('Só é possível editar cotações abertas.');
          this.router.navigate(['/pages/detalhe-cotacao', id]);
          return;
        }
        this.form = {
          nomeServico: c.nomeServico,
          categoria: c.categoria ?? 'OUTROS',
          orcamentoEstimado: c.orcamentoEstimado,
          dataLimite: c.dataLimite ? c.dataLimite.substring(0, 10) : '',
          requisitos: c.requisitos,
        };
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  salvarCotacao(): void {
    if (!this.form.nomeServico.trim() || !this.form.requisitos.trim()) {
      this.erro.set('Informe o título e os requisitos da cotação.');
      return;
    }

    const dados: CotacaoRequest = {
      nomeServico: this.form.nomeServico.trim(),
      requisitos: this.form.requisitos.trim(),
      categoria: this.form.categoria,
      orcamentoEstimado: this.form.orcamentoEstimado || null,
      // A cotação fica aberta até o fim do dia escolhido
      dataLimite: this.form.dataLimite ? `${this.form.dataLimite}T23:59:59` : null,
    };

    const id = this.id();
    const requisicao$ = id ? this.cotacaoService.atualizar(id, dados) : this.cotacaoService.criar(dados);

    this.salvando.set(true);
    this.erro.set(null);
    requisicao$.subscribe({
      next: (cotacao) => {
        this.notificacao.sucesso(id ? 'Cotação atualizada!' : 'Cotação publicada! Os fornecedores já podem enviar propostas.');
        this.router.navigate(['/pages/detalhe-cotacao', cotacao.id]);
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.salvando.set(false);
      },
    });
  }

  private formatarData(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}
