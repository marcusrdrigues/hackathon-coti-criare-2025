package com.gestao.identidade;

import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A equipe de uma organização pela API (spec 001, R2): convidar, aceitar o convite,
 * listar e remover, com o que cada papel pode fazer.
 */
@SpringBootTest
@Transactional
class EquipeApiTest {

    private static final String SENHA = "segredo123";

    @Autowired private WebApplicationContext contexto;
    @Autowired private CadastroService cadastroService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    private MockMvc mvc;
    private String proprietaria;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        cadastroService.cadastrar(new NovaOrganizacao(TipoOrganizacao.EMPRESA, "Criare Consulting",
                "11.222.333/0001-81", "Ana Ribeiro", "ana@criare.com", SENHA));
        proprietaria = entrar("ana@criare.com");
    }

    @Test
    void conviteViraMembroJaLogado() throws Exception {
        String token = convidar("Bruno Costa", "Bruno@Criare.com");

        // O banco guarda só o hash do token (docs/dados.md): quem lê o banco não usa o convite
        entityManager.flush();
        String guardado = jdbc.queryForObject("SELECT token_hash FROM tb_convite WHERE email = ?", String.class,
                "bruno@criare.com");
        assertThat(guardado).isNotEqualTo(token).doesNotContain(token).hasSize(64);

        // A lista de pendentes não mostra o token: ele só existe na criação
        enviar(get("/api/v1/equipe/convites"), proprietaria)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("bruno@criare.com"))
                .andExpect(jsonPath("$[0].convidadoPor").value("Ana Ribeiro"))
                .andExpect(jsonPath("$[0].token").doesNotExist());

        // Quem recebe o link vê de onde ele veio, sem estar logado
        enviar(post("/api/v1/convites/consulta"), null, "{\"token\":\"" + token + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacao").value("Criare Consulting"))
                .andExpect(jsonPath("$.tipo").value("EMPRESA"))
                .andExpect(jsonPath("$.convidadoPor").value("Ana Ribeiro"))
                .andExpect(jsonPath("$.email").value("bruno@criare.com"));

        MvcResult aceite = aceitar(token, "Bruno Costa")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.papel").value("MEMBRO"))
                .andExpect(jsonPath("$.usuario.organizacao.razaoSocial").value("Criare Consulting"))
                .andReturn();
        String bruno = "Bearer " + JsonPath.read(aceite.getResponse().getContentAsString(), "$.accessToken");

        // Já entra e trabalha pela organização
        enviar(get("/api/v1/cotacoes/minhas"), bruno).andExpect(status().isOk());
        entrar("bruno@criare.com");

        enviar(get("/api/v1/equipe/membros"), bruno)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Ana Ribeiro"))
                .andExpect(jsonPath("$[0].papel").value("PROPRIETARIO"))
                .andExpect(jsonPath("$[0].voce").value(false))
                .andExpect(jsonPath("$[1].nome").value("Bruno Costa"))
                .andExpect(jsonPath("$[1].voce").value(true));
        enviar(get("/api/v1/equipe/convites"), proprietaria).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void linkUsadoVencidoCanceladoOuAdulteradoNaoCriaConta() throws Exception {
        String usado = convidar("Bruno Costa", "bruno@criare.com");
        aceitar(usado, "Bruno Costa").andExpect(status().isCreated());
        aceitar(usado, "Outra Pessoa")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("já foi usado")));

        String vencido = convidar("Carla Dias", "carla@criare.com");
        // Grava o convite, vence-o direto no banco e esquece a cópia que o Hibernate tinha na memória
        entityManager.flush();
        jdbc.update("UPDATE tb_convite SET expira_em = ? WHERE email = 'carla@criare.com'",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)));
        entityManager.clear();
        aceitar(vencido, "Carla Dias")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("venceu")));

        String cancelado = convidar("Davi Lima", "davi@criare.com");
        String conviteId = JsonPath.read(enviar(get("/api/v1/equipe/convites"), proprietaria)
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        enviar(delete("/api/v1/equipe/convites/" + conviteId), proprietaria).andExpect(status().isNoContent());
        aceitar(cancelado, "Davi Lima").andExpect(status().isNotFound());

        aceitar(cancelado.substring(1) + "x", "Davi Lima")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("não é válido")));

        // Nenhuma dessas tentativas criou conta
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_usuario WHERE email IN "
                + "('carla@criare.com', 'davi@criare.com')", Integer.class)).isZero();
    }

    @Test
    void conviteNovoParaOMesmoEmailSubstituiOAnterior() throws Exception {
        String primeiro = convidar("Bruno", "bruno@criare.com");
        String segundo = convidar("Bruno Costa", "bruno@criare.com");

        enviar(get("/api/v1/equipe/convites"), proprietaria).andExpect(jsonPath("$", hasSize(1)));
        aceitar(primeiro, "Bruno").andExpect(status().isNotFound());
        aceitar(segundo, "Bruno Costa").andExpect(status().isCreated());
    }

    @Test
    void naoConvidaQuemJaTemConta() throws Exception {
        enviar(post("/api/v1/equipe/convites"), proprietaria, "{\"nome\":\"Ana\",\"email\":\"ana@criare.com\"}")
                .andExpect(status().isConflict());
        enviar(post("/api/v1/equipe/convites"), proprietaria, "{\"nome\":\"\",\"email\":\"sem-arroba\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.email").exists());
    }

    @Test
    void membroNaoConvidaNemRemove() throws Exception {
        String bruno = membro("Bruno Costa", "bruno@criare.com");

        enviar(post("/api/v1/equipe/convites"), bruno, "{\"nome\":\"Eva\",\"email\":\"eva@criare.com\"}")
                .andExpect(status().isForbidden());
        enviar(get("/api/v1/equipe/convites"), bruno).andExpect(status().isForbidden());
        enviar(delete("/api/v1/equipe/membros/" + idDoMembro("Ana Ribeiro")), bruno).andExpect(status().isForbidden());
        // Mas vê quem é da equipe
        enviar(get("/api/v1/equipe/membros"), bruno).andExpect(status().isOk());
    }

    @Test
    void removidoPerdeAsSessoesEOHistoricoFicaComONome() throws Exception {
        String token = convidar("Bruno Costa", "bruno@criare.com");
        MvcResult aceite = aceitar(token, "Bruno Costa").andReturn();
        String bruno = "Bearer " + JsonPath.read(aceite.getResponse().getContentAsString(), "$.accessToken");
        Cookie sessaoDoBruno = cookie(aceite);

        Cotacao cotacao = cotacaoService.criarCotacao(cotacao(), comoBruno(bruno));

        enviar(delete("/api/v1/equipe/membros/" + idDoMembro("Bruno Costa")), proprietaria)
                .andExpect(status().isNoContent());

        // A sessão dele deixa de renovar, e ele não entra mais
        mvc.perform(post("/api/v1/auth/refresh").cookie(sessaoDoBruno)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bruno@criare.com\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
        enviar(get("/api/v1/equipe/membros"), proprietaria).andExpect(jsonPath("$", hasSize(1)));

        // O que ele fez continua com o nome dele
        assertThat(cotacaoService.buscarPorId(cotacao.getId()).getCriadaPor().getNome()).isEqualTo("Bruno Costa");
    }

    @Test
    void ninguemRemoveASiMesmo() throws Exception {
        enviar(delete("/api/v1/equipe/membros/" + idDoMembro("Ana Ribeiro")), proprietaria)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Você não pode remover a si mesmo da equipe."));
    }

    // ---------------------------------------------------------------- apoio

    private String convidar(String nome, String email) throws Exception {
        MvcResult criado = enviar(post("/api/v1/equipe/convites"), proprietaria,
                "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn();
        return JsonPath.read(criado.getResponse().getContentAsString(), "$.token");
    }

    private ResultActions aceitar(String token, String nome) throws Exception {
        return enviar(post("/api/v1/convites/aceite"), null,
                "{\"token\":\"" + token + "\",\"nome\":\"" + nome + "\",\"senha\":\"" + SENHA + "\"}");
    }

    private String membro(String nome, String email) throws Exception {
        aceitar(convidar(nome, email), nome).andExpect(status().isCreated());
        return entrar(email);
    }

    private String idDoMembro(String nome) throws Exception {
        String lista = enviar(get("/api/v1/equipe/membros"), proprietaria).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(lista, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst();
    }

    private UsuarioAutenticado comoBruno(String bearer) throws Exception {
        String me = enviar(get("/api/v1/auth/me"), bearer).andReturn().getResponse().getContentAsString();
        return new UsuarioAutenticado(UUID.fromString(JsonPath.read(me, "$.id")),
                UUID.fromString(JsonPath.read(me, "$.organizacao.id")), TipoOrganizacao.EMPRESA,
                Papel.MEMBRO);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, String token) throws Exception {
        return enviar(requisicao, token, null);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, String token, String corpo)
            throws Exception {
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao);
    }

    private String entrar(String email) {
        try {
            MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
            return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Cookie cookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return new Cookie("refresh_token", setCookie.substring("refresh_token=".length(), setCookie.indexOf(';')));
    }

    private static Cotacao cotacao() {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Cadeiras");
        cotacao.setRequisitos("20 cadeiras");
        cotacao.setCategoria(CategoriaCotacao.MOBILIARIO);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(5));
        return cotacao;
    }
}
