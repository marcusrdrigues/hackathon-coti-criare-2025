package com.gestao.configurations;

import com.gestao.repositories.CotacaoRepository;
import com.gestao.repositories.EmpresaRepository;
import com.gestao.repositories.NegociacaoRepository;
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
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private CotacaoRepository cotacaoRepository;
    @Autowired private NegociacaoRepository negociacaoRepository;

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
        long empresas = empresaRepository.count();
        long cotacoes = cotacaoRepository.count();
        long negociacoes = negociacaoRepository.count();
        assertThat(empresas).isEqualTo(2);
        assertThat(negociacoes).isEqualTo(2);

        dadosDemonstracao.restaurar();
        dadosDemonstracao.restaurar();

        assertThat(empresaRepository.count()).isEqualTo(empresas);
        assertThat(cotacaoRepository.count()).isEqualTo(cotacoes);
        assertThat(negociacaoRepository.count()).isEqualTo(negociacoes);
    }
}
