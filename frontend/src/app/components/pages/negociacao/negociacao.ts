import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, forkJoin, interval, switchMap } from 'rxjs';
import { Mensagem, Negociacao as NegociacaoModel } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { STATUS_NEGOCIACAO } from '../../../core/utils/formatos';
import { Navbar } from '../../shared/navbar/navbar';

/** Intervalo para buscar mensagens novas da outra parte. */
const ATUALIZACAO_MS = 10_000;

/**
 * Sala de negociação, usada pelos dois perfis. A empresa pode fechar o negócio
 * ou encerrar sem acordo; os dois lados trocam mensagens e contrapropostas.
 */
@Component({
  selector: 'app-negociacao',
  imports: [Navbar, FormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './negociacao.html',
  styleUrl: './negociacao.css',
})
export class Negociacao implements OnInit {
  private readonly negociacaoService = inject(NegociacaoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly status = STATUS_NEGOCIACAO;
  protected readonly negociacao = signal<NegociacaoModel | null>(null);
  protected readonly mensagens = signal<Mensagem[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly enviando = signal(false);

  textoMensagem = '';
  valorContraproposta: number | null = null;

  protected readonly ehEmpresa = this.auth.ehEmpresa;
  protected readonly emAndamento = computed(() => this.negociacao()?.status === 'EM_ANDAMENTO');

  /** Se a última oferta veio da outra parte, a vez de responder é minha. */
  protected readonly aguardandoMinhaResposta = computed(() => {
    const ultima = this.mensagens().at(-1);
    return !!ultima && ultima.tipoRemetente !== this.auth.usuario()?.tipo;
  });

  protected readonly rotaVoltar = computed(() => {
    const n = this.negociacao();
    if (this.ehEmpresa()) {
      return n ? `/pages/detalhe-cotacao/${n.cotacaoId}` : '/pages/consultar-cotacao';
    }
    return n?.status === 'EM_ANDAMENTO' ? '/pages/propostas-enviadas' : '/pages/historico-propostas';
  });

  ngOnInit(): void {
    this.carregar();

    interval(ATUALIZACAO_MS)
      .pipe(
        // Falha momentânea de rede não deve interromper a atualização automática
        switchMap(() => this.negociacaoService.buscar(this.id()).pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((n) => {
        const mudou = n.status !== this.negociacao()?.status || n.ultimaOferta !== this.negociacao()?.ultimaOferta;
        this.negociacao.set(n);
        if (n.status === 'EM_ANDAMENTO' || mudou) {
          this.negociacaoService.mensagens(this.id()).subscribe((m) => this.mensagens.set(m));
        }
      });
  }

  carregar(): void {
    forkJoin({
      negociacao: this.negociacaoService.buscar(this.id()),
      mensagens: this.negociacaoService.mensagens(this.id()),
    }).subscribe({
      next: ({ negociacao, mensagens }) => {
        this.negociacao.set(negociacao);
        this.mensagens.set(mensagens);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  ehMinha(m: Mensagem): boolean {
    return m.remetenteId === this.auth.usuario()?.id;
  }

  enviar(): void {
    const texto = this.textoMensagem.trim();
    const valor = this.valorContraproposta && this.valorContraproposta > 0 ? this.valorContraproposta : null;

    if (!texto && valor === null) {
      this.notificacao.erro('Escreva uma mensagem ou informe um valor de contraproposta.');
      return;
    }
    this.enviarMensagem(texto || null, valor);
  }

  /** Fornecedor concorda com a última oferta da empresa. */
  aceitarOferta(): void {
    const n = this.negociacao();
    if (!n) return;
    this.enviarMensagem('Aceito a sua oferta. Podemos fechar neste valor.', n.ultimaOferta);
  }

  fecharNegocio(): void {
    const n = this.negociacao();
    if (!n) return;
    const valor = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(n.ultimaOferta);
    if (!confirm(`Fechar negócio com ${n.fornecedorNome} por ${valor}? As demais propostas desta cotação serão recusadas.`)) {
      return;
    }
    this.negociacaoService.finalizar(n.id, n.ultimaOferta).subscribe({
      next: () => {
        this.notificacao.sucesso('Negócio fechado! A cotação foi encerrada.');
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }

  encerrarSemAcordo(): void {
    const n = this.negociacao();
    if (!n || !confirm('Encerrar a negociação sem acordo? A cotação volta a ficar aberta para outras propostas.')) {
      return;
    }
    this.negociacaoService.cancelar(n.id).subscribe({
      next: () => {
        this.notificacao.info('Negociação encerrada. A cotação voltou a ficar aberta.');
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }

  private enviarMensagem(mensagem: string | null, valorOfertado: number | null): void {
    this.enviando.set(true);

    // Quem envia a API descobre pelo token
    this.negociacaoService
      .enviarMensagem({ negociacaoId: this.id(), mensagem, valorOfertado })
      .subscribe({
        next: () => {
          this.textoMensagem = '';
          this.valorContraproposta = null;
          this.enviando.set(false);
          this.carregar();
        },
        error: (e) => {
          this.notificacao.erro(mensagemDeErro(e));
          this.enviando.set(false);
        },
      });
  }
}
