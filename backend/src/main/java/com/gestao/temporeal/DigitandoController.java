package com.gestao.temporeal;

import com.gestao.security.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * "Digitando…": o cliente manda para /app/negociacoes/{id}/digitando e a outra
 * parte recebe pelo tópico da negociação. Nada é gravado. A permissão já foi
 * conferida no {@link AutenticacaoStomp}.
 */
@Controller
@RequiredArgsConstructor
public class DigitandoController {

    private final SimpMessagingTemplate mensageiro;

    @MessageMapping("/negociacoes/{id}/digitando")
    public void digitando(@DestinationVariable UUID id, Principal usuario) {
        if (usuario instanceof JwtAuthenticationToken token) {
            mensageiro.convertAndSend(Destinos.negociacao(id),
                    EventoNegociacao.digitando(UsuarioAtual.de(token.getToken()).comoRemetente()));
        }
    }
}
