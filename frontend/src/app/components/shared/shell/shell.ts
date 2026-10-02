import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { TempoRealService } from '../../../core/services/tempo-real.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';
import { iniciais } from '../../../core/utils/formatos';
import { Icone, NomeIcone } from '../../../ui/icone';
import { Menu } from '../../../ui/menu';
import { SeletorTema } from '../seletor-tema/seletor-tema';

interface Destino {
  rota: string;
  rotulo: string;
  icone: NomeIcone;
  /** Outras rotas que pertencem a este destino (ex.: o detalhe de uma cotação) */
  inclui?: string[];
}

const LARGURA_COM_LATERAL = '(min-width: 1024px)';

/**
 * Estrutura das telas internas: barra lateral no computador (como no Mail e no
 * Notes) e, no celular, barra de topo com a conta e barra de abas embaixo.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, NgTemplateOutlet, Icone, Menu, SeletorTema, CnpjPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly usuario = this.auth.usuario;
  protected readonly iniciais = computed(() => iniciais(this.usuario()?.nome ?? ''));
  protected readonly perfil = computed(() => (this.auth.ehEmpresa() ? 'Empresa' : 'Fornecedor'));

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map((e) => (e as NavigationEnd).urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly destinos = computed<Destino[]>(() =>
    this.auth.ehEmpresa()
      ? [
          { rota: '/pages/dashboard', rotulo: 'Início', icone: 'inicio' },
          {
            rota: '/pages/consultar-cotacao',
            rotulo: 'Cotações',
            icone: 'cotacoes',
            inclui: ['/pages/detalhe-cotacao', '/pages/cadastro-cotacao'],
          },
          { rota: '/pages/negociacoes', rotulo: 'Negociações', icone: 'negociacoes', inclui: ['/pages/negociacao/'] },
        ]
      : [
          { rota: '/pages/dashboard-fornecedor', rotulo: 'Início', icone: 'inicio' },
          { rota: '/pages/mural-oportunidades', rotulo: 'Mural', icone: 'mural' },
          { rota: '/pages/negociacoes', rotulo: 'Negociações', icone: 'negociacoes', inclui: ['/pages/negociacao/'] },
          { rota: '/pages/propostas-enviadas', rotulo: 'Propostas', icone: 'propostas', inclui: ['/pages/historico-propostas'] },
        ],
  );

  /** Barra lateral só quando há largura para ela; abaixo disso, abas */
  protected readonly comLateral = signal(true);

  constructor() {
    // Tempo real enquanto houver sessão nas telas internas
    const tempoReal = inject(TempoRealService);
    effect(() => (this.auth.logado() ? tempoReal.ligar() : tempoReal.desligar()));
    inject(DestroyRef).onDestroy(() => tempoReal.desligar());

    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      const midia = window.matchMedia(LARGURA_COM_LATERAL);
      this.comLateral.set(midia.matches);
      const aoMudar = (e: MediaQueryListEvent) => this.comLateral.set(e.matches);
      midia.addEventListener('change', aoMudar);
      inject(DestroyRef).onDestroy(() => midia.removeEventListener('change', aoMudar));
    }
  }

  protected ativo(destino: Destino): boolean {
    const url = this.url().split('?')[0];
    return url === destino.rota || (destino.inclui ?? []).some((rota) => url.startsWith(rota));
  }

  protected sair(): void {
    this.auth.logout();
  }
}
