package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.Mascaras;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * O superadmin da plataforma nasce só da configuração do servidor (spec 001, R3).
 * Na subida, a configuração é a fonte da verdade: o superadmin configurado fica ativo
 * com a senha configurada, e qualquer outro superadmin é desativado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperadminService {

    public static final int TAMANHO_MINIMO_SENHA = 12;
    static final String NOME = "Superadmin";

    private final UsuarioRepositorio usuarioRepositorio;
    private final CredenciaisService credenciais;
    private final RefreshTokenService refreshTokenService;

    /** E-mail e senha vindos da configuração do servidor. */
    public record Configuracao(String email, String senha) {

        @Override
        public String toString() {
            return "Configuracao[email=" + Mascaras.email(email) + ", senha=***]";
        }
    }

    /**
     * Deixa a plataforma com exatamente o superadmin configurado, ou com nenhum ativo
     * quando a configuração está vazia ou é inválida.
     *
     * @return o superadmin ativo depois da sincronização, se houver
     */
    @Transactional
    public Optional<Usuario> sincronizar(Optional<Configuracao> configuracao) {
        Optional<Configuracao> valida = configuracao.filter(SuperadminService::valida);
        String email = valida.map(c -> Documentos.normalizarEmail(c.email())).orElse(null);

        // Quem não bate com a configuração deixa de ser superadmin ativo, e as sessões acabam na hora
        for (Usuario antigo : usuarioRepositorio.listarSuperadmins()) {
            if (antigo.ativo() && !antigo.getEmail().equals(email)) {
                antigo.desativar();
                refreshTokenService.revogarTodas(antigo.getId());
                log.warn("Superadmin {} desativado: não está mais na configuração do servidor.",
                        Mascaras.email(antigo.getEmail()));
            }
        }
        return valida.flatMap(c -> garantir(email, c.senha()));
    }

    private Optional<Usuario> garantir(String email, String senha) {
        Optional<Usuario> existente = usuarioRepositorio.buscarPorEmail(email);
        if (existente.isEmpty()) {
            Usuario novo = usuarioRepositorio.salvar(Usuario.superadmin(NOME, email, credenciais.gerarHash(senha)));
            log.info("Superadmin {} criado a partir da configuração do servidor.", Mascaras.email(email));
            return Optional.of(novo);
        }
        Usuario usuario = existente.get();
        if (!usuario.isSuperadmin()) {
            // Nunca promove uma conta de organização: o e-mail configurado está errado ou foi tomado
            log.error("SUPERADMIN_EMAIL ({}) já é de uma pessoa de uma organização. Nenhum superadmin foi criado.",
                    Mascaras.email(email));
            return Optional.empty();
        }
        if (!credenciais.senhaConfere(senha, usuario.getSenhaHash())) {
            usuario.trocarSenha(credenciais.gerarHash(senha));
            refreshTokenService.revogarTodas(usuario.getId());
            log.info("Senha do superadmin {} atualizada a partir da configuração.", Mascaras.email(email));
        }
        usuario.reativar();
        return Optional.of(usuario);
    }

    private static boolean valida(Configuracao configuracao) {
        boolean emailPreenchido = configuracao.email() != null && !configuracao.email().isBlank();
        boolean senhaPreenchida = configuracao.senha() != null && !configuracao.senha().isBlank();
        if (!emailPreenchido && !senhaPreenchida) {
            return false;
        }
        if (!emailPreenchido || !senhaPreenchida) {
            log.error("Superadmin não configurado: defina SUPERADMIN_EMAIL e SUPERADMIN_SENHA juntos.");
            return false;
        }
        if (configuracao.senha().length() < TAMANHO_MINIMO_SENHA) {
            log.error("Superadmin não configurado: SUPERADMIN_SENHA precisa de pelo menos {} caracteres.",
                    TAMANHO_MINIMO_SENHA);
            return false;
        }
        return true;
    }
}
