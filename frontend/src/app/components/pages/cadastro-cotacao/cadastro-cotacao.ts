import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { CategoriaCotacao, CotacaoRequest } from '../../../core/models';
import { CotacaoService } from '../../../core/services/cotacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { Icone } from '../../../ui/icone';
import { OpcaoSeletor, Seletor } from '../../../ui/seletor';

/** Formulário de nova cotação e também de edição (rota com :id). */
@Component({
  selector: 'app-cadastro-cotacao',
  imports: [FormsModule, RouterLink, Icone, Seletor],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './cadastro-cotacao.html',
  styleUrl: './cadastro-cotacao.css',
})
export class CadastroCotacao implements OnInit {
  private readonly cotacaoService = inject(CotacaoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  /** Preenchido pela rota /pages/cadastro-cotacao/:id */
  readonly id = input<string>();

  protected readonly categorias = toSignal(
    this.cotacaoService
      .categorias()
      .pipe(map((lista) => lista.map((c): OpcaoSeletor<CategoriaCotacao> => ({ valor: c.codigo, rotulo: c.descricao })))),
    { initialValue: [] },
  );
  protected readonly dataMinima = this.formatarData(new Date(Date.now() + 24 * 60 * 60 * 1000));
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  /** Campos do formulário em signals, para a tela reagir sem detecção de mudanças manual */
  protected readonly form = {
    nomeServico: signal(''),
    categoria: signal<CategoriaCotacao | null>('OUTROS'),
    orcamentoEstimado: signal<number | null>(null),
    dataLimite: signal(''),
    requisitos: signal(''),
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
        this.form.nomeServico.set(c.nomeServico);
        this.form.categoria.set(c.categoria ?? 'OUTROS');
        this.form.orcamentoEstimado.set(c.orcamentoEstimado);
        this.form.dataLimite.set(c.dataLimite ? c.dataLimite.substring(0, 10) : '');
        this.form.requisitos.set(c.requisitos);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  salvarCotacao(): void {
    if (!this.form.nomeServico().trim() || !this.form.requisitos().trim()) {
      this.erro.set('Informe o título e os requisitos da cotação.');
      return;
    }

    const dataLimite = this.form.dataLimite();
    const dados: CotacaoRequest = {
      nomeServico: this.form.nomeServico().trim(),
      requisitos: this.form.requisitos().trim(),
      categoria: this.form.categoria(),
      orcamentoEstimado: this.form.orcamentoEstimado() || null,
      // A cotação fica aberta até o fim do dia escolhido
      dataLimite: dataLimite ? `${dataLimite}T23:59:59` : null,
    };

    const id = this.id();
    const requisicao$ = id ? this.cotacaoService.atualizar(id, dados) : this.cotacaoService.criar(dados);

    this.salvando.set(true);
    this.erro.set(null);
    requisicao$.subscribe({
      next: (cotacao) => {
        this.notificacao.sucesso(id ? 'Cotação atualizada.' : 'Cotação publicada. Os fornecedores já podem enviar propostas.');
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
