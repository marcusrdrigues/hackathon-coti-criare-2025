package com.gestao.identidade.infraestrutura.seguranca;

import com.gestao.identidade.aplicacao.porta.EmissorDeToken;
import com.gestao.identidade.dominio.TipoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Emite os access tokens como JWT assinado com HMAC-SHA256 (adaptador da porta {@link EmissorDeToken}). */
@Service
@RequiredArgsConstructor
public class EmissorDeTokenJwt implements EmissorDeToken {

    public static final String CLAIM_TIPO = "tipo";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propriedades;

    @Override
    public TokenAcesso gerar(UUID usuarioId, TipoUsuario tipo) {
        Instant agora = Instant.now();

        // Só o necessário: quem é (sub) e o perfil. Nada de dado pessoal no token.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.issuer())
                .subject(usuarioId.toString())
                .claim(CLAIM_TIPO, tipo.name())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.expiracaoAcesso()))
                .id(UUID.randomUUID().toString())
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenAcesso(token, propriedades.expiracaoAcesso().toSeconds());
    }
}
