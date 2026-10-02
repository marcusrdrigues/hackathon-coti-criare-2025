package com.gestao.compartilhado;

import com.gestao.compartilhado.infraestrutura.web.Rastreio;
import com.gestao.compartilhado.infraestrutura.web.RastreioDeRequisicao;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rastreio e higiene dos logs (spec 001, R6): toda requisição tem um identificador que
 * aparece nos logs, no cabeçalho e no erro; e nenhum log carrega senha, token, e-mail
 * completo ou o conteúdo de uma negociação.
 */
@SpringBootTest
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class RastreioELogsTest {

    private static final String ID = "[0-9a-f]{32}";
    private static final String SENHA = "SenhaSecreta123";
    private static final String EMAIL_EMPRESA = "compras.sigilo@empresa-teste.com";
    private static final String EMAIL_FORNECEDOR = "vendas.sigilo@fornecedor-teste.com";
    private static final String CONDICOES = "Condição confidencial: desconto de 7% para pagamento em 10 dias";
    private static final String MENSAGEM = "Mensagem reservada: fechamos em 9.900 se mantiver o frete";

    @Autowired private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // O MockMvc não registra os filtros sozinho: o de rastreio entra antes do Spring Security, como na aplicação
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(contexto.getBean(RastreioDeRequisicao.class))
                .apply(springSecurity())
                .build();
    }

    @Test
    void todaRespostaTemUmIdentificadorNovo() throws Exception {
        String primeiro = mvc.perform(get("/api/v1/cotacoes/categorias"))
                .andReturn().getResponse().getHeader(Rastreio.CABECALHO);
        String segundo = mvc.perform(get("/api/v1/cotacoes/categorias"))
                .andReturn().getResponse().getHeader(Rastreio.CABECALHO);

        assertThat(primeiro).matches(ID);
        assertThat(segundo).matches(ID).isNotEqualTo(primeiro);
    }

    @Test
    void erroTrazOMesmoIdentificadorDoCabecalho() throws Exception {
        // 401 sai do Spring Security, antes de qualquer controller
        MvcResult semLogin = mvc.perform(get("/api/v1/cotacoes/minhas"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(traceIdDoCorpo(semLogin)).isEqualTo(semLogin.getResponse().getHeader(Rastreio.CABECALHO));

        // 400 sai do tratamento global de erros
        MvcResult invalido = enviar(post("/api/v1/auth/login"), null, "{\"email\":\"\",\"senha\":\"\"}");
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        assertThat(traceIdDoCorpo(invalido)).isEqualTo(invalido.getResponse().getHeader(Rastreio.CABECALHO));
    }

    @Test
    void identificadorEnviadoPeloClienteNaoEhAceito() throws Exception {
        String devolvido = mvc.perform(get("/api/v1/cotacoes/categorias")
                        .header(Rastreio.CABECALHO, "falso\n[ERROR] linha injetada"))
                .andReturn().getResponse().getHeader(Rastreio.CABECALHO);

        assertThat(devolvido).matches(ID);
    }

    @Test
    void logsNaoVazamDadosSensiveis(CapturedOutput saida) throws Exception {
        enviar(post("/api/v1/empresas"), null, """
                {"razaoSocial":"Empresa Sigilo","cnpj":"11.222.333/0001-81","email":"%s","senha":"%s"}
                """.formatted(EMAIL_EMPRESA, SENHA));
        enviar(post("/api/v1/fornecedores"), null, """
                {"nomeCompleto":"Fornecedor Sigilo","cnpj":"45.236.789/0001-12","email":"%s","senha":"%s"}
                """.formatted(EMAIL_FORNECEDOR, SENHA));

        // Erros que geram log: senha errada, cadastro repetido e validação
        enviar(post("/api/v1/auth/login"), null, login(EMAIL_EMPRESA, SENHA + "x"));
        enviar(post("/api/v1/fornecedores"), null, """
                {"nomeCompleto":"Outro","cnpj":"78.345.129/0001-29","email":"%s","senha":"%s"}
                """.formatted(EMAIL_FORNECEDOR, SENHA));
        enviar(post("/api/v1/empresas"), null, "{\"email\":\"" + EMAIL_EMPRESA + "\",\"senha\":\"" + SENHA + "\"}");

        MvcResult loginEmpresa = enviar(post("/api/v1/auth/login"), null, login(EMAIL_EMPRESA, SENHA));
        MvcResult loginFornecedor = enviar(post("/api/v1/auth/login"), null, login(EMAIL_FORNECEDOR, SENHA));
        String tokenEmpresa = token(loginEmpresa);
        String tokenFornecedor = token(loginFornecedor);
        String refresh = loginEmpresa.getResponse().getHeader(HttpHeaders.SET_COOKIE);

        // Um negócio inteiro, com condições e mensagens que são sigilo comercial
        String cotacao = id(enviar(post("/api/v1/cotacoes"), tokenEmpresa, """
                {"nomeServico":"Cadeiras","requisitos":"20 cadeiras","categoria":"MOBILIARIO","dataLimite":"%s"}
                """.formatted(LocalDateTime.now().plusDays(5).withNano(0))));
        String proposta = id(enviar(post("/api/v1/propostas"), tokenFornecedor, """
                {"valor":10500.00,"descricao":"%s","cotacaoId":"%s"}
                """.formatted(CONDICOES, cotacao)));
        String negociacao = id(enviar(post("/api/v1/negociacoes"), tokenEmpresa,
                "{\"propostaId\":\"" + proposta + "\"}"));
        enviar(post("/api/v1/mensagens"), tokenEmpresa, """
                {"negociacaoId":"%s","mensagem":"%s","valorOfertado":9900.00}
                """.formatted(negociacao, MENSAGEM));

        String logs = saida.getAll();
        List<String> proibidos = List.of(SENHA, EMAIL_EMPRESA, EMAIL_FORNECEDOR, CONDICOES, MENSAGEM,
                tokenEmpresa.substring("Bearer ".length()), refresh.substring(refresh.indexOf('=') + 1, refresh.indexOf(';')));
        for (String proibido : proibidos) {
            assertThat(logs).as("os logs não podem conter: %s", proibido).doesNotContain(proibido);
        }
        // E os logs das requisições trazem o identificador de rastreio
        assertThat(logs).containsPattern("\\[" + ID + "\\] ");
    }

    // ---------------------------------------------------------------- apoio

    private MvcResult enviar(MockHttpServletRequestBuilder requisicao, String token, String corpo) throws Exception {
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo)).andReturn();
    }

    private static String login(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    private static String token(MvcResult login) throws Exception {
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String id(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    private static String traceIdDoCorpo(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.traceId");
    }
}
