import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { OrganizacaoAdmin } from '../../../core/models';
import { AdminService, OrdemOrganizacoes } from '../../../core/services/admin.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';
import { iniciais } from '../../../core/utils/formatos';
import { ListaPaginada } from '../../../core/utils/lista-paginada';
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
  protected readonly lista = new ListaPaginada<OrganizacaoAdmin>((pagina, tamanho) =>
    this.adminService.organizacoes(pagina, this.ordem(), tamanho),
  );

  ngOnInit(): void {
    this.lista.recomecar();
  }

  protected ordenar(ordem: OrdemOrganizacoes | undefined): void {
    if (ordem && ordem !== this.ordem()) {
      this.ordem.set(ordem);
      this.lista.recomecar();
    }
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
