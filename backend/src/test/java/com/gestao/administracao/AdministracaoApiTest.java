package com.gestao.administracao;

import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.SuperadminService.Configuracao;
import com.gestao.identidade.aplicacao.SuperadminService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.TipoOrganizacao;
import com.gestao.identidade.infraestrutura.seguranca.ClaimsDoToken;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O superadmin (spec 001, R3): nasce da configuração do servidor (ver application.properties
 * dos testes), entra só na área administrativa, que é somente leitura, e nenhuma pessoa de
 * organização chega lá.
 */
@SpringBootTest
@Transactional
class AdministracaoApiTest {

    private static final String EMAIL_ADMIN = "admin@plataforma.com";
    private static final String SENHA_ADMIN = "senhaDoSuperadmin123";
    private static final String SENHA = "segredo123";
    private static final String ORGANIZACOES = "/api/v1/admin/organizacoes";

    @Autowired private WebApplicationContext contexto;
    @Autowired private CadastroService cadastroService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private SuperadminService superadminService;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager entityManager;

    private MockMvc mvc;
    private UsuarioAutenticado empresa;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        empresa = cadastroService.cadastrar(new NovaOrganizacao(TipoOrganizacao.EMPRESA, "Admin Teste Empresa",
                "61.305.728/0001-16", "Pessoa da Empresa", "empresa@admin-teste.com", SENHA));
        UsuarioAutenticado fornecedor = cadastroService.cadastrar(new NovaOrganizacao(TipoOrganizacao.FORNECEDOR,
                "Admin Teste Fornecedor", "72.938.461/0001-10", "Pessoa do Fornecedor", "fornecedor@admin-teste.com",
                SENHA));
        cadastroService.adicionarMembro(empresa.organizacaoId(), "Segunda Pessoa", "segunda@admin-teste.com", SENHA);

        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Cadeiras");
        cotacao.setRequisitos("20 cadeiras");
        cotacao.setCategoria(CategoriaCotacao.MOBILIARIO);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(5));
        cotacao = cotacaoService.criarCotacao(cotacao, empresa);

        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal("9000.00"));
        proposta.setDescricao("Entrega em 10 dias");
        propostaService.criarProposta(proposta, fornecedor, cotacao.getId());
    }

    @Test
    void superadminEntraComTokenSemOrganizacao() throws Exception {
        MvcResult login = login(EMAIL_ADMIN, SENHA_ADMIN);

        String corpo = login.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(corpo, "$.usuario.papel")).isEqualTo("SUPERADMIN");
        assertThat((Object) JsonPath.read(corpo, "$.usuario.tipo")).isNull();
        assertThat((Object) JsonPath.read(corpo, "$.usuario.organizacao")).isNull();

        Jwt jwt = jwtDecoder.decode(JsonPath.read(corpo, "$.accessToken"));
        assertThat(jwt.getClaimAsString(ClaimsDoToken.PAPEL)).isEqualTo("SUPERADMIN");
        assertThat(jwt.getClaimAsString(ClaimsDoToken.ORGANIZACAO)).isNull();
        assertThat(jwt.getClaimAsString(ClaimsDoToken.TIPO)).isNull();

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL_ADMIN))
                .andExpect(jsonPath("$.papel").value("SUPERADMIN"));

        // A renovação pelo cookie mantém o papel
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookie(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("SUPERADMIN"));
    }

    @Test
    void superadminVeAsOrganizacoesComOsTotais() throws Exception {
        MvcResult resultado = mvc.perform(get(ORGANIZACOES).param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(EMAIL_ADMIN, SENHA_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50))
                .andExpect(jsonPath("$.page.number").value(0))
                .andReturn();
        String corpo = resultado.getResponse().getContentAsString();

        Map<String, Object> daEmpresa = linha(corpo, "Admin Teste Empresa");
        assertThat(daEmpresa)
                .containsEntry("tipo", "EMPRESA")
                .containsEntry("cnpj", "61305728000116")
                .containsEntry("pessoas", 2)
                .containsEntry("cotacoes", 1)
                .containsEntry("propostas", 0);
        assertThat(linha(corpo, "Admin Teste Fornecedor"))
                .containsEntry("tipo", "FORNECEDOR")
                .containsEntry("pessoas", 1)
                .containsEntry("cotacoes", 0)
                .containsEntry("propostas", 1);
        // Nada de dado pessoal nem de credencial na lista
        assertThat(corpo).doesNotContain("@admin-teste.com").doesNotContain("senha");
    }

    @Test
    void listaPaginadaTemTamanhoMaximoEOrdemPermitida() throws Exception {
        String token = bearer(login(EMAIL_ADMIN, SENHA_ADMIN));

        // Mais recentes primeiro por padrão, um por página
        mvc.perform(get(ORGANIZACOES).param("size", "1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.size").value(1));

        mvc.perform(get(ORGANIZACOES).param("size", "500").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50));

        mvc.perform(get(ORGANIZACOES).param("sort", "razaoSocial,asc").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        // Campo fora da lista, direção inválida e página negativa
        mvc.perform(get(ORGANIZACOES).param("sort", "senhaHash").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ORGANIZACOES).param("sort", "razaoSocial,lado").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ORGANIZACOES).param("page", "-1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pessoaDeOrganizacaoNaoEntraNaAreaAdministrativa() throws Exception {
        mvc.perform(get(ORGANIZACOES).header(HttpHeaders.AUTHORIZATION, bearer(login("empresa@admin-teste.com", SENHA))))
                .andExpect(status().isForbidden());
        mvc.perform(get(ORGANIZACOES)).andExpect(status().isUnauthorized());
    }

    @Test
    void superadminNaoEntraNasRotasDasOrganizacoes() throws Exception {
        String token = bearer(login(EMAIL_ADMIN, SENHA_ADMIN));

        for (String rota : List.of("/api/v1/cotacoes/abertas", "/api/v1/cotacoes/minhas", "/api/v1/dashboard/empresa",
                "/api/v1/equipe/membros", "/api/v1/negociacoes/minhas")) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/equipe/convites").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\",\"email\":\"x@x.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadastroPublicoNaoCriaSuperadmin() throws Exception {
        mvc.perform(post("/api/v1/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo":"EMPRESA","razaoSocial":"Tentativa","cnpj":"48.152.639/0001-19",
                         "nome":"Quem Tenta","email":"tenta@admin-teste.com","senha":"segredo123",
                         "papel":"SUPERADMIN","superadmin":true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("PROPRIETARIO"));

        assertThat(ehSuperadmin("tenta@admin-teste.com")).isFalse();
    }

    @Test
    void semConfiguracaoNenhumSuperadminFicaAtivo() throws Exception {
        superadminService.sincronizar(Optional.empty());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL_ADMIN, SENHA_ADMIN)))
                .andExpect(status().isUnauthorized());
        assertThat(superadminsAtivos()).isZero();
    }

    @Test
    void senhaNovaNaConfiguracaoSubstituiAAntiga() throws Exception {
        superadminService.sincronizar(Optional.of(new Configuracao(EMAIL_ADMIN, "outraSenhaDoAdmin456")));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL_ADMIN, SENHA_ADMIN)))
                .andExpect(status().isUnauthorized());
        login(EMAIL_ADMIN, "outraSenhaDoAdmin456");
    }

    @Test
    void outroEmailNaConfiguracaoTrocaOSuperadmin() throws Exception {
        assertThat(superadminService.sincronizar(Optional.of(new Configuracao("Novo@Plataforma.com",
                "senhaDoNovoAdmin789")))).isPresent();

        assertThat(superadminsAtivos()).isOne();
        login("novo@plataforma.com", "senhaDoNovoAdmin789");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL_ADMIN, SENHA_ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nuncaPromoveUmaContaDeOrganizacao() {
        assertThat(superadminService.sincronizar(Optional.of(new Configuracao("empresa@admin-teste.com",
                "senhaQualquer12345")))).isEmpty();

        assertThat(ehSuperadmin("empresa@admin-teste.com")).isFalse();
        assertThat(superadminsAtivos()).isZero();
    }

    @Test
    void senhaCurtaOuConfiguracaoIncompletaNaoCriaSuperadmin() {
        assertThat(superadminService.sincronizar(Optional.of(new Configuracao(EMAIL_ADMIN, "curta")))).isEmpty();
        assertThat(superadminService.sincronizar(Optional.of(new Configuracao(EMAIL_ADMIN, "")))).isEmpty();
        assertThat(superadminService.sincronizar(Optional.of(new Configuracao("", "")))).isEmpty();
        assertThat(superadminsAtivos()).isZero();

        // A configuração não aparece por inteiro nem em texto (ex.: num log)
        assertThat(new Configuracao(EMAIL_ADMIN, SENHA_ADMIN).toString()).doesNotContain(SENHA_ADMIN)
                .doesNotContain(EMAIL_ADMIN);
    }

    /** O que está pendente no Hibernate vai para o banco antes da consulta direta. */
    private Boolean ehSuperadmin(String email) {
        entityManager.flush();
        return jdbc.queryForObject("SELECT superadmin FROM tb_usuario WHERE email = ?", Boolean.class, email);
    }

    private int superadminsAtivos() {
        entityManager.flush();
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tb_usuario WHERE superadmin = TRUE AND desativado_em IS NULL", Integer.class);
        return total == null ? 0 : total;
    }

    private static Map<String, Object> linha(String corpo, String razaoSocial) {
        List<Map<String, Object>> linhas = JsonPath.read(corpo, "$.content[?(@.razaoSocial == '" + razaoSocial + "')]");
        assertThat(linhas).hasSize(1);
        return linhas.get(0);
    }

    private MvcResult login(String email, String senha) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(email, senha)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String credenciais(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    private static String bearer(MvcResult login) throws Exception {
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie cookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return new Cookie("refresh_token", setCookie.substring("refresh_token=".length(), setCookie.indexOf(';')));
    }
}
