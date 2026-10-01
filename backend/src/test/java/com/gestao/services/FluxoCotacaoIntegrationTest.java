package com.gestao.services;

import com.gestao.dtos.dashboard.DashboardEmpresaResponse;
import com.gestao.dtos.dashboard.DashboardFornecedorResponse;
import com.gestao.entities.Cotacao;
import com.gestao.entities.Empresa;
import com.gestao.entities.Fornecedor;
import com.gestao.entities.MensagemNegociacao;
import com.gestao.entities.Negociacao;
import com.gestao.entities.Proposta;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.enums.StatusProposta;
import com.gestao.enums.TipoRemetente;
import com.gestao.enums.TipoUsuario;
import com.gestao.exceptions.AcessoNegadoException;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.exceptions.UnauthorizedException;
import com.gestao.repositories.MensagemNegociacaoRepository;
import com.gestao.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Percorre o fluxo principal da plataforma usando os services de verdade
 * sobre um banco H2 em memória. Cada teste roda numa transação desfeita no fim.
 */
@SpringBootTest
@Transactional
class FluxoCotacaoIntegrationTest {

    @Autowired private EmpresaService empresaService;
    @Autowired private FornecedorService fornecedorService;
    @Autowired private AuthService authService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private NegociacaoService negociacaoService;
    @Autowired private MensagemNegociacaoService mensagemService;
    @Autowired private DashboardService dashboardService;
    @Autowired private MensagemNegociacaoRepository mensagemRepository;

    private Empresa empresa;
    private Fornecedor fornecedorA;
    private Fornecedor fornecedorB;

    @BeforeEach
    void setUp() {
        empresa = empresaService.cadastrarEmpresa(novaEmpresa("Criare Consulting", "11.222.333/0001-81", "Compras@Criare.com"));
        fornecedorA = fornecedorService.cadastrarFornecedor(novoFornecedor("Tech Soluções", "45.236.789/0001-12", "tech@fornecedor.com"));
        fornecedorB = fornecedorService.cadastrarFornecedor(novoFornecedor("InfoWorld", "78.345.129/0001-29", "info@fornecedor.com"));
    }

    @Test
    void cadastroNormalizaDadosEGuardaHashDaSenha() {
        assertEquals("11222333000181", empresa.getCnpj());
        assertEquals("compras@criare.com", empresa.getEmail());
        assertTrue(empresa.getSenha().startsWith("$2"), "a senha deve ser gravada com BCrypt");
        assertEquals("EMPRESA", empresa.getPerfil().getNome());
    }

    @Test
    void loginIdentificaOTipoDeUsuario() {
        var loginEmpresa = authService.login("COMPRAS@criare.com", "segredo123").resposta();
        assertEquals(TipoUsuario.EMPRESA, loginEmpresa.usuario().tipo());
        assertEquals(empresa.getId(), loginEmpresa.usuario().id());

        var loginFornecedor = authService.login("tech@fornecedor.com", "segredo123").resposta();
        assertEquals(TipoUsuario.FORNECEDOR, loginFornecedor.usuario().tipo());

        assertThrows(UnauthorizedException.class, () -> authService.login("tech@fornecedor.com", "errada"));
        assertThrows(UnauthorizedException.class, () -> authService.login("ninguem@x.com", "segredo123"));
    }

    @Test
    void emailNaoPodeSeRepetirEntreEmpresaEFornecedor() {
        assertThrows(DuplicateResourceException.class, () -> fornecedorService.cadastrarFornecedor(
                novoFornecedor("Outro", "90.817.263/0001-80", "compras@criare.com")));
    }

    @Test
    void cnpjInvalidoEhRecusado() {
        assertThrows(BusinessException.class, () -> empresaService.cadastrarEmpresa(
                novaEmpresa("Empresa X", "11.222.333/0001-00", "x@empresa.com")));
    }

