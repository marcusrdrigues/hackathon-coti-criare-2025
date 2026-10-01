package com.gestao.services;

import com.gestao.dtos.auth.TokenResponse;
import com.gestao.dtos.auth.UsuarioResponse;
import com.gestao.entities.Empresa;
import com.gestao.entities.Fornecedor;
import com.gestao.enums.TipoUsuario;
import com.gestao.exceptions.UnauthorizedException;
import com.gestao.repositories.EmpresaRepository;
import com.gestao.repositories.FornecedorRepository;
import com.gestao.security.RefreshTokenService;
import com.gestao.security.TentativasLoginService;
import com.gestao.security.TokenService;
import com.gestao.security.UsuarioAutenticado;
import com.gestao.utils.Documentos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Email ou senha inválidos.";

    private final EmpresaRepository empresaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final CredenciaisService credenciaisService;
    private final TokenService tokenService;
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

        Optional<Empresa> empresa = empresaRepository.findByEmail(emailNormalizado);
        if (empresa.isPresent()) {
            Empresa e = empresa.get();
            e.setSenha(validarSenha(email, senha, e.getSenha()));
            return abrirSessao(paraResposta(e));
        }

        Optional<Fornecedor> fornecedor = fornecedorRepository.findByEmail(emailNormalizado);
        if (fornecedor.isPresent()) {
            Fornecedor f = fornecedor.get();
            f.setSenha(validarSenha(email, senha, f.getSenha()));
            return abrirSessao(paraResposta(f));
        }

        tentativas.registrarFalha(email);
        throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
    }

    /** Troca o refresh token (do cookie) por um access token novo e um refresh token novo. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Sessao renovar(String refreshToken) {
        RefreshTokenService.Rotacao rotacao = refreshTokenService.rotacionar(refreshToken);
        UsuarioResponse usuario = carregar(rotacao.usuarioId(), rotacao.tipo());
        TokenService.TokenAcesso acesso = tokenService.gerar(usuario.id(), usuario.tipo());
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
        TokenService.TokenAcesso acesso = tokenService.gerar(usuario.id(), usuario.tipo());
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
            throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
        }
        return credenciaisService.ehHashBcrypt(senhaGravada)
                ? senhaGravada
                : credenciaisService.gerarHash(senhaDigitada);
    }

    private UsuarioResponse carregar(UUID id, TipoUsuario tipo) {
        // Conta removida depois do login: a sessão deixa de valer
        return tipo == TipoUsuario.EMPRESA
                ? empresaRepository.findById(id).map(this::paraResposta)
                        .orElseThrow(() -> new UnauthorizedException("Conta não encontrada."))
                : fornecedorRepository.findById(id).map(this::paraResposta)
                        .orElseThrow(() -> new UnauthorizedException("Conta não encontrada."));
    }

    private UsuarioResponse paraResposta(Empresa e) {
        return new UsuarioResponse(e.getId(), e.getRazaoSocial(), e.getEmail(), e.getCnpj(), TipoUsuario.EMPRESA);
    }

    private UsuarioResponse paraResposta(Fornecedor f) {
        return new UsuarioResponse(f.getId(), f.getNomeCompleto(), f.getEmail(), f.getCnpj(), TipoUsuario.FORNECEDOR);
    }
}
