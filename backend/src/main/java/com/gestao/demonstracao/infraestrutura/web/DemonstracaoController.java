package com.gestao.demonstracao.infraestrutura.web;

import com.gestao.demonstracao.aplicacao.DadosDemonstracao;
import com.gestao.identidade.aplicacao.AuthService;
import com.gestao.identidade.aplicacao.TentativasLoginService;
import com.gestao.identidade.aplicacao.dto.TokenResponse;
import com.gestao.identidade.dominio.TipoUsuario;
import com.gestao.identidade.infraestrutura.seguranca.CookieDeSessao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Login de um clique nas contas de demonstração. Só existe com o profile
 * "demo": fora dele, estas rotas simplesmente não existem (404) e a tela de
 * login esconde os botões.
 */
@RestController
@Profile("demo")
@RequestMapping("/api/v1/auth/demo")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação de sessão e logout (JWT + refresh token em cookie HttpOnly)")
public class DemonstracaoController {

    private final AuthService authService;
    private final TentativasLoginService tentativas;
    private final CookieDeSessao cookieDeSessao;

    @Operation(summary = "Contas de demonstração", description = "Rota pública, disponível só no profile demo")
    @GetMapping
    public ResponseEntity<List<ContaDemoResponse>> contas() {
        return ResponseEntity.ok(List.of(
                new ContaDemoResponse(TipoUsuario.EMPRESA, "Criare Consulting",
                        "Publica cotações, compara propostas e negocia"),
                new ContaDemoResponse(TipoUsuario.FORNECEDOR, "Tech Soluções Ltda",
                        "Encontra oportunidades, envia propostas e negocia")));
    }

    @Operation(summary = "Entrar como conta de demonstração",
            description = "Rota pública, disponível só no profile demo. Usa o mesmo fluxo do login normal")
    @PostMapping("/{perfil}")
    public ResponseEntity<TokenResponse> entrar(@PathVariable TipoUsuario perfil) {
        String email = perfil == TipoUsuario.EMPRESA
                ? DadosDemonstracao.EMAIL_EMPRESA
                : DadosDemonstracao.EMAIL_FORNECEDOR;

        // Alguém pode ter errado a senha da conta demo de propósito; o botão continua funcionando
        tentativas.limpar(email);
        AuthService.Sessao sessao = authService.login(email, DadosDemonstracao.SENHA_DEMO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieDeSessao.criar(sessao.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(sessao.resposta());
    }
}
