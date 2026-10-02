package com.gestao.identidade.infraestrutura.seguranca;

import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.aplicacao.porta.EmissorDeToken;
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

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propriedades;

    @Override
    public TokenAcesso gerar(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();

        // Só identificadores: a pessoa (sub), a organização, o lado no negócio e o papel. Nada de dado pessoal.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.issuer())
                .subject(usuario.usuarioId().toString())
                .claim(ClaimsDoToken.ORGANIZACAO, usuario.organizacaoId().toString())
                .claim(ClaimsDoToken.TIPO, usuario.tipo().name())
                .claim(ClaimsDoToken.PAPEL, usuario.papel().name())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.expiracaoAcesso()))
                .id(UUID.randomUUID().toString())
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenAcesso(token, propriedades.expiracaoAcesso().toSeconds());
    }
}
