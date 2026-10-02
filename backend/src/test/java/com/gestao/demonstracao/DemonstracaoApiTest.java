package com.gestao.demonstracao;

import com.gestao.compras.aplicacao.porta.CotacaoRepositorio;
import com.gestao.compras.aplicacao.porta.NegociacaoRepositorio;
import com.gestao.demonstracao.aplicacao.DadosDemonstracao;
import com.gestao.identidade.aplicacao.porta.OrganizacaoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profile demo num banco H2 separado (para os dados de exemplo não
 * interferirem nos outros testes).
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:gestao-demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("demo")
class DemonstracaoApiTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private DadosDemonstracao dadosDemonstracao;
    @Autowired private OrganizacaoRepositorio organizacaoRepositorio;
    @Autowired private CotacaoRepositorio cotacaoRepositorio;
    @Autowired private NegociacaoRepositorio negociacaoRepositorio;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void listaAsContasDeDemonstracaoSemExporSenha() throws Exception {
        mvc.perform(get("/api/v1/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].perfil").value("EMPRESA"))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
    }

    @Test
    void entraComUmCliqueComoEmpresaOuFornecedor() throws Exception {
        mvc.perform(post("/api/v1/auth/demo/EMPRESA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(DadosDemonstracao.EMAIL_EMPRESA))
                .andExpect(jsonPath("$.usuario.papel").value("PROPRIETARIO"))
                .andExpect(jsonPath("$.usuario.organizacao.razaoSocial").value("Criare Consulting"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")));

        mvc.perform(post("/api/v1/auth/demo/FORNECEDOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.tipo").value("FORNECEDOR"));
    }

    @Test
    void perfilInexistenteRetorna400() throws Exception {
        mvc.perform(post("/api/v1/auth/demo/ADMIN")).andExpect(status().isBadRequest());
    }

    @Test
    void healthCheckEhPublico() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void restaurarVoltaOsDadosAoEstadoInicial() {
        long organizacoes = organizacaoRepositorio.contar();
        long cotacoes = cotacaoRepositorio.contar();
        long negociacoes = negociacaoRepositorio.contar();
        assertThat(organizacoes).isEqualTo(6);
        assertThat(negociacoes).isEqualTo(2);

        dadosDemonstracao.restaurar();
        dadosDemonstracao.restaurar();

        assertThat(organizacaoRepositorio.contar()).isEqualTo(organizacoes);
        assertThat(cotacaoRepositorio.contar()).isEqualTo(cotacoes);
        assertThat(negociacaoRepositorio.contar()).isEqualTo(negociacoes);
    }
}
