import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { DashboardEmpresa, DashboardFornecedor } from '../models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/dashboard`;

  empresa(empresaId: string): Observable<DashboardEmpresa> {
    return this.http.get<DashboardEmpresa>(`${this.url}/empresa/${empresaId}`);
  }

  fornecedor(fornecedorId: string): Observable<DashboardFornecedor> {
    return this.http.get<DashboardFornecedor>(`${this.url}/fornecedor/${fornecedorId}`);
  }
}
