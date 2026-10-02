package com.gestao.security;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Empresa;
import com.gestao.entities.Fornecedor;
import com.gestao.entities.Negociacao;
import com.gestao.entities.Proposta;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.TipoUsuario;
import com.gestao.services.CotacaoService;
import com.gestao.services.EmpresaService;
import com.gestao.services.FornecedorService;
import com.gestao.services.MensagemNegociacaoService;
import com.gestao.services.NegociacaoService;
import com.gestao.services.PropostaService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a segurança pela camada HTTP: filtros do Spring Security, @PreAuthorize,
 * regras de posse nos services e o fluxo do cookie de refresh token.
 */
@SpringBootTest
@Transactional
class SegurancaApiTest {

    private static final String SENHA = "segredo123";

    @Autowired private WebApplicationContext contexto;
    @Autowired private EmpresaService empresaService;
    @Autowired private FornecedorService fornecedorService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private NegociacaoService negociacaoService;
    @Autowired private MensagemNegociacaoService mensagemService;

    private MockMvc mvc;
    private Empresa criare;
    private Fornecedor tech;
    private Cotacao cotacaoDaOutraEmpresa;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        criare = empresaService.cadastrarEmpresa(empresa("Criare Consulting", "11.222.333/0001-81", "empresa@api.com"));
        Empresa outra = empresaService.cadastrarEmpresa(empresa("Outra SA", "90.817.263/0001-80", "outra@api.com"));
        tech = fornecedorService.cadastrarFornecedor(fornecedor("Tech Soluções", "45.236.789/0001-12", "fornecedor@api.com"));

