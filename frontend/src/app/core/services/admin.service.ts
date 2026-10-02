import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { OrganizacaoAdmin, Pagina } from '../models';

/** Ordens que a API aceita para a lista de organizações. */
export type OrdemOrganizacoes = 'criadaEm,desc' | 'razaoSocial,asc';

/** A área administrativa do superadmin (somente leitura nesta fase). */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  organizacoes(pagina: number, ordem: OrdemOrganizacoes, tamanho = 20): Observable<Pagina<OrganizacaoAdmin>> {
    const params = new HttpParams().set('page', pagina).set('size', tamanho).set('sort', ordem);
    return this.http.get<Pagina<OrganizacaoAdmin>>(`${API_URL}/admin/organizacoes`, { params });
  }
}
