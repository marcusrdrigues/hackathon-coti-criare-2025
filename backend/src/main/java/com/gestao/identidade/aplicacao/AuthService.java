package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.EventoDeSeguranca.Tipo;
import com.gestao.compartilhado.dominio.MuitasTentativasException;
import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.identidade.aplicacao.dto.TokenResponse;
import com.gestao.identidade.aplicacao.dto.UsuarioResponse;
import com.gestao.identidade.aplicacao.porta.EmissorDeToken;
import com.gestao.identidade.aplicacao.porta.MembroRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Login, renovação e encerramento de sessão. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Email ou senha inválidos.";
    private static final String SEM_ACESSO = "Conta sem acesso. Fale com o proprietário da sua organização.";

    private final UsuarioRepositorio usuarioRepositorio;
    private final MembroRepositorio membroRepositorio;
    private final CredenciaisService credenciais;
    private final EmissorDeToken emissorDeToken;
    private final RefreshTokenService refreshTokenService;
    private final TentativasLoginService tentativas;
    private final EventosDaIdentidade eventos;

    /** Resultado de login/renovação: o corpo da resposta e o refresh token que vai para o cookie. */
    public record Sessao(TokenResponse resposta, String refreshToken) {}

    /**
     * A mensagem de erro é a mesma para e-mail inexistente e senha errada, para não
     * revelar quais e-mails estão cadastrados. Cada tentativa vira um evento de segurança,
     * gravado mesmo quando o login é desfeito.
     */
    @Transactional
    public Sessao login(String email, String senha) {
        Optional<Usuario> conta = usuarioRepositorio.buscarPorEmail(Documentos.normalizarEmail(email));
        try {
            tentativas.verificarBloqueio(email);
        } catch (MuitasTentativasException e) {
            eventos.publicarTentativa(Tipo.LOGIN_BLOQUEADO, conta, email, "Excesso de senhas erradas.");
            throw e;
        }
        Usuario usuario = conta
                .filter(u -> credenciais.senhaConfere(senha, u.getSenhaHash()))
                .orElseThrow(() -> {
                    tentativas.registrarFalha(email);
                    eventos.publicarTentativa(Tipo.LOGIN_FALHOU, conta, email,
                            conta.isPresent() ? "Senha errada." : "E-mail sem conta.");
                    return new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
                });
        TokenResponse resposta;
        try {
            resposta = resposta(usuario);
        } catch (NaoAutenticadoException e) {
            eventos.publicarTentativa(Tipo.LOGIN_FALHOU, conta, email, "Conta sem acesso.");
            throw e;
        }
        tentativas.limpar(email);
        eventos.publicar(Tipo.LOGIN, usuario, null);
        return new Sessao(resposta, refreshTokenService.emitir(usuario.getId()));
    }

    /** Troca o refresh token (do cookie) por um access token novo e um refresh token novo. */
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public Sessao renovar(String refreshToken) {
        RefreshTokenService.Rotacao rotacao = refreshTokenService.rotacionar(refreshToken);
        // Saiu da organização ou foi desativado depois do login: a sessão deixa de valer
        return new Sessao(resposta(pessoa(rotacao.usuarioId())), rotacao.novoToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revogar(refreshToken)
                .ifPresent(usuarioId -> eventos.publicar(Tipo.LOGOUT, usuarioId, null));
    }

    /** Abre a sessão de quem acabou de entrar na plataforma sem passar pelo login (ao aceitar um convite). */
    Sessao abrirSessao(UUID usuarioId) {
        return new Sessao(resposta(vinculoAtivo(usuarioId)), refreshTokenService.emitir(usuarioId));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuario(UsuarioAutenticado atual) {
        Usuario usuario = pessoa(atual.usuarioId());
        if (usuario.isSuperadmin()) {
            return paraRespostaDoSuperadmin(superadminAtivo(usuario));
        }
        return paraResposta(vinculoAtivo(usuario.getId()));
    }

    private Usuario pessoa(UUID usuarioId) {
        return usuarioRepositorio.buscarPorId(usuarioId).orElseThrow(() -> new NaoAutenticadoException(SEM_ACESSO));
    }

    /** O superadmin entra pela plataforma; as demais pessoas, pelo vínculo ativo com a organização. */
    private TokenResponse resposta(Usuario usuario) {
        if (!usuario.isSuperadmin()) {
            return resposta(vinculoAtivo(usuario.getId()));
        }
        EmissorDeToken.TokenAcesso acesso = emissorDeToken.gerar(
                UsuarioAutenticado.superadmin(superadminAtivo(usuario).getId()));
        return new TokenResponse(acesso.valor(), "Bearer", acesso.expiraEmSegundos(), paraRespostaDoSuperadmin(usuario));
    }

    /** Desativado quando sai da configuração do servidor: a sessão deixa de valer na próxima renovação. */
    private static Usuario superadminAtivo(Usuario usuario) {
        if (!usuario.ativo()) {
            throw new NaoAutenticadoException(SEM_ACESSO);
        }
        return usuario;
    }

    private Membro vinculoAtivo(UUID usuarioId) {
        return membroRepositorio.buscarAtivoDoUsuario(usuarioId)
                .filter(Membro::ativo)
                .orElseThrow(() -> new NaoAutenticadoException(SEM_ACESSO));
    }

    private TokenResponse resposta(Membro membro) {
        EmissorDeToken.TokenAcesso acesso = emissorDeToken.gerar(UsuarioAutenticado.de(membro));
        return new TokenResponse(acesso.valor(), "Bearer", acesso.expiraEmSegundos(), paraResposta(membro));
    }

    private static UsuarioResponse paraRespostaDoSuperadmin(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), null, Papel.SUPERADMIN, null);
    }

    static UsuarioResponse paraResposta(Membro membro) {
        Usuario usuario = membro.getUsuario();
        Organizacao organizacao = membro.getOrganizacao();
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), organizacao.getTipo(),
                membro.getPapel(), new UsuarioResponse.OrganizacaoResumo(
                        organizacao.getId(), organizacao.getRazaoSocial(), organizacao.getCnpj()));
    }
}
