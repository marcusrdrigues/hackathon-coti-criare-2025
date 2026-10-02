package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.ConviteRepositorio;
import com.gestao.identidade.dominio.Convite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * O outro lado do convite: quem recebeu o link confere de onde ele veio e, definindo a
 * senha, entra na organização como membro, já com a sessão aberta.
 */
@Service
@RequiredArgsConstructor
public class ConviteService {

    static final String INVALIDO = "Este convite não é válido. Peça um novo a quem convidou.";
    static final String VENCIDO = "Este convite venceu. Peça um novo a quem convidou.";
    static final String USADO = "Este convite já foi usado. Se a conta é sua, entre com o seu e-mail e senha.";

    private final ConviteRepositorio conviteRepositorio;
    private final CadastroService cadastroService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public Convite consultar(String token) {
        return pendente(token, Instant.now());
    }

    /** Cria a pessoa, o vínculo de membro e a sessão, numa transação só. */
    @Transactional
    public AuthService.Sessao aceitar(String token, String nome, String senha) {
        Instant agora = Instant.now();
        Convite convite = pendente(token, agora);
        UsuarioAutenticado membro = cadastroService.adicionarMembro(
                convite.getOrganizacao().getId(), nome, convite.getEmail(), senha);
        convite.aceitar(agora);
        return authService.abrirSessao(membro.usuarioId());
    }

    private Convite pendente(String token, Instant agora) {
        if (token == null || token.isBlank()) {
            throw new RecursoNaoEncontradoException(INVALIDO);
        }
        Convite convite = conviteRepositorio.buscarPorHash(TokenAleatorio.hash(token.trim()))
                .orElseThrow(() -> new RecursoNaoEncontradoException(INVALIDO));
        return switch (convite.situacao(agora)) {
            case PENDENTE -> convite;
            case VENCIDO -> throw new RecursoNaoEncontradoException(VENCIDO);
            case ACEITO -> throw new RecursoNaoEncontradoException(USADO);
            case CANCELADO -> throw new RecursoNaoEncontradoException(INVALIDO);
        };
    }
}
