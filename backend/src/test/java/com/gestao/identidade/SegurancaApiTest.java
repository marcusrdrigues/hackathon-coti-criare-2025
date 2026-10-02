package com.gestao.identidade;

import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.MensagemNegociacaoService;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.TipoOrganizacao;
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
    @Autowired private CadastroService cadastroService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private NegociacaoService negociacaoService;
    @Autowired private MensagemNegociacaoService mensagemService;

    private MockMvc mvc;
    private UsuarioAutenticado criare;
    private UsuarioAutenticado tech;
    private Cotacao cotacaoDaOutraEmpresa;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        criare = cadastrar(TipoOrganizacao.EMPRESA, "Criare Consulting", "11.222.333/0001-81", "empresa@api.com");
        UsuarioAutenticado outra = cadastrar(TipoOrganizacao.EMPRESA, "Outra SA", "90.817.263/0001-80", "outra@api.com");
        tech = cadastrar(TipoOrganizacao.FORNECEDOR, "Tech Soluções", "45.236.789/0001-12", "fornecedor@api.com");

        Cotacao c = new Cotacao();
        c.setNomeServico("Notebooks");
        c.setRequisitos("10 unidades");
        c.setCategoria(CategoriaCotacao.TECNOLOGIA);
        c.setDataLimite(LocalDateTime.now().plusDays(5));
        cotacaoDaOutraEmpresa = cotacaoService.criarCotacao(c, outra);
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
                .andExpect(jsonPath("$.id").value(criare.usuarioId().toString()))
                .andExpect(jsonPath("$.nome").value("Pessoa da Criare Consulting"))
                .andExpect(jsonPath("$.email").value("empresa@api.com"))
                .andExpect(jsonPath("$.tipo").value("EMPRESA"))
                .andExpect(jsonPath("$.papel").value("PROPRIETARIO"))
                .andExpect(jsonPath("$.organizacao.id").value(criare.organizacaoId().toString()))
                .andExpect(jsonPath("$.organizacao.razaoSocial").value("Criare Consulting"))
                .andExpect(jsonPath("$.organizacao.cnpj").value("11222333000181"));
    }

    @Test
    void cadastroPublicoCriaOrganizacaoEProprietario() throws Exception {
        mvc.perform(post("/api/v1/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo":"FORNECEDOR","razaoSocial":"InfoWorld","cnpj":"78.345.129/0001-29",
                         "nome":"Eduardo Lima","email":"Eduardo@InfoWorld.com","senha":"segredo123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Eduardo Lima"))
                .andExpect(jsonPath("$.email").value("eduardo@infoworld.com"))
                .andExpect(jsonPath("$.tipo").value("FORNECEDOR"))
                .andExpect(jsonPath("$.papel").value("PROPRIETARIO"))
                .andExpect(jsonPath("$.organizacao.razaoSocial").value("InfoWorld"))
                .andExpect(jsonPath("$.organizacao.cnpj").value("78345129000129"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        login("eduardo@infoworld.com");
    }

    @Test
    void cadastroRepetidoOuIncompletoEhRecusado() throws Exception {
        // Mesmo CNPJ e mesmo tipo
        mvc.perform(post("/api/v1/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo":"FORNECEDOR","razaoSocial":"Tech de novo","cnpj":"45236789000112",
                         "nome":"Outra pessoa","email":"nova@tech.com","senha":"segredo123"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CNPJ já cadastrado!"));

        // Sem o tipo e sem o nome da pessoa
        mvc.perform(post("/api/v1/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"razaoSocial":"Sem tipo","cnpj":"56.102.938/0001-77","email":"x@y.com","senha":"segredo123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tipo").value("Informe se é empresa ou fornecedor"))
                .andExpect(jsonPath("$.errors.nome").value("Seu nome é obrigatório"));
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cotação não encontrada!"));
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
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- helpers

    private Negociacao negociacaoComMensagemDaEmpresa() {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Cadeiras");
        cotacao.setRequisitos("20 cadeiras ergonômicas");
        cotacao.setCategoria(CategoriaCotacao.TECNOLOGIA);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(5));
        cotacao = cotacaoService.criarCotacao(cotacao, criare);

        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal("11500.00"));
        proposta.setDescricao("Entrega em 10 dias");
        proposta = propostaService.criarProposta(proposta, tech, cotacao.getId());

        Negociacao negociacao = negociacaoService.criarNegociacao(proposta.getId(), criare);
        mensagemService.enviarMensagem(negociacao.getId(), "Fechamos em 10.800?", new BigDecimal("10800.00"),
                criare);
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

    private UsuarioAutenticado cadastrar(TipoOrganizacao tipo, String razaoSocial, String cnpj, String email) {
        return cadastroService.cadastrar(
                new NovaOrganizacao(tipo, razaoSocial, cnpj, "Pessoa da " + razaoSocial, email, SENHA));
    }
}
