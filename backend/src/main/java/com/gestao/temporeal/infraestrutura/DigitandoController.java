package com.gestao.temporeal.infraestrutura;

import com.gestao.compras.dominio.TipoRemetente;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import com.gestao.temporeal.aplicacao.EventoNegociacao;
import com.gestao.temporeal.aplicacao.porta.CanalTempoReal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
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

    private final CanalTempoReal canal;

    @MessageMapping("/negociacoes/{id}/digitando")
    public void digitando(@DestinationVariable UUID id, Principal usuario) {
        if (usuario instanceof JwtAuthenticationToken token) {
            canal.publicarNaNegociacao(id,
                    EventoNegociacao.digitando(TipoRemetente.de(UsuarioAtual.de(token.getToken()).tipo())));
        }
    }
}
