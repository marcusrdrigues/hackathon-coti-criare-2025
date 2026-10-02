package com.gestao.auditoria;

import com.gestao.auditoria.dominio.OrigemDaRevisao;
import com.gestao.auditoria.infraestrutura.persistencia.Revisao;
import com.gestao.compartilhado.infraestrutura.web.RastreioDeRequisicao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.dominio.Membro;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O histórico de alterações (spec 003, R1). Sem {@code @Transactional} de propósito: o Envers
 * grava o histórico na confirmação da transação, então cada requisição precisa confirmar a sua.
 * Por isso os dados de cada teste são únicos (CNPJ e e-mail sorteados).
 */
@SpringBootTest
class HistoricoDeAlteracoesTest {

    private static final String SENHA = "segredo123";

    @Autowired private WebApplicationContext contexto;
    @Autowired private CadastroService cadastroService;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transacao;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String sufixo;
    private String empresa;
    private String emailEmpresa;
    private String fornecedor;

    @BeforeEach
    void setUp() throws Exception {
        // Com os filtros de observação e de rastreio, como na aplicação: a revisão guarda o traceId
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(new ServerHttpObservationFilter(contexto.getBean(ObservationRegistry.class)),
                        contexto.getBean(RastreioDeRequisicao.class))
                .apply(springSecurity())
                .build();
        sufixo = UUID.randomUUID().toString().substring(0, 8);
        emailEmpresa = "historico-" + sufixo + "@empresa.com";
        String emailFornecedor = "historico-" + sufixo + "@fornecedor.com";
        cadastrar("EMPRESA", "Empresa Histórico " + sufixo, emailEmpresa);
        cadastrar("FORNECEDOR", "Fornecedor Histórico " + sufixo, emailFornecedor);
        empresa = entrar(emailEmpresa);
        fornecedor = entrar(emailFornecedor);
    }

    @Test
    void edicaoGuardaOsValoresAnterioresEQuemMudou() throws Exception {
        String id = publicarCotacao("Cadeiras " + sufixo, "12000.00", 5);
        enviar(put("/api/v1/cotacoes/" + id), empresa, cotacaoJson("Cadeiras " + sufixo, "15000.00", 9), 200);

        UUID cotacaoId = UUID.fromString(id);
        List<Number> revisoes = ler(leitor -> leitor.getRevisions(Cotacao.class, cotacaoId));
        assertThat(revisoes).hasSize(2);

        Cotacao antes = ler(leitor -> leitor.find(Cotacao.class, cotacaoId, revisoes.get(0)));
        Cotacao depois = ler(leitor -> leitor.find(Cotacao.class, cotacaoId, revisoes.get(1)));
        assertThat(antes.getOrcamentoEstimado()).isEqualByComparingTo("12000.00");
        assertThat(depois.getOrcamentoEstimado()).isEqualByComparingTo("15000.00");
        assertThat(depois.getDataLimite()).isAfter(antes.getDataLimite());

        Revisao edicao = ler(leitor -> leitor.findRevision(Revisao.class, revisoes.get(1)));
        assertThat(edicao.getOrigem()).isEqualTo(OrigemDaRevisao.PESSOA);
        assertThat(edicao.getUsuarioId()).isEqualTo(idDaPessoa(emailEmpresa));
        assertThat(edicao.getOrganizacaoId()).isEqualTo(idDaOrganizacao(emailEmpresa));
        assertThat(edicao.getTraceId()).hasSize(32);
        assertThat(edicao.getMomento()).isPositive();
        assertThat(edicao.getEntidades()).contains(Cotacao.class.getName());
    }

    @Test
    void propostaRetiradaContinuaNoHistoricoComQuemRetirou() throws Exception {
        String cotacao = publicarCotacao("Notebooks " + sufixo, "50000.00", 5);
        String proposta = id(enviar(post("/api/v1/propostas"), fornecedor, """
                {"valor":48000.00,"descricao":"Entrega em 10 dias","cotacaoId":"%s"}
                """.formatted(cotacao), 201));
        mvc.perform(delete("/api/v1/propostas/" + proposta).header(HttpHeaders.AUTHORIZATION, fornecedor))
                .andExpect(status().isNoContent());

        UUID propostaId = UUID.fromString(proposta);
        List<Object[]> versoes = ler(leitor -> {
            @SuppressWarnings("unchecked")
            List<Object[]> lista = leitor.createQuery()
                    .forRevisionsOfEntity(Proposta.class, false, true)
                    .add(AuditEntity.id().eq(propostaId))
                    .addOrder(AuditEntity.revisionNumber().asc())
                    .getResultList();
            return lista;
        });
        assertThat(versoes).hasSize(2);
        Object[] retirada = versoes.get(1);
        assertThat(retirada[2]).isEqualTo(RevisionType.DEL);
        // O último estado fica guardado: dá para saber o que foi retirado
        assertThat(((Proposta) retirada[0]).getValor()).isEqualByComparingTo(new BigDecimal("48000.00"));
        assertThat(((Revisao) retirada[1]).getUsuarioId())
                .isEqualTo(idDaPessoa("historico-" + sufixo + "@fornecedor.com"));
    }

