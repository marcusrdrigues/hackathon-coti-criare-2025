package com.gestao;

import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.identidade.aplicacao.AuthService;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.EquipeService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.TipoOrganizacao;
import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Isolamento entre organizações (spec 001, R4; ADR 0016).
 *
 * <p>Toda rota da API que recebe um id, no caminho ou no corpo, tem aqui pelo menos um caso:
 * uma pessoa de outra organização tenta usar um recurso da Criare e recebe 404, com a mesma
 * mensagem de um id que não existe. Uma rota nova com id e sem caso cadastrado quebra o build.
 */
@SpringBootTest
@Transactional
class IsolamentoEntreOrganizacoesTest {

    private static final String SENHA = "segredo123";

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping rotas;
    @Autowired private CadastroService cadastroService;
    @Autowired private AuthService authService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private NegociacaoService negociacaoService;
    @Autowired private EquipeService equipeService;

    private MockMvc mvc;

    /** Da Criare: uma cotação aberta com proposta pendente e outra já em negociação. */
    private UUID cotacaoAberta;
    private UUID propostaPendente;
    private UUID cotacaoEmNegociacao;
    private UUID negociacao;
    /** Da equipe da Criare: um membro e um convite pendente. */
    private UUID membroDaCriare;
    private UUID conviteDaCriare;

    /** Uma empresa e um fornecedor que não têm nada com esses dados. */
    private String empresaIntrusa;
    private String fornecedorIntruso;

    /** Uma tentativa: quem tenta e a requisição, montada com o id real ou com um que não existe. */
    private record Caso(String quem, String token, UUID id, Function<UUID, MockHttpServletRequestBuilder> requisicao) {}

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        UsuarioAutenticado criare = cadastrar(TipoOrganizacao.EMPRESA, "Criare Consulting", "11.222.333/0001-81",
                "compras@criare.com");
        UsuarioAutenticado tech = cadastrar(TipoOrganizacao.FORNECEDOR, "Tech Soluções", "45.236.789/0001-12",
                "vendas@tech.com");
        cadastrar(TipoOrganizacao.EMPRESA, "Concorrente SA", "90.817.263/0001-80", "intrusa@concorrente.com");
        cadastrar(TipoOrganizacao.FORNECEDOR, "Outro Fornecedor", "78.345.129/0001-29", "intruso@fornecedor.com");

        cotacaoAberta = cotacaoService.criarCotacao(cotacao(), criare).getId();
        propostaPendente = propostaService.criarProposta(proposta(), tech, cotacaoAberta).getId();

        cotacaoEmNegociacao = cotacaoService.criarCotacao(cotacao(), criare).getId();
        Proposta aceita = propostaService.criarProposta(proposta(), tech, cotacaoEmNegociacao);
        negociacao = negociacaoService.criarNegociacao(aceita.getId(), criare).getId();

        cadastroService.adicionarMembro(criare.organizacaoId(), "Bruno Costa", "bruno@criare.com", SENHA);
        membroDaCriare = equipeService.listarMembros(criare).stream()
                .filter(m -> m.getUsuario().getEmail().equals("bruno@criare.com")).findFirst().orElseThrow().getId();
        conviteDaCriare = equipeService.convidar(criare, "Carla Dias", "carla@criare.com").convite().getId();

