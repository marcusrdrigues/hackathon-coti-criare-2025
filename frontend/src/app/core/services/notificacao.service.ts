import { Injectable, signal } from '@angular/core';

export interface Notificacao {
  id: number;
  tipo: 'sucesso' | 'erro' | 'info' | 'aviso';
  texto: string;
  /** Quem ou o que originou o aviso (avisos ao vivo) */
  titulo?: string;
  /** Rota para abrir o assunto do aviso */
  link?: string | null;
}

const MAXIMO_NA_TELA = 3;

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

  /** Algo que aconteceu agora em outra tela: nova mensagem, proposta, negociação. */
  aviso(titulo: string, texto: string, link: string | null): void {
    this.adicionar('aviso', texto, 7000, { titulo, link });
  }

  fechar(id: number): void {
    this.notificacoes.update((lista) => lista.filter((n) => n.id !== id));
  }

  private adicionar(
    tipo: Notificacao['tipo'],
    texto: string,
    duracaoMs = 4000,
    extras: Pick<Notificacao, 'titulo' | 'link'> = {},
  ): void {
    const id = this.proximoId++;
    // Com muitos avisos seguidos, ficam só os mais novos na tela
    this.notificacoes.update((lista) => [...lista.slice(-(MAXIMO_NA_TELA - 1)), { id, tipo, texto, ...extras }]);
    setTimeout(() => this.fechar(id), duracaoMs);
  }
}
