package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.MuitasTentativasException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra força bruta: depois de 5 senhas erradas para o mesmo e-mail
 * em 15 minutos, o login fica bloqueado até a janela passar.
 *
 * Fica em memória, o que basta para uma instância só. Com várias instâncias,
 * o contador precisaria ir para um armazenamento compartilhado (ex.: Redis).
 */
@Component
public class TentativasLoginService {

    static final int MAXIMO_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> falhas = new ConcurrentHashMap<>();

    public void verificarBloqueio(String email) {
        Deque<Instant> registro = falhas.get(chave(email));
        if (registro == null) {
            return;
        }
        synchronized (registro) {
            descartarAntigas(registro);
            if (registro.size() >= MAXIMO_FALHAS) {
                long segundos = Math.max(1, Duration.between(Instant.now(), registro.peekFirst().plus(JANELA)).toSeconds());
                long minutos = Math.max(1, (segundos + 59) / 60);
                throw new MuitasTentativasException(
                        "Muitas tentativas de login. Tente novamente em " + minutos + " minuto(s).", segundos);
            }
        }
    }

    public void registrarFalha(String email) {
        Deque<Instant> registro = falhas.computeIfAbsent(chave(email), k -> new ArrayDeque<>());
        synchronized (registro) {
            descartarAntigas(registro);
            registro.addLast(Instant.now());
        }
    }

    public void limpar(String email) {
        falhas.remove(chave(email));
    }

    private void descartarAntigas(Deque<Instant> registro) {
        Instant limite = Instant.now().minus(JANELA);
        while (!registro.isEmpty() && registro.peekFirst().isBefore(limite)) {
            registro.pollFirst();
        }
    }

    private String chave(String email) {
        String normalizado = Documentos.normalizarEmail(email);
        return normalizado == null ? "" : normalizado;
    }
}
