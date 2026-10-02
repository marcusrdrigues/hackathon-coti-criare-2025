import { CurrencyPipe, DatePipe, NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  OnInit,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, filter, forkJoin, interval, pairwise, switchMap } from 'rxjs';
import { Mensagem, Negociacao as NegociacaoModel } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { EventoNegociacao, TempoRealService } from '../../../core/services/tempo-real.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { STATUS_NEGOCIACAO } from '../../../core/utils/formatos';
import { ConfirmacaoService } from '../../../ui/confirmacao';
import { Icone } from '../../../ui/icone';
import { Painel } from '../../../ui/painel';
import { Status } from '../../../ui/status';
import { ListaNegociacoes, ordenarNegociacoes } from '../../shared/lista-negociacoes/lista-negociacoes';

/** Sem conexão em tempo real, a sala volta a consultar a API neste intervalo. */
const ATUALIZACAO_SEM_CONEXAO_MS = 10_000;

/** "Digitando…" some se nenhum sinal novo chegar nesse tempo. */
const DIGITANDO_EXPIRA_MS = 4_000;

/** Intervalo mínimo entre dois sinais de "digitando" enviados. */
const DIGITANDO_INTERVALO_MS = 2_500;

/** Mensagens agrupadas por dia, como no Mensagens do iPhone. */
interface GrupoDoDia {
  dia: string;
  mensagens: Mensagem[];
}

/**
 * Sala de negociação, usada pelos dois perfis. As duas partes trocam mensagens e
 * contrapropostas; a empresa fecha o negócio ou encerra sem acordo; o fornecedor
 * pode aceitar a última oferta da empresa.
 */