        Cotacao c = new Cotacao();
        c.setNomeServico("Notebooks");
        c.setRequisitos("10 unidades");
        c.setCategoria(CategoriaCotacao.TECNOLOGIA);
        c.setDataLimite(LocalDateTime.now().plusDays(5));
        cotacaoDaOutraEmpresa = cotacaoService.criarCotacao(c, outra.getId());
    }

    @Test
    void rotaProtegidaSemTokenRetorna401() throws Exception {
        mvc.perform(get("/api/v1/cotacoes/abertas"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mvc.perform(get("/api/v1/cotacoes/abertas").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token inválido ou expirado."));
    }

    @Test
    void rotasPublicasNaoExigemToken() throws Exception {
        mvc.perform(get("/api/v1/cotacoes/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    @Test
    void loginDevolveAccessTokenECookieHttpOnly() throws Exception {
        MvcResult resultado = login("empresa@api.com");

        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("refresh_token=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/v1/auth");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain(cookie(resultado).getValue());

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(resultado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("empresa@api.com"))
                .andExpect(jsonPath("$.tipo").value("EMPRESA"));
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"empresa@api.com\",\"senha\":\"errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha inválidos."));
    }

    @Test
    void perfilErradoRetorna403() throws Exception {
        String tokenFornecedor = bearer(login("fornecedor@api.com"));

        mvc.perform(post("/api/v1/cotacoes").header(HttpHeaders.AUTHORIZATION, tokenFornecedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeServico\":\"X\",\"requisitos\":\"Y\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/dashboard/empresa").header(HttpHeaders.AUTHORIZATION, tokenFornecedor))
                .andExpect(status().isForbidden());
    }

    @Test
    void empresaNaoVeAsPropostasDaCotacaoDeOutraEmpresa() throws Exception {
        String tokenEmpresa = bearer(login("empresa@api.com"));

        mvc.perform(get("/api/v1/propostas/cotacao/" + cotacaoDaOutraEmpresa.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenEmpresa))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Esta cotação pertence a outra empresa."));
    }

    @Test
    void identidadeVemDoTokenENaoDoCorpo() throws Exception {
        String tokenEmpresa = bearer(login("empresa@api.com"));

        // Mesmo enviando o empresaId de outra empresa, a cotação fica em nome de quem está logado
        mvc.perform(post("/api/v1/cotacoes").header(HttpHeaders.AUTHORIZATION, tokenEmpresa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeServico\":\"Cadeiras\",\"requisitos\":\"20 unidades\",\"empresaId\":\""
                                + cotacaoDaOutraEmpresa.getEmpresa().getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresaNome").value("Criare Consulting"));
    }

    @Test
    void refreshPeloCookieGeraNovoTokenEDetectaReuso() throws Exception {
        Cookie primeiro = cookie(login("fornecedor@api.com"));

        MvcResult renovado = mvc.perform(post("/api/v1/auth/refresh").cookie(primeiro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie segundo = cookie(renovado);
        assertThat(segundo.getValue()).isNotEqualTo(primeiro.getValue());

        // Reapresentar o cookie antigo derruba todas as sessões
        mvc.perform(post("/api/v1/auth/refresh").cookie(primeiro)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(segundo)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutApagaOCookie() throws Exception {
        Cookie refresh = cookie(login("empresa@api.com"));

        MvcResult resultado = mvc.perform(post("/api/v1/auth/logout").cookie(refresh))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        mvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void preflightDoFrontEndEhLiberado() throws Exception {
        mvc.perform(options("/api/v1/cotacoes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void naoLidasAparecemNaListaESomemAoMarcarComoLida() throws Exception {
        Negociacao negociacao = negociacaoComMensagemDaEmpresa();
        String fornecedor = bearer(login("fornecedor@api.com"));
        String url = "/api/v1/negociacoes/" + negociacao.getId();

        mvc.perform(get("/api/v1/negociacoes/minhas").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].naoLidas").value(1));
        mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(jsonPath("$.naoLidas").value(1));

        mvc.perform(patch(url + "/leitura").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/negociacoes/minhas").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(jsonPath("$[0].naoLidas").value(0));
        // Para a empresa, a própria mensagem nunca conta
        mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(login("empresa@api.com"))))
                .andExpect(jsonPath("$.naoLidas").value(0));
    }

    @Test
    void quemNaoParticipaNaoMarcaComoLida() throws Exception {
        Negociacao negociacao = negociacaoComMensagemDaEmpresa();
        mvc.perform(patch("/api/v1/negociacoes/" + negociacao.getId() + "/leitura")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("outra@api.com"))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private Negociacao negociacaoComMensagemDaEmpresa() {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Cadeiras");
        cotacao.setRequisitos("20 cadeiras ergonômicas");
        cotacao.setCategoria(CategoriaCotacao.TECNOLOGIA);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(5));
        cotacao = cotacaoService.criarCotacao(cotacao, criare.getId());

        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal("11500.00"));
        proposta.setDescricao("Entrega em 10 dias");
        proposta = propostaService.criarProposta(proposta, tech.getId(), cotacao.getId());

        Negociacao negociacao = negociacaoService.criarNegociacao(proposta.getId(), criare.getId());
        mensagemService.enviarMensagem(negociacao.getId(), "Fechamos em 10.800?", new BigDecimal("10800.00"),
                new UsuarioAutenticado(criare.getId(), TipoUsuario.EMPRESA));
        return negociacao;
    }

    private MvcResult login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String bearer(MvcResult login) throws Exception {
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        return "Bearer " + token;
    }

    private Cookie cookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String valor = setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));
        return new Cookie("refresh_token", valor);
    }

    private Empresa empresa(String nome, String cnpj, String email) {
        Empresa e = new Empresa();
        e.setRazaoSocial(nome);
        e.setCnpj(cnpj);
        e.setEmail(email);
        e.setSenha(SENHA);
        return e;
    }

    private Fornecedor fornecedor(String nome, String cnpj, String email) {
        Fornecedor f = new Fornecedor();
        f.setNomeCompleto(nome);
        f.setCnpj(cnpj);
        f.setEmail(email);
        f.setSenha(SENHA);
        return f;
    }
}
