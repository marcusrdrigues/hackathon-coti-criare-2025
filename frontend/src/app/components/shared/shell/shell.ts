import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvisosService } from '../../../core/services/avisos.service';
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
  /** Mostra o total de mensagens não lidas */
  comNaoLidas?: boolean;
}

const LARGURA_COM_LATERAL = '(min-width: 1024px)';

/** Onde fica a escolha de recolher a barra lateral (só neste navegador). */
const CHAVE_LATERAL = 'barra_lateral';

function lateralRecolhida(): boolean {
  try {
    return localStorage.getItem(CHAVE_LATERAL) === 'recolhida';
  } catch {
    return false;
  }
}

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
  protected readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);

  protected readonly usuario = this.auth.usuario;
  protected readonly iniciais = computed(() => iniciais(this.usuario()?.nome ?? ''));
  protected readonly perfil = computed(() => (this.auth.ehEmpresa() ? 'Empresa' : 'Fornecedor'));
  protected readonly papel = computed(() => (this.usuario()?.papel === 'PROPRIETARIO' ? 'Proprietário' : 'Membro'));
  /** O superadmin não tem organização: navegação própria, sem equipe, sem abas e sem tempo real. */
  protected readonly superadmin = this.auth.ehSuperadmin;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map((e) => (e as NavigationEnd).urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly destinos = computed<Destino[]>(() => {
    if (this.superadmin()) {
      return [{ rota: '/pages/admin', rotulo: 'Organizações', icone: 'organizacoes' }];
    }
    return this.auth.ehEmpresa()
      ? [
          { rota: '/pages/dashboard', rotulo: 'Início', icone: 'inicio' },
          {
            rota: '/pages/consultar-cotacao',
            rotulo: 'Cotações',
            icone: 'cotacoes',
            inclui: ['/pages/detalhe-cotacao', '/pages/cadastro-cotacao'],
          },
          {
            rota: '/pages/negociacoes',
            rotulo: 'Negociações',
            icone: 'negociacoes',
            inclui: ['/pages/negociacao/'],
            comNaoLidas: true,
          },
        ]
      : [
          { rota: '/pages/dashboard-fornecedor', rotulo: 'Início', icone: 'inicio' },
          { rota: '/pages/mural-oportunidades', rotulo: 'Mural', icone: 'mural' },
          {
            rota: '/pages/negociacoes',
            rotulo: 'Negociações',
            icone: 'negociacoes',
            inclui: ['/pages/negociacao/'],
            comNaoLidas: true,
          },
          { rota: '/pages/propostas-enviadas', rotulo: 'Propostas', icone: 'propostas', inclui: ['/pages/historico-propostas'] },
        ];
  });

  /** Com um destino só, a barra de abas não ajuda: o celular fica só com o topo. */
  protected readonly comAbas = computed(() => this.destinos().length > 1);

  /** Barra lateral só quando há largura para ela; abaixo disso, abas */
  protected readonly comLateral = signal(true);
  /** Barra lateral só com ícones. Nunca começa recolhida para quem não escolheu (some a descoberta). */
  protected readonly recolhida = signal(lateralRecolhida());

  constructor() {
    // Tempo real (e os avisos que vêm por ele) enquanto houver sessão nas telas internas
    const tempoReal = inject(TempoRealService);
    effect(() => {
      if (this.auth.logado() && !this.superadmin()) {
        tempoReal.ligar();
        this.avisos.iniciar();
      } else {
        this.avisos.parar();
        tempoReal.desligar();
      }
    });
    inject(DestroyRef).onDestroy(() => {
      this.avisos.parar();
      tempoReal.desligar();
    });

    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      const midia = window.matchMedia(LARGURA_COM_LATERAL);
      this.comLateral.set(midia.matches);
      const aoMudar = (e: MediaQueryListEvent) => this.comLateral.set(e.matches);
      midia.addEventListener('change', aoMudar);
      inject(DestroyRef).onDestroy(() => midia.removeEventListener('change', aoMudar));
    }
  }

  protected readonly naEquipe = computed(() => this.url().split('?')[0] === '/pages/equipe');

  protected ativo(destino: Destino): boolean {
    const url = this.url().split('?')[0];
    return url === destino.rota || (destino.inclui ?? []).some((rota) => url.startsWith(rota));
  }

  /** Total que aparece no destino ("99+" acima disso) */
  protected contador(total: number): string {
    return total > 99 ? '99+' : String(total);
  }

  protected alternarLateral(): void {
    this.recolhida.update((recolhida) => !recolhida);
    try {
      localStorage.setItem(CHAVE_LATERAL, this.recolhida() ? 'recolhida' : 'aberta');
    } catch {
      // Sem armazenamento: a escolha vale só até recarregar
    }
  }

  protected sair(): void {
    this.auth.logout();
  }
}
