import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { CadastroRequest, Usuario } from '../models';

@Injectable({ providedIn: 'root' })
export class CadastroService {
  private readonly http = inject(HttpClient);

  /** Cria a organização e a pessoa proprietária juntas. */
  cadastrar(dados: CadastroRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API_URL}/cadastro`, dados);
  }
}