    @Test
    void cadastroPublicoEAlteracaoDoSistemaTemAOrigemCerta() {
        // O cadastro é uma rota pública: quem age é a pessoa que está entrando
        UUID vinculo = idDoVinculo(emailEmpresa);
        Revisao cadastro = ler(leitor -> leitor.findRevision(Revisao.class, ultimaRevisaoDe(leitor, vinculo)));
        assertThat(cadastro.getOrigem()).isEqualTo(OrigemDaRevisao.PUBLICO);
        assertThat(cadastro.getUsuarioId()).isNull();

        // Fora de uma requisição e sem ninguém logado, como uma rotina agendada: é o próprio sistema.
        // Numa thread própria, porque a do teste carrega uma requisição simulada do Spring.
        UUID organizacao = idDaOrganizacao(emailEmpresa);
        CompletableFuture.runAsync(() -> cadastroService.adicionarMembro(organizacao, "Pessoa do Sistema",
                "sistema-" + sufixo + "@empresa.com", SENHA)).join();

        UUID membro = idDoVinculo("sistema-" + sufixo + "@empresa.com");
        Revisao revisao = ler(leitor -> leitor.findRevision(Revisao.class, ultimaRevisaoDe(leitor, membro)));
        assertThat(revisao.getOrigem()).isEqualTo(OrigemDaRevisao.SISTEMA);
        assertThat(revisao.getUsuarioId()).isNull();
    }

    @Test
    void credenciaisNuncaEntramNoHistorico() {
        List<String> colunas = jdbc.queryForList("""
                SELECT table_name || '.' || column_name FROM information_schema.columns
                WHERE table_name LIKE '%\\_aud' ESCAPE '\\' OR table_name LIKE 'tb\\_revisao%' ESCAPE '\\'
                """, String.class);
        assertThat(colunas).isNotEmpty().noneMatch(coluna -> coluna.contains("hash") || coluna.contains("senha")
                || coluna.contains("token"));
    }

    // ---------------------------------------------------------------- apoio

    /** O id do vínculo (membro) da pessoa com este e-mail. */
    private UUID idDoVinculo(String email) {
        return jdbc.queryForObject("SELECT m.id FROM tb_membro m JOIN tb_usuario u ON u.id = m.usuario_id "
                + "WHERE u.email = ?", UUID.class, email);
    }

    private UUID idDaOrganizacao(String email) {
        return jdbc.queryForObject("SELECT m.organizacao_id FROM tb_membro m JOIN tb_usuario u ON u.id = m.usuario_id "
                + "WHERE u.email = ?", UUID.class, email);
    }

    private UUID idDaPessoa(String email) {
        return jdbc.queryForObject("SELECT id FROM tb_usuario WHERE email = ?", UUID.class, email);
    }

    private static Number ultimaRevisaoDe(AuditReader leitor, UUID membroId) {
        return leitor.getRevisions(Membro.class, membroId).getLast();
    }

    private <T> T ler(Function<AuditReader, T> consulta) {
        return transacao.execute(status -> consulta.apply(AuditReaderFactory.get(entityManager)));
    }

    private String publicarCotacao(String nome, String orcamento, int dias) throws Exception {
        return id(enviar(post("/api/v1/cotacoes"), empresa, cotacaoJson(nome, orcamento, dias), 201));
    }

    private static String cotacaoJson(String nome, String orcamento, int dias) {
        return """
                {"nomeServico":"%s","requisitos":"Com regulagem de altura.","categoria":"MOBILIARIO",
                 "orcamentoEstimado":%s,"dataLimite":"%s"}
                """.formatted(nome, orcamento, LocalDateTime.now().plusDays(dias).withNano(0));
    }

    private void cadastrar(String tipo, String razaoSocial, String email) throws Exception {
        enviar(post("/api/v1/cadastro"), null, """
                {"tipo":"%s","razaoSocial":"%s","cnpj":"%s","nome":"Pessoa %s","email":"%s","senha":"%s"}
                """.formatted(tipo, razaoSocial, cnpjAleatorio(), sufixo, email, SENHA), 201);
    }

    private String entrar(String email) throws Exception {
        MvcResult login = enviar(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}", 200);
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
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