        empresaIntrusa = token("intrusa@concorrente.com");
        fornecedorIntruso = token("intruso@fornecedor.com");
    }

    /** As tentativas, por rota. A chave é exatamente como o Spring registra a rota. */
    private Map<String, List<Caso>> casos() {
        String cotacaoJson = """
                {"nomeServico":"Invasão","requisitos":"Trocar os requisitos de outra empresa"}""";
        Map<String, List<Caso>> casos = new LinkedHashMap<>();

        casos.put("GET /api/v1/cotacoes/{id}", List.of(
                new Caso("outra empresa", empresaIntrusa, cotacaoAberta, id -> get("/api/v1/cotacoes/" + id)),
                // Fora do mural e sem proposta dele, a cotação não existe para o fornecedor
                new Caso("fornecedor sem proposta", fornecedorIntruso, cotacaoEmNegociacao,
                        id -> get("/api/v1/cotacoes/" + id))));
        casos.put("PUT /api/v1/cotacoes/{id}", List.of(
                new Caso("outra empresa", empresaIntrusa, cotacaoAberta,
                        id -> json(put("/api/v1/cotacoes/" + id), cotacaoJson))));
        casos.put("PATCH /api/v1/cotacoes/{id}/cancelar", List.of(
                new Caso("outra empresa", empresaIntrusa, cotacaoAberta, id -> patch("/api/v1/cotacoes/" + id + "/cancelar"))));

        casos.put("POST /api/v1/propostas", List.of(
                new Caso("fornecedor sem acesso à cotação", fornecedorIntruso, cotacaoEmNegociacao,
                        id -> json(post("/api/v1/propostas"), """
                                {"valor":100.00,"descricao":"Proposta","cotacaoId":"%s"}""".formatted(id)))));
        casos.put("GET /api/v1/propostas/{id}", List.of(
                new Caso("outra empresa", empresaIntrusa, propostaPendente, id -> get("/api/v1/propostas/" + id)),
                new Caso("fornecedor concorrente", fornecedorIntruso, propostaPendente, id -> get("/api/v1/propostas/" + id))));
        casos.put("GET /api/v1/propostas/cotacao/{cotacaoId}", List.of(
                new Caso("outra empresa", empresaIntrusa, cotacaoAberta, id -> get("/api/v1/propostas/cotacao/" + id))));
        casos.put("PATCH /api/v1/propostas/{id}/recusar", List.of(
                new Caso("outra empresa", empresaIntrusa, propostaPendente, id -> patch("/api/v1/propostas/" + id + "/recusar"))));
        casos.put("DELETE /api/v1/propostas/{id}", List.of(
                new Caso("fornecedor concorrente", fornecedorIntruso, propostaPendente, id -> delete("/api/v1/propostas/" + id))));

        casos.put("POST /api/v1/negociacoes", List.of(
                new Caso("outra empresa", empresaIntrusa, propostaPendente,
                        id -> json(post("/api/v1/negociacoes"), "{\"propostaId\":\"" + id + "\"}"))));
        casos.put("GET /api/v1/negociacoes/{id}", List.of(
                new Caso("outra empresa", empresaIntrusa, negociacao, id -> get("/api/v1/negociacoes/" + id)),
                new Caso("fornecedor concorrente", fornecedorIntruso, negociacao, id -> get("/api/v1/negociacoes/" + id))));
        casos.put("PATCH /api/v1/negociacoes/{id}/leitura", List.of(
                new Caso("outra empresa", empresaIntrusa, negociacao, id -> patch("/api/v1/negociacoes/" + id + "/leitura")),
                new Caso("fornecedor concorrente", fornecedorIntruso, negociacao,
                        id -> patch("/api/v1/negociacoes/" + id + "/leitura"))));
        casos.put("PATCH /api/v1/negociacoes/{id}/finalizar", List.of(
                new Caso("outra empresa", empresaIntrusa, negociacao,
                        id -> json(patch("/api/v1/negociacoes/" + id + "/finalizar"), "{\"valorFinal\":1.00}"))));
        casos.put("PATCH /api/v1/negociacoes/{id}/cancelar", List.of(
                new Caso("outra empresa", empresaIntrusa, negociacao, id -> patch("/api/v1/negociacoes/" + id + "/cancelar"))));

        casos.put("POST /api/v1/mensagens", List.of(
                new Caso("outra empresa", empresaIntrusa, negociacao,
                        id -> json(post("/api/v1/mensagens"), "{\"negociacaoId\":\"" + id + "\",\"mensagem\":\"oi\"}")),
                new Caso("fornecedor concorrente", fornecedorIntruso, negociacao,
                        id -> json(post("/api/v1/mensagens"), "{\"negociacaoId\":\"" + id + "\",\"mensagem\":\"oi\"}"))));
        casos.put("GET /api/v1/mensagens/negociacao/{negociacaoId}", List.of(
                new Caso("outra empresa", empresaIntrusa, negociacao, id -> get("/api/v1/mensagens/negociacao/" + id)),
                new Caso("fornecedor concorrente", fornecedorIntruso, negociacao,
                        id -> get("/api/v1/mensagens/negociacao/" + id))));

        // A empresa intrusa é proprietária da organização dela, então passa pelo @PreAuthorize
        casos.put("DELETE /api/v1/equipe/membros/{id}", List.of(
                new Caso("proprietária de outra organização", empresaIntrusa, membroDaCriare,
                        id -> delete("/api/v1/equipe/membros/" + id))));
        casos.put("DELETE /api/v1/equipe/convites/{id}", List.of(
                new Caso("proprietária de outra organização", empresaIntrusa, conviteDaCriare,
                        id -> delete("/api/v1/equipe/convites/" + id))));
        return casos;
    }

    @Test
    void todaRotaComIdTemTesteDeIsolamento() {
        // Se este teste falhou por causa de uma rota nova: cadastre em casos() a tentativa de
        // outra organização. Se a rota saiu, tire o caso dela.
        assertThat(casos().keySet()).as("rotas com id e os casos de isolamento cadastrados")
                .containsExactlyInAnyOrderElementsOf(rotasComId());
    }

    @Test
    void outraOrganizacaoRecebe404ComoSeORecursoNaoExistisse() throws Exception {
        SoftAssertions verificacao = new SoftAssertions();
        for (Map.Entry<String, List<Caso>> rota : casos().entrySet()) {
            for (Caso caso : rota.getValue()) {
                String descricao = rota.getKey() + " (" + caso.quem() + ")";
                MvcResult real = enviar(caso, caso.id());
                MvcResult inexistente = enviar(caso, UUID.randomUUID());

                verificacao.assertThat(real.getResponse().getStatus()).as(descricao).isEqualTo(404);
                verificacao.assertThat(mensagem(real)).as(descricao + ": mesma mensagem de um id inexistente")
                        .isEqualTo(mensagem(inexistente));
            }
        }
        verificacao.assertAll();
    }

    // ---------------------------------------------------------------- apoio

    /** Rotas da aplicação que recebem um id no caminho ou num campo UUID do corpo. */
    private Set<String> rotasComId() {
        Set<String> comId = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> rota : rotas.getHandlerMethods().entrySet()) {
            HandlerMethod metodo = rota.getValue();
            if (!metodo.getBeanType().getPackageName().startsWith("com.gestao")) {
                continue;
            }
            boolean idNoCorpo = Arrays.stream(metodo.getMethodParameters()).anyMatch(this::corpoComId);
            for (String caminho : rota.getKey().getPatternValues()) {
                if (caminho.contains("{") || idNoCorpo) {
                    rota.getKey().getMethodsCondition().getMethods()
                            .forEach(verbo -> comId.add(verbo.name() + " " + caminho));
                }
            }
        }
        return comId;
    }

    private boolean corpoComId(MethodParameter parametro) {
        Class<?> tipo = parametro.getParameterType();
        return parametro.hasParameterAnnotation(RequestBody.class) && tipo.isRecord()
                && Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getType).anyMatch(UUID.class::equals);
    }

    private MvcResult enviar(Caso caso, UUID id) throws Exception {
        return mvc.perform(caso.requisicao().apply(id).header(HttpHeaders.AUTHORIZATION, caso.token())).andReturn();
    }

    private static String mensagem(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.detail");
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private UsuarioAutenticado cadastrar(TipoOrganizacao tipo, String razaoSocial, String cnpj, String email) {
        return cadastroService.cadastrar(
                new NovaOrganizacao(tipo, razaoSocial, cnpj, "Pessoa da " + razaoSocial, email, SENHA));
    }

    private String token(String email) {
        return "Bearer " + authService.login(email, SENHA).resposta().accessToken();
    }

    private static Cotacao cotacao() {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Notebooks");
        cotacao.setRequisitos("10 notebooks");
        cotacao.setCategoria(CategoriaCotacao.TECNOLOGIA);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(5));
        return cotacao;
    }

    private static Proposta proposta() {
        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal("1000.00"));
        proposta.setDescricao("Entrega em 10 dias");
        return proposta;
    }
}
