package com.gestao.temporeal;

import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.MensagemNegociacaoService;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.identidade.aplicacao.AuthService;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.EquipeService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.TipoOrganizacao;
import com.gestao.temporeal.infraestrutura.Destinos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tempo real de ponta a ponta no servidor: servidor de verdade numa porta
 * aleatória e clientes STOMP de verdade, como o navegador faria.
 *
 * Os dados são gravados de fato (sem rollback), porque os avisos só saem depois
 * do commit; por isso a classe usa um banco H2 só dela.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:gestao-tempo-real;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TempoRealIntegrationTest {

    private static final String SENHA = "segredo123";
    private static final long ESPERA_S = 5;
    private static final String AVISOS = Destinos.PREFIXO_USUARIO + Destinos.FILA_AVISOS.substring(1);

    @Value("${local.server.port}")
    private int porta;

    @Autowired private CadastroService cadastroService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private NegociacaoService negociacaoService;
    @Autowired private MensagemNegociacaoService mensagemService;
    @Autowired private AuthService authService;
    @Autowired private EquipeService equipeService;

    private UsuarioAutenticado empresa;
    private UsuarioAutenticado fornecedor;
    private UsuarioAutenticado intruso;
    private WebSocketStompClient cliente;
    private final List<StompSession> sessoes = new ArrayList<>();

    @BeforeAll
    void criarUsuarios() {
        empresa = cadastrar(TipoOrganizacao.EMPRESA, "Hospital Santa Vida", "27.384.915/0001-02", "compras@hospital.com");
        cadastroService.adicionarMembro(empresa.organizacaoId(), "Colega do Hospital", "colega@hospital.com", SENHA);
        fornecedor = cadastrar(TipoOrganizacao.FORNECEDOR, "Limpa Bem", "90.817.263/0001-80", "contato@limpabem.com");
        intruso = cadastrar(TipoOrganizacao.FORNECEDOR, "Clima Frio", "56.102.938/0001-77", "contato@climafrio.com");

        cliente = new WebSocketStompClient(new StandardWebSocketClient());
        cliente.setMessageConverter(new JsonComoTexto());
        cliente.setDefaultHeartbeat(new long[] {0, 0});
    }

    @AfterEach
    void desconectar() {
        sessoes.forEach(s -> {
            if (s.isConnected()) {
                s.disconnect();
            }
        });
        sessoes.clear();
    }

    @Test
    void conexaoSemTokenERecusada() {
        CompletableFuture<StompSession> conexao = cliente.connectAsync(url(), new WebSocketHttpHeaders(),
                new StompHeaders(), new StompSessionHandlerAdapter() {
                });
        assertThrows(Exception.class, () -> conexao.get(ESPERA_S, TimeUnit.SECONDS));
    }

    @Test
    void conexaoComTokenInvalidoERecusada() {
        assertThrows(Exception.class, () -> conectar("token-falso", new Erros()));
    }

    @Test
    void superadminNaoAcompanhaNegociacoes() {
        // O token do superadmin não tem organização: o WebSocket recusa a conexão
        String token = authService.login("admin@plataforma.com", "senhaDoSuperadmin123").resposta().accessToken();
        assertThrows(Exception.class, () -> conectar(token, new Erros()));
    }

    @Test
    void mensagemDoFornecedorChegaNaHoraParaAEquipeDaEmpresa() throws Exception {
        Negociacao negociacao = novaNegociacao();
        StompSession sessaoEmpresa = conectar(token("compras@hospital.com"), new Erros());
        BlockingQueue<String> topico = assinar(sessaoEmpresa, Destinos.negociacao(negociacao.getId()));
        BlockingQueue<String> avisos = assinar(sessaoEmpresa, AVISOS);
        // Outra pessoa da mesma empresa, conectada ao mesmo tempo
        BlockingQueue<String> avisosDoColega = assinar(conectar(token("colega@hospital.com"), new Erros()), AVISOS);

        mensagemService.enviarMensagem(negociacao.getId(), "Consigo entregar em 5 dias", new BigDecimal("7100.00"),
                fornecedor);

        String evento = topico.poll(ESPERA_S, TimeUnit.SECONDS);
        assertNotNull(evento, "a empresa deveria receber a mensagem pelo tópico da negociação");
        assertTrue(evento.contains("\"MENSAGEM\""), evento);
        assertTrue(evento.contains("Consigo entregar em 5 dias"), evento);

        String aviso = avisos.poll(ESPERA_S, TimeUnit.SECONDS);
        assertNotNull(aviso, "a empresa deveria receber o aviso pessoal");
        assertTrue(aviso.contains("Limpa Bem"), aviso);
        assertTrue(aviso.contains("Nova oferta"), aviso);

        String avisoDoColega = avisosDoColega.poll(ESPERA_S, TimeUnit.SECONDS);
        assertNotNull(avisoDoColega, "os avisos são da organização: o colega também deveria receber");
        assertTrue(avisoDoColega.contains("Nova oferta"), avisoDoColega);
    }

    @Test
    void fornecedorEAvisadoQuandoAEmpresaAbreANegociacao() throws Exception {
        Proposta proposta = novaProposta();
        StompSession sessaoFornecedor = conectar(token("contato@limpabem.com"), new Erros());
        BlockingQueue<String> avisos = assinar(sessaoFornecedor, AVISOS);

        negociacaoService.criarNegociacao(proposta.getId(), empresa);

        String aviso = avisos.poll(ESPERA_S, TimeUnit.SECONDS);
        assertNotNull(aviso, "o fornecedor deveria ser avisado da negociação");
        assertTrue(aviso.contains("NEGOCIACAO_INICIADA"), aviso);
        assertTrue(aviso.contains("Hospital Santa Vida"), aviso);
    }

    @Test
    void digitandoChegaParaAOutraParte() throws Exception {
        Negociacao negociacao = novaNegociacao();
        StompSession sessaoFornecedor = conectar(token("contato@limpabem.com"), new Erros());
        BlockingQueue<String> topico = assinar(sessaoFornecedor, Destinos.negociacao(negociacao.getId()));
        StompSession sessaoEmpresa = conectar(token("compras@hospital.com"), new Erros());

        sessaoEmpresa.send("/app/negociacoes/" + negociacao.getId() + "/digitando", "");

        String evento = topico.poll(ESPERA_S, TimeUnit.SECONDS);
        assertNotNull(evento, "o fornecedor deveria ver que a empresa está digitando");
        assertTrue(evento.contains("\"DIGITANDO\""), evento);
        assertTrue(evento.contains("\"EMPRESA\""), evento);
    }

    @Test
    void quemSaiDaOrganizacaoTemAConexaoEncerrada() throws Exception {
        UsuarioAutenticado saindo = cadastroService.adicionarMembro(
                empresa.organizacaoId(), "Pessoa de Saída", "saindo@hospital.com", SENHA);
        Erros erros = new Erros();
        conectar(token("saindo@hospital.com"), erros);
        UUID vinculo = equipeService.listarMembros(empresa).stream()
                .filter(m -> m.getUsuario().getId().equals(saindo.usuarioId()))
                .findFirst().orElseThrow().getId();

        equipeService.removerMembro(empresa, vinculo);

        assertNotNull(erros.primeiro.get(ESPERA_S, TimeUnit.SECONDS), "a conexão deveria ser encerrada");
    }

    @Test
    void quemNaoParticipaNaoAssinaANegociacao() throws Exception {
        Negociacao negociacao = novaNegociacao();
        Erros erros = new Erros();
        StompSession sessaoIntruso = conectar(token("contato@climafrio.com"), erros);
        BlockingQueue<String> topico = assinar(sessaoIntruso, Destinos.negociacao(negociacao.getId()));

        assertNotNull(erros.primeiro.get(ESPERA_S, TimeUnit.SECONDS), "a assinatura deveria ser recusada");

        mensagemService.enviarMensagem(negociacao.getId(), "Só para as partes", null, fornecedor);
        assertNull(topico.poll(1, TimeUnit.SECONDS), "quem não participa não pode receber nada");
    }

    // ---------------------------------------------------------------- apoio

    private String url() {
        return "ws://localhost:" + porta + Destinos.ENDPOINT;
    }

    private StompSession conectar(String token, Erros erros) throws Exception {
        StompHeaders cabecalhos = new StompHeaders();
        cabecalhos.add("Authorization", "Bearer " + token);
        StompSession sessao = cliente.connectAsync(url(), new WebSocketHttpHeaders(), cabecalhos, erros)
                .get(ESPERA_S, TimeUnit.SECONDS);
        sessoes.add(sessao);
        return sessao;
    }

    private BlockingQueue<String> assinar(StompSession sessao, String destino) throws InterruptedException {
        BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        sessao.subscribe(destino, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recebidas.add((String) payload);
            }
        });
        // O SUBSCRIBE é assíncrono: um instante para o broker registrar a assinatura
        Thread.sleep(300);
        return recebidas;
    }

    private String token(String email) {
        return authService.login(email, SENHA).resposta().accessToken();
    }

    private Negociacao novaNegociacao() {
        return negociacaoService.criarNegociacao(novaProposta().getId(), empresa);
    }

    private Proposta novaProposta() {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Limpeza mensal");
        cotacao.setRequisitos("Equipe de 3 pessoas, 3 vezes por semana");
        cotacao.setCategoria(CategoriaCotacao.LIMPEZA_MANUTENCAO);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(5));
        cotacao = cotacaoService.criarCotacao(cotacao, empresa);

        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal("7500.00"));
        proposta.setDescricao("Materiais inclusos");
        return propostaService.criarProposta(proposta, fornecedor, cotacao.getId());
    }

    private UsuarioAutenticado cadastrar(TipoOrganizacao tipo, String razaoSocial, String cnpj, String email) {
        return cadastroService.cadastrar(
                new NovaOrganizacao(tipo, razaoSocial, cnpj, "Pessoa da " + razaoSocial, email, SENHA));
    }

    /** Guarda o primeiro erro da sessão: frame ERROR do servidor ou queda da conexão. */
    private static class Erros extends StompSessionHandlerAdapter {
        final CompletableFuture<String> primeiro = new CompletableFuture<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            primeiro.complete("ERROR: " + headers.getFirst("message"));
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                    byte[] payload, Throwable exception) {
            primeiro.complete(exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            primeiro.complete("conexão encerrada: " + exception.getMessage());
        }
    }

    /** O servidor manda JSON; aqui basta lê-lo como texto e procurar o que interessa. */
    private static class JsonComoTexto extends StringMessageConverter {
        @Override
        protected boolean supportsMimeType(MessageHeaders headers) {
            return true;
        }
    }
}
