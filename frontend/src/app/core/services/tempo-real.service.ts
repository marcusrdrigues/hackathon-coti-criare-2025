import { Injectable, computed, inject, signal } from '@angular/core';
import { Client, IMessage, ReconnectionTimeMode, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject, firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Mensagem, Negociacao, TipoUsuario } from '../models';
import { AuthService } from './auth.service';

/** O que chega em /topic/negociacoes/{id} (ver EventoNegociacao.java). */
export interface EventoNegociacao {
  tipo: 'MENSAGEM' | 'STATUS' | 'DIGITANDO';
  mensagem: Mensagem | null;
  negociacao: Negociacao | null;
  remetente: TipoUsuario | null;
}

export type EstadoConexao = 'desligado' | 'conectando' | 'conectado';

/** Renova o token se faltar menos que isso para ele vencer. */
const MARGEM_TOKEN_MS = 60_000;

interface Assinatura {
  mensagens: Subject<IMessage>;
  stomp: StompSubscription | null;
  usos: number;
}

/**
 * Conexão em tempo real com a API (STOMP sobre WebSocket, ver docs/adr/0012).
 *
 * - Liga quando há sessão e desliga no logout (quem controla é a estrutura das telas internas)
 * - Reconecta sozinha, com espera crescente, e renova o token antes de cada conexão
 * - As telas assinam destinos como Observables; ao reconectar, as assinaturas são refeitas
 */
@Injectable({ providedIn: 'root' })
export class TempoRealService {
  private readonly auth = inject(AuthService);

  private cliente: Client | null = null;
  private readonly assinaturas = new Map<string, Assinatura>();

  readonly estado = signal<EstadoConexao>('desligado');
  readonly conectado = computed(() => this.estado() === 'conectado');

  ligar(): void {
    if (this.cliente) {
      return;
    }
    const cliente = new Client({
      brokerURL: environment.wsUrl,
      reconnectDelay: 1_000,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      maxReconnectDelay: 30_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => undefined,
      beforeConnect: async (c) => {
        this.estado.set('conectando');
        const token = await this.tokenValido();
        if (!token) {
          // Sem sessão para renovar: não adianta insistir
          await c.deactivate();
          this.estado.set('desligado');
          return;
        }
        c.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => {
        this.estado.set('conectado');
        for (const [destino, assinatura] of this.assinaturas) {
          assinatura.stomp = cliente.subscribe(destino, (m) => assinatura.mensagens.next(m));
        }
      },
      onWebSocketClose: () => {
        for (const assinatura of this.assinaturas.values()) {
          assinatura.stomp = null;
        }
        if (this.cliente === cliente) {
          this.estado.set('conectando');
        }
      },
    });
    this.cliente = cliente;
    cliente.activate();
  }

  desligar(): void {
    const cliente = this.cliente;
    this.cliente = null;
    this.estado.set('desligado');
    void cliente?.deactivate();
  }

  /** Eventos de uma negociação: mensagens, mudanças de status e "digitando". */
  negociacao(id: string): Observable<EventoNegociacao> {
    return this.assinar<EventoNegociacao>(`/topic/negociacoes/${id}`);
  }

  /** Avisa a outra parte que este usuário está escrevendo. */
  avisarDigitando(negociacaoId: string): void {
    if (this.cliente?.connected) {
      this.cliente.publish({ destination: `/app/negociacoes/${negociacaoId}/digitando`, body: '' });
    }
  }

  /** Assina um destino; várias telas podem assinar o mesmo, e a assinatura STOMP é uma só. */
  assinar<T>(destino: string): Observable<T> {
    return new Observable<T>((observador) => {
      let assinatura = this.assinaturas.get(destino);
      if (!assinatura) {
        assinatura = { mensagens: new Subject<IMessage>(), stomp: null, usos: 0 };
        this.assinaturas.set(destino, assinatura);
      }
      const atual = assinatura;
      atual.usos++;
      if (this.cliente?.connected && !atual.stomp) {
        atual.stomp = this.cliente.subscribe(destino, (m) => atual.mensagens.next(m));
      }

      const leitura = atual.mensagens.subscribe((m) => {
        try {
          observador.next(JSON.parse(m.body) as T);
        } catch {
          // Mensagem malformada: ignora, a tela se corrige na próxima leitura
        }
      });

      return () => {
        leitura.unsubscribe();
        atual.usos--;
        if (atual.usos === 0) {
          if (this.cliente?.connected) {
            atual.stomp?.unsubscribe();
          }
          this.assinaturas.delete(destino);
        }
      };
    });
  }

  /** Token de acesso com folga de validade; renova pelo cookie quando precisa. */
  private async tokenValido(): Promise<string | null> {
    const token = this.auth.token;
    if (token && expiraEm(token) - Date.now() > MARGEM_TOKEN_MS) {
      return token;
    }
    if (!this.auth.logado()) {
      return null;
    }
    try {
      return await firstValueFrom(this.auth.renovarSessao());
    } catch {
      return null;
    }
  }
}

/** Momento (ms) em que o JWT vence, lido do campo "exp" sem validar a assinatura. */
export function expiraEm(token: string): number {
  try {
    const corpo = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const { exp } = JSON.parse(atob(corpo)) as { exp?: number };
    return typeof exp === 'number' ? exp * 1000 : 0;
  } catch {
    return 0;
  }
}
