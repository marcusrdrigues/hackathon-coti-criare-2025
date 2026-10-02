import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { OrganizacaoAdmin } from '../../../core/models';
import { AdminService, OrdemOrganizacoes } from '../../../core/services/admin.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';
import { mensagemDeErro } from '../../../core/utils/erros';
import { iniciais } from '../../../core/utils/formatos';
import { CarregarMais } from '../../../ui/carregar-mais';
import { Icone } from '../../../ui/icone';
import { OpcaoSegmento, Segmentado } from '../../../ui/segmentado';

/**
 * Área administrativa do superadmin: as organizações da plataforma e quanto cada uma usa.
 * Somente leitura nesta fase; a auditoria vem na fase 5.
 */
@Component({
  selector: 'app-admin-organizacoes',
  imports: [DatePipe, CnpjPipe, CarregarMais, Icone, Segmentado],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-organizacoes.html',
  styleUrl: './admin-organizacoes.css',
})
export class AdminOrganizacoes implements OnInit {
  private readonly adminService = inject(AdminService);

  protected readonly iniciais = iniciais;
  protected readonly ordens: OpcaoSegmento<OrdemOrganizacoes>[] = [
    { valor: 'criadaEm,desc', rotulo: 'Mais recentes' },
    { valor: 'razaoSocial,asc', rotulo: 'Nome' },
  ];

  protected readonly ordem = signal<OrdemOrganizacoes>('criadaEm,desc');
  protected readonly organizacoes = signal<OrganizacaoAdmin[] | null>(null);
  protected readonly total = signal(0);
  protected readonly erro = signal<string | null>(null);
  protected readonly carregando = signal(false);
  private proximaPagina = 0;

  ngOnInit(): void {
    this.carregar();
  }

  protected ordenar(ordem: OrdemOrganizacoes | undefined): void {
    if (!ordem || ordem === this.ordem()) {
      return;
    }
    this.ordem.set(ordem);
    this.organizacoes.set(null);
    this.proximaPagina = 0;
    this.carregar();
  }

  /** Traz a próxima página e junta à lista que já está na tela. */
  protected carregar(): void {
    this.carregando.set(true);
    this.adminService.organizacoes(this.proximaPagina, this.ordem()).subscribe({
      next: (pagina) => {
        this.organizacoes.update((atual) => [...(atual ?? []), ...pagina.content]);
        this.total.set(pagina.page.totalElements);
        this.proximaPagina = pagina.page.number + 1;
        this.carregando.set(false);
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      },
    });
  }

  protected uso(o: OrganizacaoAdmin): string {
    return o.tipo === 'EMPRESA'
      ? `${o.cotacoes} ${o.cotacoes === 1 ? 'cotação' : 'cotações'}`
      : `${o.propostas} ${o.propostas === 1 ? 'proposta' : 'propostas'}`;
  }

  protected pessoas(o: OrganizacaoAdmin): string {
    return `${o.pessoas} ${o.pessoas === 1 ? 'pessoa' : 'pessoas'}`;
  }
}
