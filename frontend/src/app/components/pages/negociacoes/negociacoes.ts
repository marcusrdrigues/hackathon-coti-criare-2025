import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Negociacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { NegociacaoService } from '../../../core/services/negociacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { Icone } from '../../../ui/icone';
import { OpcaoSegmento, Segmentado } from '../../../ui/segmentado';
import { ListaNegociacoes, ordenarNegociacoes } from '../../shared/lista-negociacoes/lista-negociacoes';

type Filtro = 'ANDAMENTO' | 'TODAS';

/** Todas as negociações do usuário, dos dois perfis. */
@Component({
  selector: 'app-negociacoes',
  imports: [RouterLink, Icone, Segmentado, ListaNegociacoes],
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

      @if (erro(); as mensagem) {
        <p class="nota nota-erro" role="alert"><ui-icone nome="alerta" [tamanho]="18" />{{ mensagem }}</p>
      } @else if (negociacoes(); as lista) {
        @if (lista.length > 0) {
          <div class="filtros">
            <ui-segmentado rotulo="Mostrar" [opcoes]="filtros()" [(valor)]="filtro" />
          </div>
          @if (filtradas().length > 0) {
            <app-lista-negociacoes [negociacoes]="filtradas()" />
          } @else {
            <div class="lista"><p class="vazio">Nenhuma negociação em andamento.</p></div>
          }
        } @else {
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

  protected readonly negociacoes = signal<Negociacao[] | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly filtro = signal<Filtro>('ANDAMENTO');

  protected readonly filtros = computed<OpcaoSegmento<Filtro>[]>(() => {
    const lista = this.negociacoes() ?? [];
    return [
      { valor: 'ANDAMENTO', rotulo: 'Em andamento', contagem: lista.filter((n) => n.status === 'EM_ANDAMENTO').length },
      { valor: 'TODAS', rotulo: 'Todas', contagem: lista.length },
    ];
  });

  protected readonly filtradas = computed(() => {
    const lista = this.negociacoes() ?? [];
    return this.filtro() === 'TODAS' ? lista : lista.filter((n) => n.status === 'EM_ANDAMENTO');
  });

  ngOnInit(): void {
    this.negociacaoService.listarMinhas().subscribe({
      next: (lista) => {
        const ordenada = ordenarNegociacoes(lista);
        this.negociacoes.set(ordenada);
        // Sem nenhuma em andamento, mostra todas de uma vez
        if (!ordenada.some((n) => n.status === 'EM_ANDAMENTO')) {
          this.filtro.set('TODAS');
        }
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
