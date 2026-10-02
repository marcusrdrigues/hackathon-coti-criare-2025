package com.gestao.auditoria;

import com.gestao.compartilhado.infraestrutura.web.RastreioDeRequisicao;
import com.gestao.identidade.aplicacao.SuperadminService;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os eventos de segurança (spec 003, R2). Sem {@code @Transactional} de propósito: o evento é
 * gravado numa transação própria, e o teste confere que ele fica mesmo quando a operação que o
 * gerou é desfeita. Cada teste usa um domínio de e-mail sorteado, e o e-mail mascarado
 * ({@code p***@eventos-sorteado.com}) separa os eventos de um teste dos de outro.
 */
@SpringBootTest
class EventosDeSegurancaTest {

    private static final String SENHA = "segredo123";

    @Autowired private WebApplicationContext contexto;
    @Autowired private SuperadminService superadminService;
    @Autowired private JdbcTemplate jdbc;

    @Value("${app.superadmin.email}") private String emailDoSuperadmin;
    @Value("${app.superadmin.senha}") private String senhaDoSuperadmin;

    private MockMvc mvc;
    private String dominio;
    private String emailEmpresa;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(new ServerHttpObservationFilter(contexto.getBean(ObservationRegistry.class)),
                        contexto.getBean(RastreioDeRequisicao.class))
                .apply(springSecurity())
                .build();
        dominio = "eventos-" + UUID.randomUUID().toString().substring(0, 8) + ".com";
        emailEmpresa = "ana@" + dominio;
        cadastrar("EMPRESA", emailEmpresa);
    }

    @Test
    void senhaErradaFicaRegistradaMesmoComOLoginDesfeito() throws Exception {
        login(emailEmpresa, "senhaErrada", 401);
        login("ninguem@" + dominio, SENHA, 401);

        List<Map<String, Object>> falhas = eventos("LOGIN_FALHOU");
        assertThat(falhas).hasSize(2);

        Map<String, Object> senhaErrada = doEmail(falhas, "a***@" + dominio);
        assertThat(senhaErrada.get("usuario_id")).isEqualTo(idDaPessoa(emailEmpresa));
        assertThat(senhaErrada.get("organizacao_id")).isEqualTo(idDaOrganizacao(emailEmpresa));
        assertThat(senhaErrada.get("detalhe")).isEqualTo("Senha errada.");
        assertThat((String) senhaErrada.get("trace_id")).hasSize(32);

        // E-mail que não existe: só mascarado, sem pessoa nem organização
        Map<String, Object> semConta = doEmail(falhas, "n***@" + dominio);
        assertThat(semConta.get("usuario_id")).isNull();
        assertThat(semConta.get("organizacao_id")).isNull();
    }

    @Test
    void bloqueioPorExcessoDeTentativasFicaRegistrado() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(emailEmpresa, "senhaErrada" + i, 401);
        }
        login(emailEmpresa, SENHA, 429);

        assertThat(eventos("LOGIN_FALHOU")).hasSize(5);
        assertThat(eventos("LOGIN_BLOQUEADO")).singleElement()
                .satisfies(evento -> assertThat(evento.get("usuario_id")).isEqualTo(idDaPessoa(emailEmpresa)));
        assertThat(eventos("LOGIN")).isEmpty();
    }

    @Test
    void loginESaidaFicamRegistrados() throws Exception {
        MvcResult entrada = login(emailEmpresa, SENHA, 200);
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie(entrada))).andExpect(status().isNoContent());

        assertThat(eventos("LOGIN")).singleElement().satisfies(evento -> {
            assertThat(evento.get("usuario_id")).isEqualTo(idDaPessoa(emailEmpresa));
            assertThat(evento.get("organizacao_id")).isEqualTo(idDaOrganizacao(emailEmpresa));
        });
        assertThat(eventos("LOGOUT")).singleElement()
                .satisfies(evento -> assertThat(evento.get("usuario_id")).isEqualTo(idDaPessoa(emailEmpresa)));
    }

    @Test
    void tokenDeSessaoReutilizadoRegistraARevogacao() throws Exception {
        Cookie primeiro = cookie(login(emailEmpresa, SENHA, 200));
        mvc.perform(post("/api/v1/auth/refresh").cookie(primeiro)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").cookie(primeiro)).andExpect(status().isUnauthorized());

        assertThat(eventos("SESSAO_REVOGADA_POR_REUSO")).singleElement().satisfies(evento -> {
            assertThat(evento.get("usuario_id")).isEqualTo(idDaPessoa(emailEmpresa));
            assertThat(evento.get("organizacao_id")).isEqualTo(idDaOrganizacao(emailEmpresa));
        });
    }

    @Test
    void conviteEEquipeFicamRegistrados() throws Exception {
        String proprietaria = token(login(emailEmpresa, SENHA, 200));
        String cancelado = id(enviar(post("/api/v1/equipe/convites"), proprietaria,
                "{\"nome\":\"Bruno\",\"email\":\"bruno@" + dominio + "\"}", 201));
        enviar(delete("/api/v1/equipe/convites/" + cancelado), proprietaria, null, 204);

        MvcResult criado = enviar(post("/api/v1/equipe/convites"), proprietaria,
                "{\"nome\":\"Carla\",\"email\":\"carla@" + dominio + "\"}", 201);
        String linkDoConvite = JsonPath.read(criado.getResponse().getContentAsString(), "$.token");
        enviar(post("/api/v1/convites/aceite"), null,
                "{\"token\":\"" + linkDoConvite + "\",\"nome\":\"Carla\",\"senha\":\"" + SENHA + "\"}", 201);
        String vinculo = jdbc.queryForObject("SELECT m.id FROM tb_membro m JOIN tb_usuario u ON u.id = m.usuario_id "
                + "WHERE u.email = ?", String.class, "carla@" + dominio);
        enviar(delete("/api/v1/equipe/membros/" + vinculo), proprietaria, null, 204);

        UUID ana = idDaPessoa(emailEmpresa);
        UUID organizacao = idDaOrganizacao(emailEmpresa);
        assertThat(eventos("CONVITE_CRIADO")).hasSize(2)
                .allSatisfy(evento -> assertThat(evento.get("usuario_id")).isEqualTo(ana));
        assertThat(eventos("CONVITE_CANCELADO")).singleElement()
                .satisfies(evento -> assertThat(evento.get("email_mascarado")).isEqualTo("b***@" + dominio));
        // Quem aceita é a pessoa que acabou de entrar, já na organização
        assertThat(eventos("CONVITE_ACEITO")).singleElement().satisfies(evento -> {
            assertThat(evento.get("usuario_id")).isEqualTo(idDaPessoa("carla@" + dominio));
            assertThat(evento.get("organizacao_id")).isEqualTo(organizacao);
        });
        assertThat(eventos("MEMBRO_REMOVIDO")).singleElement().satisfies(evento -> {
            assertThat(evento.get("usuario_id")).isEqualTo(ana);
            assertThat(evento.get("organizacao_id")).isEqualTo(organizacao);
            assertThat(evento.get("email_mascarado")).isEqualTo("c***@" + dominio);
        });
    }

    @Test
    void acessoNegadoPeloPerfilEPelaRotaFicaRegistrado() throws Exception {
        String emailFornecedor = "fabio@" + dominio;
        cadastrar("FORNECEDOR", emailFornecedor);
        String fornecedor = token(login(emailFornecedor, SENHA, 200));

        // Barrado pelo @PreAuthorize (só empresa publica cotação) e pela regra da rota (console)
        enviar(post("/api/v1/cotacoes"), fornecedor, """
                {"nomeServico":"Cadeiras","requisitos":"Com regulagem.","categoria":"MOBILIARIO",
                 "orcamentoEstimado":1000.00,"dataLimite":"%s"}
                """.formatted(LocalDateTime.now().plusDays(5).withNano(0)), 403);
        mvc.perform(get("/api/v1/admin/organizacoes").header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isForbidden());

        List<Map<String, Object>> negados = jdbc.queryForList(
                "SELECT * FROM tb_evento_seguranca WHERE tipo = 'ACESSO_NEGADO' AND usuario_id = ?",
                idDaPessoa(emailFornecedor));
        assertThat(negados).extracting(evento -> evento.get("detalhe"))
                .containsExactlyInAnyOrder("POST /api/v1/cotacoes", "GET /api/v1/admin/organizacoes");
        assertThat(negados).allSatisfy(evento -> {
            assertThat(evento.get("organizacao_id")).isEqualTo(idDaOrganizacao(emailFornecedor));
            assertThat((String) evento.get("trace_id")).hasSize(32);
        });
    }

    @Test
    void mudancasNoSuperadminDaConfiguracaoFicamRegistradas() {
        String outro = "outro@" + dominio;
        try {
            // Outro e-mail na configuração: cria o novo e desativa o atual
            superadminService.sincronizar(Optional.of(new SuperadminService.Configuracao(outro, "senhaDoOutroAdmin")));
            // De volta, com outra senha: desativa o outro e troca a senha do atual
            superadminService.sincronizar(Optional.of(
                    new SuperadminService.Configuracao(emailDoSuperadmin, "umaSenhaNovaQualquer")));
        } finally {
            superadminService.sincronizar(Optional.of(
                    new SuperadminService.Configuracao(emailDoSuperadmin, senhaDoSuperadmin)));
        }

        assertThat(eventos("SUPERADMIN_CRIADO")).singleElement()
                .satisfies(evento -> assertThat(evento.get("usuario_id")).isEqualTo(idDaPessoa(outro)));
        assertThat(eventos("SUPERADMIN_DESATIVADO")).singleElement()
                .satisfies(evento -> assertThat(evento.get("organizacao_id")).isNull());

        UUID superadmin = idDaPessoa(emailDoSuperadmin);
        List<Map<String, Object>> doAtual = jdbc.queryForList(
                "SELECT * FROM tb_evento_seguranca WHERE usuario_id = ? AND tipo IN "
                        + "('SUPERADMIN_DESATIVADO', 'SUPERADMIN_SENHA_TROCADA') ORDER BY ocorrido_em", superadmin);
        assertThat(doAtual).extracting(evento -> evento.get("tipo"))
                .containsSubsequence("SUPERADMIN_DESATIVADO", "SUPERADMIN_SENHA_TROCADA", "SUPERADMIN_SENHA_TROCADA");
    }

    @Test
    void nenhumEventoGuardaOEmailInteiro() throws Exception {
        login(emailEmpresa, "senhaErrada", 401);
        login(emailEmpresa, SENHA, 200);

        List<String> emails = jdbc.queryForList(
                "SELECT email_mascarado FROM tb_evento_seguranca WHERE email_mascarado IS NOT NULL", String.class);
        assertThat(emails).isNotEmpty().allSatisfy(email -> assertThat(email).contains("***"));
        assertThat(jdbc.queryForList("SELECT detalhe FROM tb_evento_seguranca WHERE detalhe IS NOT NULL", String.class))
                .noneMatch(detalhe -> detalhe.contains("@"));
    }

    // ---------------------------------------------------------------- apoio

    /** Os eventos deste teste de um tipo, pelo domínio sorteado do e-mail. */
    private List<Map<String, Object>> eventos(String tipo) {
        return jdbc.queryForList("SELECT * FROM tb_evento_seguranca WHERE tipo = ? AND email_mascarado LIKE ?",
                tipo, "%@" + dominio);
    }

    private static Map<String, Object> doEmail(List<Map<String, Object>> eventos, String emailMascarado) {
        return eventos.stream().filter(evento -> emailMascarado.equals(evento.get("email_mascarado")))
                .findFirst().orElseThrow();
    }

    private UUID idDaPessoa(String email) {
        return jdbc.queryForObject("SELECT id FROM tb_usuario WHERE email = ?", UUID.class, email);
    }

    private UUID idDaOrganizacao(String email) {
        return jdbc.queryForObject("SELECT m.organizacao_id FROM tb_membro m JOIN tb_usuario u ON u.id = m.usuario_id "
                + "WHERE u.email = ? AND m.removido_em IS NULL", UUID.class, email);
    }

    private void cadastrar(String tipo, String email) throws Exception {
        enviar(post("/api/v1/cadastro"), null, """
                {"tipo":"%s","razaoSocial":"Organização %s","cnpj":"%s","nome":"Pessoa","email":"%s","senha":"%s"}
                """.formatted(tipo, email, cnpjAleatorio(), email, SENHA), 201);
    }

    private MvcResult login(String email, String senha, int esperado) throws Exception {
        return enviar(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}", esperado);
    }

    private static String token(MvcResult login) throws Exception {
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie cookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return new Cookie("refresh_token", setCookie.substring("refresh_token=".length(), setCookie.indexOf(';')));
    }

    private MvcResult enviar(MockHttpServletRequestBuilder requisicao, String token, String corpo, int esperado)
            throws Exception {
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao).andExpect(status().is(esperado)).andReturn();
    }

    private static String id(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    }

    /** Um CNPJ com dígitos verificadores válidos, para os dados deste teste não colidirem com outros. */
    private static String cnpjAleatorio() {
        int[] numeros = new int[14];
        for (int i = 0; i < 8; i++) {
            numeros[i] = ThreadLocalRandom.current().nextInt(10);
        }
        numeros[11] = 1;
        numeros[12] = digito(numeros, 12);
        numeros[13] = digito(numeros, 13);
        StringBuilder cnpj = new StringBuilder();
        for (int n : numeros) {
            cnpj.append(n);
        }
        return cnpj.toString();
    }

    private static int digito(int[] numeros, int tamanho) {
        int[] pesos = tamanho == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += numeros[i] * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
