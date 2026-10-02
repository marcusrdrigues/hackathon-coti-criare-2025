package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.identidade.aplicacao.dto.TokenResponse;
import com.gestao.identidade.aplicacao.dto.UsuarioResponse;
import com.gestao.identidade.aplicacao.porta.EmissorDeToken;
import com.gestao.identidade.aplicacao.porta.EmpresaRepositorio;
import com.gestao.identidade.aplicacao.porta.FornecedorRepositorio;
import com.gestao.identidade.dominio.Empresa;
import com.gestao.identidade.dominio.Fornecedor;
import com.gestao.identidade.dominio.TipoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Email ou senha inválidos.";

    private final EmpresaRepositorio empresaRepositorio;
    private final FornecedorRepositorio fornecedorRepositorio;
    private final CredenciaisService credenciaisService;
    private final EmissorDeToken emissorDeToken;
    private final RefreshTokenService refreshTokenService;
    private final TentativasLoginService tentativas;

    /** Resultado de login/renovação: o corpo da resposta e o refresh token que vai para o cookie. */
    public record Sessao(TokenResponse resposta, String refreshToken) {}

    /**
     * Procura o e-mail entre empresas e fornecedores. A mensagem de erro é a
     * mesma para e-mail inexistente e senha errada, para não revelar quais
     * e-mails estão cadastrados.
     */
    @Transactional
    public Sessao login(String email, String senha) {
        tentativas.verificarBloqueio(email);
        String emailNormalizado = Documentos.normalizarEmail(email);

        Optional<Empresa> empresa = empresaRepositorio.buscarPorEmail(emailNormalizado);
        if (empresa.isPresent()) {
            Empresa e = empresa.get();
            e.setSenha(validarSenha(email, senha, e.getSenha()));
            return abrirSessao(paraResposta(e));
        }

        Optional<Fornecedor> fornecedor = fornecedorRepositorio.buscarPorEmail(emailNormalizado);
        if (fornecedor.isPresent()) {
            Fornecedor f = fornecedor.get();
            f.setSenha(validarSenha(email, senha, f.getSenha()));
            return abrirSessao(paraResposta(f));
        }

        tentativas.registrarFalha(email);
        throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
    }

    /** Troca o refresh token (do cookie) por um access token novo e um refresh token novo. */
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public Sessao renovar(String refreshToken) {
        RefreshTokenService.Rotacao rotacao = refreshTokenService.rotacionar(refreshToken);
        UsuarioResponse usuario = carregar(rotacao.usuarioId(), rotacao.tipo());
        EmissorDeToken.TokenAcesso acesso = emissorDeToken.gerar(usuario.id(), usuario.tipo());
        return new Sessao(new TokenResponse(acesso.valor(), "Bearer", acesso.expiraEmSegundos(), usuario),
                rotacao.novoToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revogar(refreshToken);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuario(UsuarioAutenticado atual) {
        return carregar(atual.id(), atual.tipo());
    }

    private Sessao abrirSessao(UsuarioResponse usuario) {
        tentativas.limpar(usuario.email());
        EmissorDeToken.TokenAcesso acesso = emissorDeToken.gerar(usuario.id(), usuario.tipo());
        String refresh = refreshTokenService.emitir(usuario.id(), usuario.tipo());
        return new Sessao(new TokenResponse(acesso.valor(), "Bearer", acesso.expiraEmSegundos(), usuario), refresh);
    }

    /**
     * Confere a senha e devolve o valor a gravar: o próprio hash, ou um hash
     * novo quando o cadastro antigo ainda tinha a senha em texto puro.
     */
    private String validarSenha(String email, String senhaDigitada, String senhaGravada) {
        if (!credenciaisService.senhaConfere(senhaDigitada, senhaGravada)) {
            tentativas.registrarFalha(email);
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        return credenciaisService.ehHashBcrypt(senhaGravada)
                ? senhaGravada
                : credenciaisService.gerarHash(senhaDigitada);
    }

    private UsuarioResponse carregar(UUID id, TipoUsuario tipo) {
        // Conta removida depois do login: a sessão deixa de valer
        return tipo == TipoUsuario.EMPRESA
                ? empresaRepositorio.buscarPorId(id).map(this::paraResposta)
                        .orElseThrow(() -> new NaoAutenticadoException("Conta não encontrada."))
                : fornecedorRepositorio.buscarPorId(id).map(this::paraResposta)
                        .orElseThrow(() -> new NaoAutenticadoException("Conta não encontrada."));
    }

    private UsuarioResponse paraResposta(Empresa e) {
        return new UsuarioResponse(e.getId(), e.getRazaoSocial(), e.getEmail(), e.getCnpj(), TipoUsuario.EMPRESA);
    }

    private UsuarioResponse paraResposta(Fornecedor f) {
        return new UsuarioResponse(f.getId(), f.getNomeCompleto(), f.getEmail(), f.getCnpj(), TipoUsuario.FORNECEDOR);
    }
}
