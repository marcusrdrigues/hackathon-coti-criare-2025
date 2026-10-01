import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { EmpresaCadastroRequest, FornecedorCadastroRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class CadastroService {
  private readonly http = inject(HttpClient);

  cadastrarEmpresa(dados: EmpresaCadastroRequest): Observable<unknown> {
    return this.http.post(`${API_URL}/empresas`, dados);
  }

  cadastrarFornecedor(dados: FornecedorCadastroRequest): Observable<unknown> {
    return this.http.post(`${API_URL}/fornecedores`, dados);
  }
}
