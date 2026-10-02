package com.gestao.compras;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O contrato da API de compras pela camada HTTP: o mesmo caminho que o front-end
 * percorre, do cadastro ao negócio fechado, e o formato das respostas de erro.
 */
@SpringBootTest
@Transactional
class ComprasApiTest {

    private static final String SENHA = "segredo123";

    @Autowired private WebApplicationContext contexto;

    private MockMvc mvc;
    private String empresa;
    private String fornecedor;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        enviar(post("/api/v1/empresas"), null, """
                {"razaoSocial":"Criare Consulting","cnpj":"11.222.333/0001-81","email":"compras@api.com","senha":"%s"}
                """.formatted(SENHA), 201);
        enviar(post("/api/v1/fornecedores"), null, """
                {"nomeCompleto":"Tech Soluções","cnpj":"45.236.789/0001-12","email":"vendas@api.com","senha":"%s"}
                """.formatted(SENHA), 201);

        empresa = entrar("compras@api.com");
        fornecedor = entrar("vendas@api.com");
    }

    @Test
    void doPedidoAoNegocioFechado() throws Exception {
        String cotacaoId = publicarCotacao();

        enviar(put("/api/v1/cotacoes/" + cotacaoId), empresa, cotacaoJson("20 cadeiras ergonômicas"), 200);
        mvc.perform(get("/api/v1/cotacoes/minhas").header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(cotacaoId)));

        mvc.perform(get("/api/v1/cotacoes/abertas").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(jsonPath("$[*].id", hasItem(cotacaoId)));
        mvc.perform(get("/api/v1/cotacoes/" + cotacaoId).header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeServico").value("20 cadeiras ergonômicas"));

        String propostaId = enviarProposta(cotacaoId);
        mvc.perform(get("/api/v1/propostas/minhas").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(jsonPath("$[*].id", hasItem(propostaId)));
        mvc.perform(get("/api/v1/propostas/" + propostaId).header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/propostas/cotacao/" + cotacaoId).header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(jsonPath("$", hasSize(1)));

        String negociacaoId = id(enviar(post("/api/v1/negociacoes"), empresa,
                "{\"propostaId\":\"" + propostaId + "\"}", 201));
        enviar(post("/api/v1/mensagens"), fornecedor, """
                {"negociacaoId":"%s","mensagem":"Fecho em 10.800","valorOfertado":10800.00}
                """.formatted(negociacaoId), 201);
        mvc.perform(get("/api/v1/mensagens/negociacao/" + negociacaoId).header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].mensagem").value("Fecho em 10.800"));

        enviar(patch("/api/v1/negociacoes/" + negociacaoId + "/finalizar"), empresa,
                "{\"valorFinal\":10800.00}", 200);

        mvc.perform(get("/api/v1/dashboard/empresa").header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacoesFechadas").value(1));
        mvc.perform(get("/api/v1/dashboard/fornecedor").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacoesGanhas").value(1))
                .andExpect(jsonPath("$.valorTotalGanho").value(10800.0));
    }

    @Test
    void retirarRecusarECancelar() throws Exception {
        String cotacaoId = publicarCotacao();

        // O fornecedor retira a proposta e manda outra; a empresa recusa a nova
        String primeira = enviarProposta(cotacaoId);
        mvc.perform(delete("/api/v1/propostas/" + primeira).header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isNoContent());
        String segunda = enviarProposta(cotacaoId);
        enviar(patch("/api/v1/propostas/" + segunda + "/recusar"), empresa, "{}", 200);
        mvc.perform(get("/api/v1/propostas/" + segunda).header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(jsonPath("$.status").value("RECUSADA"));

        enviar(patch("/api/v1/cotacoes/" + cotacaoId + "/cancelar"), empresa, "{}", 200);
        mvc.perform(get("/api/v1/cotacoes/" + cotacaoId).header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        // Negociação encerrada sem acordo devolve a cotação ao mural
        String outra = publicarCotacao();
        String proposta = enviarProposta(outra);
        String negociacaoId = id(enviar(post("/api/v1/negociacoes"), empresa,
                "{\"propostaId\":\"" + proposta + "\"}", 201));
        enviar(patch("/api/v1/negociacoes/" + negociacaoId + "/cancelar"), empresa, "{}", 200);
        mvc.perform(get("/api/v1/cotacoes/" + outra).header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void errosTemSempreOMesmoFormato() throws Exception {
        // Validação: um erro por campo
        mvc.perform(post("/api/v1/cotacoes").header(HttpHeaders.AUTHORIZATION, empresa)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nomeServico\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nomeServico").exists())
                .andExpect(jsonPath("$.errors.requisitos").exists());

        // Recurso que não existe, id malformado e JSON inválido
        mvc.perform(get("/api/v1/cotacoes/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/v1/cotacoes/nao-e-um-id").header(HttpHeaders.AUTHORIZATION, empresa))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/cotacoes").header(HttpHeaders.AUTHORIZATION, empresa)
                        .contentType(MediaType.APPLICATION_JSON).content("{ isto não é json"))
                .andExpect(status().isBadRequest());

        // Cadastro repetido e regra de negócio
        enviar(post("/api/v1/fornecedores"), null, """
                {"nomeCompleto":"Outra","cnpj":"78.345.129/0001-29","email":"vendas@api.com","senha":"%s"}
                """.formatted(SENHA), 409);
        String cotacaoId = publicarCotacao();
        enviarProposta(cotacaoId);
        mvc.perform(post("/api/v1/propostas").header(HttpHeaders.AUTHORIZATION, fornecedor)
                        .contentType(MediaType.APPLICATION_JSON).content(propostaJson(cotacaoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---------------------------------------------------------------- apoio

    private String publicarCotacao() throws Exception {
        return id(enviar(post("/api/v1/cotacoes"), empresa, cotacaoJson("Cadeiras para o escritório"), 201));
    }

    private String enviarProposta(String cotacaoId) throws Exception {
        return id(enviar(post("/api/v1/propostas"), fornecedor, propostaJson(cotacaoId), 201));
    }

    private static String cotacaoJson(String nome) {
        return """
                {"nomeServico":"%s","requisitos":"Com regulagem de altura e apoio lombar.",
                 "categoria":"MOBILIARIO","orcamentoEstimado":12000.00,"dataLimite":"%s"}
                """.formatted(nome, LocalDateTime.now().plusDays(5).withNano(0));
    }

    private static String propostaJson(String cotacaoId) {
        return """
                {"valor":11500.00,"descricao":"Entrega em 10 dias, frete incluso.","cotacaoId":"%s"}
                """.formatted(cotacaoId);
    }

    private MvcResult enviar(MockHttpServletRequestBuilder requisicao, String token, String corpo, int esperado)
            throws Exception {
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().is(esperado))
                .andReturn();
    }

    private String entrar(String email) throws Exception {
        MvcResult login = enviar(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}", 200);
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String id(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }
}
