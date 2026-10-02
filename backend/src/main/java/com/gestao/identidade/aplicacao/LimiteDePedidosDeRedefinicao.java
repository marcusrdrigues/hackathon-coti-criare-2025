package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Até 3 pedidos de redefinição por e-mail a cada hora (spec 004, R1): ninguém usa o portal
 * para encher a caixa de outra pessoa nem gasta a cota diária do provedor.
 *
 * <p>Fica em memória, como as tentativas de login, o que basta para uma instância só.
 */
@Component
class LimiteDePedidosDeRedefinicao {

    static final int MAXIMO = 3;
    static final Duration JANELA = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> pedidos = new ConcurrentHashMap<>();

    /** Registra o pedido se ainda couber na janela; devolve {@code false} quando passou do limite. */
    boolean permitir(String email) {
        Deque<Instant> registro = pedidos.computeIfAbsent(Documentos.normalizarEmail(email), k -> new ArrayDeque<>());
        synchronized (registro) {
            Instant agora = Instant.now();
            Instant limite = agora.minus(JANELA);
            while (!registro.isEmpty() && registro.peekFirst().isBefore(limite)) {
                registro.pollFirst();
            }
            if (registro.size() >= MAXIMO) {
                return false;
            }
            registro.addLast(agora);
            return true;
        }
    }
}
