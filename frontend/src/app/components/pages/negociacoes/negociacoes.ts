import { ChangeDetectionStrategy, Component, OnInit, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Negociacao, SituacaoNegociacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { aoReceberAviso, mudouNegociacao } from '../../../core/services/avisos.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { ListaPaginada } from '../../../core/utils/lista-paginada';
import { CarregarMais } from '../../../ui/carregar-mais';
import { Icone } from '../../../ui/icone';
import { OpcaoSegmento, Segmentado } from '../../../ui/segmentado';
import { ListaNegociacoes } from '../../shared/lista-negociacoes/lista-negociacoes';

/** Todas as negociações do usuário, dos dois perfis. */
@Component({
  selector: 'app-negociacoes',
  imports: [RouterLink, CarregarMais, Icone, Segmentado, ListaNegociacoes],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pagina">
      <header class="cabecalho">
        <div class="cabecalho-texto">
          <h1 class="titulo-pagina">Negociações</h1>
          <p class="subtitulo-pagina">
            {{ auth.ehEmpresa() ? 'Conversas com os fornecedores até fechar o valor' : 'Conversas com as empresas até fechar o valor' }}
          </p>
        </div>
      </header>

      @if (lista.erro(); as mensagem) {
        <p class="nota nota-erro" role="alert"><ui-icone nome="alerta" [tamanho]="18" />{{ mensagem }}</p>
      } @else if (temAlguma() === false) {
        <div class="lista">
          <div class="vazio">
            <p class="vazio-titulo">Nenhuma negociação ainda</p>
            @if (auth.ehEmpresa()) {
              <p>Abra uma cotação e escolha "Negociar" numa proposta para começar.</p>
              <a class="botao botao-secundario" routerLink="/pages/consultar-cotacao">Ver cotações</a>
            } @else {
              <p>Quando uma empresa quiser negociar a sua proposta, a conversa aparece aqui.</p>
              <a class="botao botao-secundario" routerLink="/pages/mural-oportunidades">Ver o mural</a>
            }
          </div>
        </div>
      } @else if (lista.itens(); as itens) {
        <div class="filtros">
          <ui-segmentado rotulo="Mostrar" [opcoes]="filtros" [valor]="filtro()" (valorChange)="filtrar($event)" />
        </div>
        @if (itens.length > 0) {
          <app-lista-negociacoes [negociacoes]="itens" />
          <ui-carregar-mais [mostrando]="itens.length" [total]="lista.total()" [carregando]="lista.carregando()" [falha]="lista.falhaAoAtualizar()"
                            singular="negociação" plural="negociações" (carregar)="lista.carregarMais()" />
        } @else {
          <div class="lista"><p class="vazio">Nenhuma negociação em andamento.</p></div>
        }
      } @else {
        <div class="carregando-pagina" role="status"><span class="girando"></span><span class="visually-hidden">Carregando</span></div>
      }
    </div>
  `,
  styles: `
    .filtros {
      margin-bottom: var(--esp-4);
    }
  `,
})
export class Negociacoes implements OnInit {
  private readonly negociacaoService = inject(NegociacaoService);
  protected readonly auth = inject(AuthService);

  protected readonly filtro = signal<SituacaoNegociacao>('ANDAMENTO');
  protected readonly filtros: OpcaoSegmento<SituacaoNegociacao>[] = [
    { valor: 'ANDAMENTO', rotulo: 'Em andamento' },
    { valor: 'TODAS', rotulo: 'Todas' },
  ];
  /** Em andamento primeiro, depois as mais recentes, na ordem da API */
  protected readonly lista = new ListaPaginada<Negociacao>((pagina, tamanho) =>
    this.negociacaoService.minhas(this.filtro(), pagina, tamanho),
  );
  /** Se a organização já teve alguma negociação (decide entre o filtro e o estado vazio) */
  protected readonly temAlguma = signal<boolean | null>(null);
  private primeiraVez = true;

  constructor() {
    // Negociação nova, fechada ou encerrada aparece na lista sem recarregar a página
    aoReceberAviso(mudouNegociacao, () => {
      this.temAlguma.set(true);
      this.lista.recarregar();
    });
    // Sem nenhuma em andamento na primeira visita, mostra todas de uma vez
    effect(() => {
      const itens = this.lista.itens();
      if (!this.primeiraVez || itens === null) {
        return;
      }
      this.primeiraVez = false;
      if (itens.length > 0) {
        this.temAlguma.set(true);
        return;
      }
      untracked(() => {
        this.filtro.set('TODAS');
        this.lista.recomecar();
      });
    });
    effect(() => {
      if (!this.primeiraVez && this.filtro() === 'TODAS' && this.lista.itens()?.length === 0) {
        this.temAlguma.set(false);
      }
    });
  }

  ngOnInit(): void {
    this.lista.recomecar();
  }

  protected filtrar(filtro: SituacaoNegociacao | undefined): void {
    if (filtro && filtro !== this.filtro()) {
      this.filtro.set(filtro);
      this.lista.recomecar();
    }
  }
}