@Component({
  selector: 'app-negociacao',
  imports: [
    FormsModule,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    NgTemplateOutlet,
    Icone,
    Painel,
    Status,
    ListaNegociacoes,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './negociacao.html',
  styleUrl: './negociacao.css',
})
export class Negociacao implements OnInit {
  private readonly negociacaoService = inject(NegociacaoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly confirmacao = inject(ConfirmacaoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly tempoReal = inject(TempoRealService);
  protected readonly auth = inject(AuthService);

  /** Ligado ao servidor: a sala atualiza na hora, sem consultar de tempos em tempos. */
  protected readonly aoVivo = this.tempoReal.conectado;
  /** A outra parte está escrevendo agora. */
  protected readonly outraDigitando = signal(false);
  private fimDoDigitando: ReturnType<typeof setTimeout> | undefined;
  private ultimoSinalDigitando = 0;

  readonly id = input.required<string>();

  protected readonly status = STATUS_NEGOCIACAO;
  protected readonly negociacao = signal<NegociacaoModel | null>(null);
  protected readonly mensagens = signal<Mensagem[]>([]);
  protected readonly outras = signal<NegociacaoModel[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly detalhesAbertos = signal(false);

  protected readonly texto = signal('');
  protected readonly valor = signal<number | null>(null);

  private readonly rolagem = viewChild<ElementRef<HTMLElement>>('rolagem');
  private readonly campoValor = viewChild<ElementRef<HTMLInputElement>>('campoValor');

  protected readonly ehEmpresa = this.auth.ehEmpresa;
  protected readonly emAndamento = computed(() => this.negociacao()?.status === 'EM_ANDAMENTO');

  protected readonly outraParte = computed(() => {
    const n = this.negociacao();
    return n ? (this.ehEmpresa() ? n.fornecedorNome : n.empresaNome) : '';
  });

  /** Última mensagem com valor: é a oferta que está na mesa. */
  private readonly ultimaOfertaMsg = computed(() => [...this.mensagens()].reverse().find((m) => m.valorOfertado !== null));

  /** A última oferta veio da outra parte, então a decisão é minha. */
  protected readonly ofertaDaOutraParte = computed(() => {
    const ultima = this.ultimaOfertaMsg();
    return !!ultima && ultima.tipoRemetente !== this.auth.usuario()?.tipo;
  });

  /** Diferença entre a proposta inicial e a última oferta, em % */
  protected readonly variacao = computed(() => {
    const n = this.negociacao();
    if (!n || !n.valorProposta) return null;
    const atual = n.status === 'FINALIZADA' && n.valorFinal !== null ? n.valorFinal : n.ultimaOferta;
    const pct = ((atual - n.valorProposta) / n.valorProposta) * 100;
    if (Math.abs(pct) < 0.05) return null;
    const texto = Math.abs(pct).toLocaleString('pt-BR', { maximumFractionDigits: 1 });
    return { texto: `${pct > 0 ? '+' : '−'}${texto}%`, descricao: `${pct > 0 ? 'acima' : 'abaixo'} da proposta inicial` };
  });

  protected readonly grupos = computed<GrupoDoDia[]>(() => {
    const grupos: GrupoDoDia[] = [];
    for (const m of this.mensagens()) {
      const dia = this.rotuloDoDia(m.dataEnvio);
      const ultimo = grupos.at(-1);
      if (ultimo?.dia === dia) {
        ultimo.mensagens.push(m);
      } else {
        grupos.push({ dia, mensagens: [m] });
      }
    }
    return grupos;
  });

  protected readonly podeEnviar = computed(() => !this.enviando() && (!!this.texto().trim() || (this.valor() ?? 0) > 0));

  constructor() {
    // Trocar de conversa pela lista ao lado reaproveita o componente: recarrega pelo id
    effect(() => {
      this.id();
      untracked(() => {
        this.negociacao.set(null);
        this.mensagens.set([]);
        this.texto.set('');
        this.valor.set(null);
        this.outraDigitando.set(false);
        this.carregar();
      });
    });

    // Eventos da negociação aberta, ao vivo; troca de assinatura quando muda de conversa
    toObservable(this.id)
      .pipe(
        switchMap((id) => this.tempoReal.negociacao(id)),
        takeUntilDestroyed(),
      )
      .subscribe((evento) => this.aoReceber(evento));

    // Voltou a conexão depois de uma queda: busca o que pode ter chegado nesse meio-tempo
    toObservable(this.aoVivo)
      .pipe(
        pairwise(),
        filter(([antes, agora]) => !antes && agora && this.negociacao() !== null),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.carregar());

    inject(DestroyRef).onDestroy(() => clearTimeout(this.fimDoDigitando));

    // Rola até a mensagem mais nova sempre que chegar mensagem
    effect(() => {
      this.mensagens();
      afterNextRender(() => this.rolarParaOFim(), { injector: this.injector });
    });
  }

  ngOnInit(): void {
    this.negociacaoService
      .listarMinhas()
      .pipe(catchError(() => EMPTY))
      .subscribe((lista) => this.outras.set(ordenarNegociacoes(lista)));

    // Plano B: sem tempo real (servidor acordando, rede instável), consulta de tempos em tempos
    interval(ATUALIZACAO_SEM_CONEXAO_MS)
      .pipe(
        filter(() => !this.aoVivo()),
        // Falha momentânea de rede não deve interromper a atualização automática
        switchMap(() => this.negociacaoService.buscar(this.id()).pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((n) => {
        const mudou = n.status !== this.negociacao()?.status || n.ultimaOferta !== this.negociacao()?.ultimaOferta;
        this.negociacao.set(n);
        if (n.status === 'EM_ANDAMENTO' || mudou) {
          this.negociacaoService.mensagens(this.id()).subscribe((m) => {
            if (m.length !== this.mensagens().length) {
              this.mensagens.set(m);
            }
          });
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

  /** Mensagem nova, mudança de status ou "digitando" da outra parte. */
  private aoReceber(evento: EventoNegociacao): void {
    if (evento.tipo === 'DIGITANDO') {
      if (evento.remetente !== this.auth.usuario()?.tipo) {
        this.mostrarDigitando();
      }
      return;
    }
    if (evento.negociacao) {
      this.negociacao.set(evento.negociacao);
    }
    if (evento.tipo === 'MENSAGEM' && evento.mensagem) {
      this.adicionarMensagem(evento.mensagem);
      if (evento.mensagem.tipoRemetente !== this.auth.usuario()?.tipo) {
        this.outraDigitando.set(false);
      }
    }
  }

  /** Acrescenta sem duplicar: a própria mensagem chega pela resposta do POST e pelo tópico. */
  private adicionarMensagem(mensagem: Mensagem): void {
    if (this.mensagens().some((m) => m.id === mensagem.id)) {
      return;
    }
    this.mensagens.update((lista) => [...lista, mensagem].sort((a, b) => a.dataEnvio.localeCompare(b.dataEnvio)));
  }

  private mostrarDigitando(): void {
    this.outraDigitando.set(true);
    clearTimeout(this.fimDoDigitando);
    this.fimDoDigitando = setTimeout(() => this.outraDigitando.set(false), DIGITANDO_EXPIRA_MS);
  }

  /** Ao escrever, avisa a outra parte (no máximo um sinal a cada 2,5 s). */
  protected aoDigitar(texto: string): void {
    this.texto.set(texto);
    const agora = Date.now();
    if (texto.trim() && agora - this.ultimoSinalDigitando > DIGITANDO_INTERVALO_MS) {
      this.ultimoSinalDigitando = agora;
      this.tempoReal.avisarDigitando(this.id());
    }
  }

  protected ehMinha(m: Mensagem): boolean {
    return m.remetenteId === this.auth.usuario()?.id;
  }

  protected ehPrimeira(m: Mensagem): boolean {
    return this.mensagens()[0]?.id === m.id;
  }

  protected enviar(): void {
    if (!this.podeEnviar()) {
      return;
    }
    const valor = (this.valor() ?? 0) > 0 ? this.valor() : null;
    this.enviarMensagem(this.texto().trim() || null, valor);
  }

  /** Enter envia; Shift+Enter quebra a linha */
  protected teclaNaMensagem(evento: KeyboardEvent): void {
    if (evento.key === 'Enter' && !evento.shiftKey && !evento.isComposing) {
      evento.preventDefault();
      this.enviar();
    }
  }

  protected contrapropor(): void {
    this.campoValor()?.nativeElement.focus();
  }

  /** Fornecedor concorda com a última oferta da empresa. */
  protected aceitarOferta(): void {
    const n = this.negociacao();
    if (n) {
      this.enviarMensagem('Aceito a sua oferta. Podemos fechar neste valor.', n.ultimaOferta);
    }
  }

  protected async fecharNegocio(): Promise<void> {
    const n = this.negociacao();
    if (!n) return;
    const confirmou = await this.confirmacao.confirmar({
      titulo: `Fechar negócio por ${this.moeda(n.ultimaOferta)}?`,
      mensagem: `Com ${n.fornecedorNome}. A cotação é encerrada e as demais propostas são recusadas.`,
      confirmar: 'Fechar negócio',
    });
    if (!confirmou) return;
    this.negociacaoService.finalizar(n.id, n.ultimaOferta).subscribe({
      next: () => {
        this.notificacao.sucesso('Negócio fechado. A cotação foi encerrada.');
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }

  protected async encerrarSemAcordo(): Promise<void> {
    const n = this.negociacao();
    if (!n) return;
    const confirmou = await this.confirmacao.confirmar({
      titulo: 'Encerrar sem acordo?',
      mensagem: 'A cotação volta a ficar aberta e você pode negociar com outro fornecedor.',
      confirmar: 'Encerrar',
      destrutivo: true,
    });
    if (!confirmou) return;
    this.detalhesAbertos.set(false);
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
    this.negociacaoService.enviarMensagem({ negociacaoId: this.id(), mensagem, valorOfertado }).subscribe({
      next: (mensagem) => {
        this.texto.set('');
        this.valor.set(null);
        this.enviando.set(false);
        this.ultimoSinalDigitando = 0;
        this.adicionarMensagem(mensagem);
        // A última oferta pode ter mudado; o tópico também avisa, mas sem conexão é a resposta que vale
        this.negociacaoService.buscar(this.id()).subscribe((n) => this.negociacao.set(n));
      },
      error: (e) => {
        this.notificacao.erro(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }

  private rolarParaOFim(): void {
    const el = this.rolagem()?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }

  private rotuloDoDia(data: string): string {
    const d = new Date(data);
    const hoje = new Date();
    const ontem = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 1);
    if (d.toDateString() === hoje.toDateString()) return 'Hoje';
    if (d.toDateString() === ontem.toDateString()) return 'Ontem';
    return d.toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: d.getFullYear() === hoje.getFullYear() ? undefined : 'numeric' });
  }

  private moeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
  }
}
