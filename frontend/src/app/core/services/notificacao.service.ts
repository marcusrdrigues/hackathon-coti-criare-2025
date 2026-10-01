import { Injectable, signal } from '@angular/core';

export interface Notificacao {
  id: number;
  tipo: 'sucesso' | 'erro' | 'info';
  texto: string;
}

/** Avisos rápidos (toasts) no canto da tela, no lugar dos alert() do navegador. */
@Injectable({ providedIn: 'root' })
export class NotificacaoService {
  private proximoId = 1;
  readonly notificacoes = signal<Notificacao[]>([]);

  sucesso(texto: string): void {
    this.adicionar('sucesso', texto);
  }

  erro(texto: string): void {
    this.adicionar('erro', texto, 6000);
  }

  info(texto: string): void {
    this.adicionar('info', texto);
  }

  fechar(id: number): void {
    this.notificacoes.update((lista) => lista.filter((n) => n.id !== id));
  }

  private adicionar(tipo: Notificacao['tipo'], texto: string, duracaoMs = 4000): void {
    const id = this.proximoId++;
    this.notificacoes.update((lista) => [...lista, { id, tipo, texto }]);
    setTimeout(() => this.fechar(id), duracaoMs);
  }
}