    @Test
    void fluxoCompletoAteFecharONegocio() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "10000.00");
        Proposta propostaB = enviarProposta(fornecedorB, cotacao, "9500.00");

        // Mesmo fornecedor não envia duas propostas para a mesma cotação
        assertThrows(BusinessException.class, () -> enviarProposta(fornecedorA, cotacao, "9000.00"));

        // Empresa escolhe negociar com o fornecedor A
        Negociacao negociacao = negociacaoService.criarNegociacao(propostaA.getId(), empresa.getId());
        assertEquals(StatusProposta.ACEITA, propostaA.getStatus());
        assertEquals(StatusCotacao.EM_NEGOCIACAO, cotacao.getStatus());
        assertEquals(new BigDecimal("10000.00"), negociacao.getUltimaOferta());

        // A proposta original abre o histórico
        List<MensagemNegociacao> historico = mensagemRepository.findByNegociacaoIdOrderByDataEnvioAsc(negociacao.getId());
        assertEquals(1, historico.size());
        assertEquals(TipoRemetente.FORNECEDOR, historico.get(0).getTipoRemetente());

        // Não dá para abrir uma segunda negociação na mesma cotação
        assertThrows(BusinessException.class, () -> negociacaoService.criarNegociacao(propostaB.getId(), empresa.getId()));

        // Contraproposta da empresa e resposta do fornecedor
        mensagemService.enviarMensagem(negociacao.getId(), "Consegue fazer por 9.000?",
                new BigDecimal("9000.00"), comoEmpresa());
        mensagemService.enviarMensagem(negociacao.getId(), null,
                new BigDecimal("9200.00"), comoFornecedor(fornecedorA));
        assertEquals(new BigDecimal("9200.00"), negociacao.getUltimaOferta());

        // Quem não participa da negociação não pode enviar mensagem
        assertThrows(AcessoNegadoException.class, () -> mensagemService.enviarMensagem(negociacao.getId(), "oi",
                null, comoFornecedor(fornecedorB)));

        // Fechamento
        negociacaoService.finalizarNegociacao(negociacao.getId(), new BigDecimal("9200.00"), empresa.getId());
        assertEquals(StatusNegociacao.FINALIZADA, negociacao.getStatus());
        assertEquals(StatusCotacao.FECHADA, cotacao.getStatus());
        assertEquals(StatusProposta.RECUSADA, propostaB.getStatus());

        // Negociação encerrada não recebe mais mensagens
        assertThrows(BusinessException.class, () -> mensagemService.enviarMensagem(negociacao.getId(), "oi",
                null, comoEmpresa()));

        DashboardFornecedorResponse painelA = dashboardService.resumoFornecedor(fornecedorA.getId());
        assertEquals(1, painelA.cotacoesGanhas());
        assertEquals(0, new BigDecimal("9200.00").compareTo(painelA.valorTotalGanho()));
    }

    @Test
    void cancelarNegociacaoReabreACotacao() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "10000.00");
        Proposta propostaB = enviarProposta(fornecedorB, cotacao, "9800.00");

        Negociacao negociacao = negociacaoService.criarNegociacao(propostaA.getId(), empresa.getId());
        negociacaoService.cancelarNegociacao(negociacao.getId(), empresa.getId());

        assertEquals(StatusNegociacao.CANCELADA, negociacao.getStatus());
        assertEquals(StatusProposta.RECUSADA, propostaA.getStatus());
        assertEquals(StatusCotacao.ABERTA, cotacao.getStatus());

        // Agora a empresa pode negociar com o outro fornecedor
        Negociacao segunda = negociacaoService.criarNegociacao(propostaB.getId(), empresa.getId());
        assertNotEquals(negociacao.getId(), segunda.getId());
    }

    @Test
    void cotacaoComPrazoVencidoSaiDoMuralENaoRecebePropostas() {
        Cotacao cotacao = criarCotacao(2);
        assertTrue(cotacaoService.listarCotacoesAbertas().stream().anyMatch(c -> c.getId().equals(cotacao.getId())));

        cotacao.setDataLimite(LocalDateTime.now().minusMinutes(1));

        assertTrue(cotacaoService.listarCotacoesAbertas().stream().noneMatch(c -> c.getId().equals(cotacao.getId())));
        assertThrows(BusinessException.class, () -> enviarProposta(fornecedorA, cotacao, "500.00"));
    }

    @Test
    void naoCriaCotacaoComDataLimiteNoPassado() {
        Cotacao cotacao = novaCotacao(-1);
        assertThrows(BusinessException.class, () -> cotacaoService.criarCotacao(cotacao, empresa.getId()));
    }

    @Test
    void cancelarCotacaoRecusaPropostasPendentes() {
        Cotacao cotacao = criarCotacao(5);
        Proposta proposta = enviarProposta(fornecedorA, cotacao, "700.00");

        cotacaoService.cancelarCotacao(cotacao.getId(), empresa.getId());

        assertEquals(StatusCotacao.CANCELADA, cotacao.getStatus());
        assertEquals(StatusProposta.RECUSADA, proposta.getStatus());
    }

    @Test
    void dashboardDaEmpresaConsolidaOsNumeros() {
        Cotacao c1 = criarCotacao(5);
        criarCotacao(5);
        enviarProposta(fornecedorA, c1, "100.00");
        enviarProposta(fornecedorB, c1, "90.00");

        DashboardEmpresaResponse painel = dashboardService.resumoEmpresa(empresa.getId());

        assertEquals(2, painel.cotacoesAbertas());
        assertEquals(2, painel.propostasRecebidas());
        assertEquals(1, painel.categorias().size());
        assertEquals(CategoriaCotacao.TECNOLOGIA.name(), painel.categorias().get(0).categoria());
        assertEquals(100, painel.categorias().get(0).percentual());
        assertEquals(2, painel.topFornecedores().size());
    }

    @Test
    void outraEmpresaNaoVeNemAlteraACotacao() {
        Empresa concorrente = empresaService.cadastrarEmpresa(
                novaEmpresa("Concorrente SA", "90.817.263/0001-80", "compras@concorrente.com"));
        Cotacao cotacao = criarCotacao(5);
        enviarProposta(fornecedorA, cotacao, "500.00");

        UsuarioAutenticado outra = new UsuarioAutenticado(concorrente.getId(), TipoUsuario.EMPRESA);
        assertThrows(AcessoNegadoException.class, () -> cotacaoService.buscarParaUsuario(cotacao.getId(), outra));
        assertThrows(AcessoNegadoException.class, () -> cotacaoService.cancelarCotacao(cotacao.getId(), concorrente.getId()));
        assertThrows(AcessoNegadoException.class, () -> propostaService.listarPorCotacao(cotacao.getId(), concorrente.getId()));

        // Fornecedores enxergam qualquer cotação (é o mural)
        assertEquals(cotacao.getId(), cotacaoService.buscarParaUsuario(cotacao.getId(), comoFornecedor(fornecedorB)).getId());
    }

    @Test
    void fornecedorNaoVePropostaDoConcorrente() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "500.00");

        assertThrows(AcessoNegadoException.class,
                () -> propostaService.buscarParaUsuario(propostaA.getId(), comoFornecedor(fornecedorB)));
        assertThrows(AcessoNegadoException.class,
                () -> propostaService.deletarProposta(propostaA.getId(), fornecedorB.getId()));
        assertEquals(propostaA.getId(),
                propostaService.buscarParaUsuario(propostaA.getId(), comoFornecedor(fornecedorA)).getId());
    }

    @Test
    void fornecedorNaoFechaNegocioNemVeNegociacaoDosOutros() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "500.00");
        Negociacao negociacao = negociacaoService.criarNegociacao(propostaA.getId(), empresa.getId());

        assertThrows(AcessoNegadoException.class,
                () -> negociacaoService.finalizarNegociacao(negociacao.getId(), BigDecimal.TEN, fornecedorA.getId()));
        assertThrows(AcessoNegadoException.class,
                () -> negociacaoService.buscarParaParticipante(negociacao.getId(), comoFornecedor(fornecedorB)));
        assertThrows(AcessoNegadoException.class,
                () -> mensagemService.listarMensagens(negociacao.getId(), comoFornecedor(fornecedorB)));
    }

    // ---------------------------------------------------------------- helpers

    private UsuarioAutenticado comoEmpresa() {
        return new UsuarioAutenticado(empresa.getId(), TipoUsuario.EMPRESA);
    }

    private UsuarioAutenticado comoFornecedor(Fornecedor fornecedor) {
        return new UsuarioAutenticado(fornecedor.getId(), TipoUsuario.FORNECEDOR);
    }

    private Cotacao criarCotacao(int diasDePrazo) {
        return cotacaoService.criarCotacao(novaCotacao(diasDePrazo), empresa.getId());
    }

    private Cotacao novaCotacao(int diasDePrazo) {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Compra de notebooks");
        cotacao.setRequisitos("10 notebooks com 16 GB de RAM");
        cotacao.setCategoria(CategoriaCotacao.TECNOLOGIA);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(diasDePrazo));
        return cotacao;
    }

    private Proposta enviarProposta(Fornecedor fornecedor, Cotacao cotacao, String valor) {
        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal(valor));
        proposta.setDescricao("Entrega em 10 dias");
        return propostaService.criarProposta(proposta, fornecedor.getId(), cotacao.getId());
    }

    private Empresa novaEmpresa(String nome, String cnpj, String email) {
        Empresa e = new Empresa();
        e.setRazaoSocial(nome);
        e.setCnpj(cnpj);
        e.setEmail(email);
        e.setSenha("segredo123");
        return e;
    }

    private Fornecedor novoFornecedor(String nome, String cnpj, String email) {
        Fornecedor f = new Fornecedor();
        f.setNomeCompleto(nome);
        f.setCnpj(cnpj);
        f.setEmail(email);
        f.setSenha("segredo123");
        return f;
    }
}
