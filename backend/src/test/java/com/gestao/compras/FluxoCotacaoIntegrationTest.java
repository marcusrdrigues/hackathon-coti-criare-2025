package com.gestao.compras;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.MensagemNegociacaoService;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.aplicacao.porta.MensagemNegociacaoRepositorio;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.compras.dominio.TipoRemetente;
import com.gestao.identidade.aplicacao.AuthService;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.aplicacao.porta.OrganizacaoRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;
import com.gestao.identidade.dominio.Usuario;
import com.gestao.painel.aplicacao.PainelService;
import com.gestao.painel.aplicacao.dto.PainelEmpresaResponse;
import com.gestao.painel.aplicacao.dto.PainelFornecedorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Autowired private CadastroService cadastroService;
    @Autowired private OrganizacaoRepositorio organizacaoRepositorio;
    @Autowired private UsuarioRepositorio usuarioRepositorio;
    @Autowired private AuthService authService;
    @Autowired private CotacaoService cotacaoService;
    @Autowired private PropostaService propostaService;
    @Autowired private NegociacaoService negociacaoService;
    @Autowired private MensagemNegociacaoService mensagemService;
    @Autowired private PainelService dashboardService;
    @Autowired private MensagemNegociacaoRepositorio mensagemRepositorio;

    private UsuarioAutenticado empresa;
    private UsuarioAutenticado fornecedorA;
    private UsuarioAutenticado fornecedorB;

    @BeforeEach
    void setUp() {
        empresa = cadastrar(TipoOrganizacao.EMPRESA, "Criare Consulting", "11.222.333/0001-81", "Compras@Criare.com");
        fornecedorA = cadastrar(TipoOrganizacao.FORNECEDOR, "Tech Soluções", "45.236.789/0001-12", "tech@fornecedor.com");
        fornecedorB = cadastrar(TipoOrganizacao.FORNECEDOR, "InfoWorld", "78.345.129/0001-29", "info@fornecedor.com");
    }

    @Test
    void cadastroCriaOrganizacaoEProprietarioComDadosNormalizados() {
        assertEquals(TipoOrganizacao.EMPRESA, empresa.tipo());
        assertEquals(Papel.PROPRIETARIO, empresa.papel());
        assertNotEquals(empresa.organizacaoId(), empresa.usuarioId());

        Organizacao organizacao = organizacaoRepositorio.buscarPorId(empresa.organizacaoId()).orElseThrow();
        assertEquals("11222333000181", organizacao.getCnpj());
        assertEquals("Criare Consulting", organizacao.getRazaoSocial());

        Usuario pessoa = usuarioRepositorio.buscarPorId(empresa.usuarioId()).orElseThrow();
        assertEquals("compras@criare.com", pessoa.getEmail());
        assertEquals("Pessoa da Criare Consulting", pessoa.getNome());
        assertTrue(pessoa.getSenhaHash().startsWith("$2"), "a senha deve ser gravada com BCrypt");
    }

    @Test
    void loginIdentificaPessoaOrganizacaoEPapel() {
        var loginEmpresa = authService.login("COMPRAS@criare.com", "segredo123").resposta();
        assertEquals(TipoOrganizacao.EMPRESA, loginEmpresa.usuario().tipo());
        assertEquals(empresa.usuarioId(), loginEmpresa.usuario().id());
        assertEquals(empresa.organizacaoId(), loginEmpresa.usuario().organizacao().id());
        assertEquals(Papel.PROPRIETARIO, loginEmpresa.usuario().papel());

        var loginFornecedor = authService.login("tech@fornecedor.com", "segredo123").resposta();
        assertEquals(TipoOrganizacao.FORNECEDOR, loginFornecedor.usuario().tipo());

        assertThrows(NaoAutenticadoException.class, () -> authService.login("tech@fornecedor.com", "errada"));
        assertThrows(NaoAutenticadoException.class, () -> authService.login("ninguem@x.com", "segredo123"));
    }

    @Test
    void emailNaoPodeSeRepetirNaPlataforma() {
        assertThrows(RecursoDuplicadoException.class, () -> cadastrar(TipoOrganizacao.FORNECEDOR,
                "Outro", "90.817.263/0001-80", "compras@criare.com"));
        assertThrows(RecursoDuplicadoException.class, () -> cadastroService.adicionarMembro(
                fornecedorA.organizacaoId(), "Outra pessoa", "tech@fornecedor.com", "segredo123"));
    }

    @Test
    void cnpjEhUnicoPorTipo() {
        // O mesmo CNPJ não se cadastra duas vezes como empresa...
        assertThrows(RecursoDuplicadoException.class, () -> cadastrar(TipoOrganizacao.EMPRESA,
                "Criare de novo", "11222333000181", "outra@criare.com"));
        // ...mas a mesma empresa pode também ser fornecedora
        UsuarioAutenticado criareFornecedora = cadastrar(TipoOrganizacao.FORNECEDOR,
                "Criare Consulting", "11.222.333/0001-81", "vendas@criare.com");
        assertNotEquals(empresa.organizacaoId(), criareFornecedora.organizacaoId());
    }

    @Test
    void cnpjInvalidoEhRecusado() {
        assertThrows(RegraDeNegocioException.class, () -> cadastrar(TipoOrganizacao.EMPRESA,
                "Empresa X", "11.222.333/0001-00", "x@empresa.com"));
    }

    @Test
    void cadaPessoaDaOrganizacaoFicaRegistradaNoQueFez() {
        UsuarioAutenticado colega = cadastroService.adicionarMembro(
                empresa.organizacaoId(), "Bruno Costa", "bruno@criare.com", "segredo123");
        assertEquals(Papel.MEMBRO, colega.papel());
        assertEquals(empresa.organizacaoId(), colega.organizacaoId());

        // O membro entra e age pela mesma organização
        var login = authService.login("bruno@criare.com", "segredo123").resposta();
        assertEquals(Papel.MEMBRO, login.usuario().papel());
        assertEquals(empresa.organizacaoId(), login.usuario().organizacao().id());

        Cotacao cotacao = cotacaoService.criarCotacao(novaCotacao(5), colega);
        assertEquals(empresa.organizacaoId(), cotacao.getEmpresa().getId());
        assertEquals(colega.usuarioId(), cotacao.getCriadaPor().getId());

        Proposta proposta = enviarProposta(fornecedorA, cotacao, "1000.00");
        assertEquals(fornecedorA.usuarioId(), proposta.getEnviadaPor().getId());

        // A negociação abre com a proposta, escrita por quem a enviou; depois cada um assina o seu
        Negociacao negociacao = negociacaoService.criarNegociacao(proposta.getId(), empresa);
        mensagemService.enviarMensagem(negociacao.getId(), "Fecha em 900?", new BigDecimal("900.00"), empresa);
        mensagemService.enviarMensagem(negociacao.getId(), "Eu assumo daqui.", null, colega);

        List<MensagemNegociacao> historico = mensagemRepositorio.listarDaNegociacao(negociacao.getId());
        assertEquals(List.of(fornecedorA.usuarioId(), empresa.usuarioId(), colega.usuarioId()),
                historico.stream().map(m -> m.getRemetente().getId()).toList());
        assertEquals(List.of(TipoRemetente.FORNECEDOR, TipoRemetente.EMPRESA, TipoRemetente.EMPRESA),
                historico.stream().map(MensagemNegociacao::getTipoRemetente).toList());

        // Mensagem de um colega não conta como "não lida" para o outro: é da mesma organização
        assertEquals(0, negociacaoService.contarNaoLidas(negociacao, empresa));
    }

    @Test
    void fluxoCompletoAteFecharONegocio() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "10000.00");
        Proposta propostaB = enviarProposta(fornecedorB, cotacao, "9500.00");

        // Mesmo fornecedor não envia duas propostas para a mesma cotação
        assertThrows(RegraDeNegocioException.class, () -> enviarProposta(fornecedorA, cotacao, "9000.00"));

        // Empresa escolhe negociar com o fornecedor A
        Negociacao negociacao = negociacaoService.criarNegociacao(propostaA.getId(), empresa);
        assertEquals(StatusProposta.ACEITA, propostaA.getStatus());
        assertEquals(StatusCotacao.EM_NEGOCIACAO, cotacao.getStatus());
        assertEquals(new BigDecimal("10000.00"), negociacao.getUltimaOferta());

        // A proposta original abre o histórico
        List<MensagemNegociacao> historico = mensagemRepositorio.listarDaNegociacao(negociacao.getId());
        assertEquals(1, historico.size());
        assertEquals(TipoRemetente.FORNECEDOR, historico.get(0).getTipoRemetente());

        // Não dá para abrir uma segunda negociação na mesma cotação
        assertThrows(RegraDeNegocioException.class, () -> negociacaoService.criarNegociacao(propostaB.getId(), empresa));

        // Contraproposta da empresa e resposta do fornecedor
        mensagemService.enviarMensagem(negociacao.getId(), "Consegue fazer por 9.000?",
                new BigDecimal("9000.00"), empresa);
        mensagemService.enviarMensagem(negociacao.getId(), null,
                new BigDecimal("9200.00"), fornecedorA);
        assertEquals(new BigDecimal("9200.00"), negociacao.getUltimaOferta());

        // Para quem não participa, a negociação não existe
        assertThrows(RecursoNaoEncontradoException.class, () -> mensagemService.enviarMensagem(negociacao.getId(), "oi",
                null, fornecedorB));

        // Fechamento
        negociacaoService.finalizarNegociacao(negociacao.getId(), new BigDecimal("9200.00"), empresa);
        assertEquals(StatusNegociacao.FINALIZADA, negociacao.getStatus());
        assertEquals(StatusCotacao.FECHADA, cotacao.getStatus());
        assertEquals(StatusProposta.RECUSADA, propostaB.getStatus());

        // Negociação encerrada não recebe mais mensagens
        assertThrows(RegraDeNegocioException.class, () -> mensagemService.enviarMensagem(negociacao.getId(), "oi",
                null, empresa));

        PainelFornecedorResponse painelA = dashboardService.resumoFornecedor(fornecedorA.organizacaoId());
        assertEquals(1, painelA.cotacoesGanhas());
        assertEquals(0, new BigDecimal("9200.00").compareTo(painelA.valorTotalGanho()));
    }

    @Test
    void cancelarNegociacaoReabreACotacao() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "10000.00");
        Proposta propostaB = enviarProposta(fornecedorB, cotacao, "9800.00");

        Negociacao negociacao = negociacaoService.criarNegociacao(propostaA.getId(), empresa);
        negociacaoService.cancelarNegociacao(negociacao.getId(), empresa);

        assertEquals(StatusNegociacao.CANCELADA, negociacao.getStatus());
        assertEquals(StatusProposta.RECUSADA, propostaA.getStatus());
        assertEquals(StatusCotacao.ABERTA, cotacao.getStatus());

        // Agora a empresa pode negociar com o outro fornecedor
        Negociacao segunda = negociacaoService.criarNegociacao(propostaB.getId(), empresa);
        assertNotEquals(negociacao.getId(), segunda.getId());
    }

    @Test
    void contaAsMensagensNaoLidasDeCadaLado() {
        Cotacao cotacao = criarCotacao(5);
        Proposta proposta = enviarProposta(fornecedorA, cotacao, "10000.00");
        Negociacao negociacao = negociacaoService.criarNegociacao(proposta.getId(), empresa);
        UUID id = negociacao.getId();

        // A proposta que abriu a negociação já foi lida pela empresa, e é do próprio fornecedor
        assertEquals(0, negociacaoService.contarNaoLidas(negociacao, empresa));
        assertEquals(0, negociacaoService.contarNaoLidas(negociacao, fornecedorA));

        mensagemService.enviarMensagem(id, "Consegue fazer por 9 mil?", null, empresa);
        mensagemService.enviarMensagem(id, null, new BigDecimal("9000.00"), empresa);
        assertEquals(2, negociacaoService.contarNaoLidas(negociacao, fornecedorA));
        assertEquals(Map.of(id, 2), negociacaoService.contarNaoLidas(fornecedorA));
        // As próprias mensagens nunca contam
        assertEquals(0, negociacaoService.contarNaoLidas(negociacao, empresa));
        assertTrue(negociacaoService.contarNaoLidas(empresa).isEmpty());

        // Responder conta como ter lido o que veio antes
        mensagemService.enviarMensagem(id, "Fecho em 9.500", new BigDecimal("9500.00"), fornecedorA);
        assertEquals(0, negociacaoService.contarNaoLidas(negociacao, fornecedorA));
        assertEquals(1, negociacaoService.contarNaoLidas(negociacao, empresa));

        negociacaoService.marcarComoLida(id, empresa);
        assertEquals(0, negociacaoService.contarNaoLidas(negociacao, empresa));
        assertTrue(negociacaoService.contarNaoLidas(empresa).isEmpty());

        // Quem não participa não marca nada
        assertThrows(RecursoNaoEncontradoException.class, () -> negociacaoService.marcarComoLida(id, fornecedorB));
    }

    @Test
    void cotacaoComPrazoVencidoSaiDoMuralENaoRecebePropostas() {
        Cotacao cotacao = criarCotacao(2);
        assertTrue(cotacaoService.listarCotacoesAbertas().stream().anyMatch(c -> c.getId().equals(cotacao.getId())));

        cotacao.setDataLimite(LocalDateTime.now().minusMinutes(1));

        assertTrue(cotacaoService.listarCotacoesAbertas().stream().noneMatch(c -> c.getId().equals(cotacao.getId())));
        assertThrows(RegraDeNegocioException.class, () -> enviarProposta(fornecedorA, cotacao, "500.00"));
    }

    @Test
    void naoCriaCotacaoComDataLimiteNoPassado() {
        Cotacao cotacao = novaCotacao(-1);
        assertThrows(RegraDeNegocioException.class, () -> cotacaoService.criarCotacao(cotacao, empresa));
    }

    @Test
    void cancelarCotacaoRecusaPropostasPendentes() {
        Cotacao cotacao = criarCotacao(5);
        Proposta proposta = enviarProposta(fornecedorA, cotacao, "700.00");

        cotacaoService.cancelarCotacao(cotacao.getId(), empresa);

        assertEquals(StatusCotacao.CANCELADA, cotacao.getStatus());
        assertEquals(StatusProposta.RECUSADA, proposta.getStatus());
    }

    @Test
    void dashboardDaEmpresaConsolidaOsNumeros() {
        Cotacao c1 = criarCotacao(5);
        criarCotacao(5);
        enviarProposta(fornecedorA, c1, "100.00");
        enviarProposta(fornecedorB, c1, "90.00");

        PainelEmpresaResponse painel = dashboardService.resumoEmpresa(empresa.organizacaoId());

        assertEquals(2, painel.cotacoesAbertas());
        assertEquals(2, painel.propostasRecebidas());
        assertEquals(1, painel.categorias().size());
        assertEquals(CategoriaCotacao.TECNOLOGIA.name(), painel.categorias().get(0).categoria());
        assertEquals(100, painel.categorias().get(0).percentual());
        assertEquals(2, painel.topFornecedores().size());
    }

    @Test
    void outraEmpresaNaoVeNemAlteraACotacao() {
        UsuarioAutenticado outra = cadastrar(TipoOrganizacao.EMPRESA,
                "Concorrente SA", "90.817.263/0001-80", "compras@concorrente.com");
        Cotacao cotacao = criarCotacao(5);
        enviarProposta(fornecedorA, cotacao, "500.00");

        assertThrows(RecursoNaoEncontradoException.class, () -> cotacaoService.buscarParaUsuario(cotacao.getId(), outra));
        assertThrows(RecursoNaoEncontradoException.class, () -> cotacaoService.cancelarCotacao(cotacao.getId(), outra));
        assertThrows(RecursoNaoEncontradoException.class, () -> propostaService.listarPorCotacao(cotacao.getId(), outra));

        // Fornecedores enxergam as cotações abertas (é o mural)
        assertEquals(cotacao.getId(), cotacaoService.buscarParaUsuario(cotacao.getId(), fornecedorB).getId());
    }

    @Test
    void cotacaoQueSaiDoMuralSoContinuaVisivelParaQuemPropos() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "500.00");
        negociacaoService.criarNegociacao(propostaA.getId(), empresa);

        assertEquals(cotacao.getId(), cotacaoService.buscarParaUsuario(cotacao.getId(), fornecedorA).getId());
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cotacaoService.buscarParaUsuario(cotacao.getId(), fornecedorB));
        // E quem não vê a cotação também não consegue enviar proposta para ela
        assertThrows(RecursoNaoEncontradoException.class, () -> enviarProposta(fornecedorB, cotacao, "450.00"));
    }

    @Test
    void outraOrganizacaoRecebeOMesmoErroDeUmIdQueNaoExiste() {
        Cotacao cotacao = criarCotacao(5);
        UsuarioAutenticado outra = cadastrar(TipoOrganizacao.EMPRESA,
                "Concorrente SA", "90.817.263/0001-80", "compras@concorrente.com");

        RecursoNaoEncontradoException deOutra = assertThrows(RecursoNaoEncontradoException.class,
                () -> cotacaoService.buscarParaUsuario(cotacao.getId(), outra));
        RecursoNaoEncontradoException inexistente = assertThrows(RecursoNaoEncontradoException.class,
                () -> cotacaoService.buscarParaUsuario(UUID.randomUUID(), outra));
        assertEquals(inexistente.getMessage(), deOutra.getMessage());
    }

    @Test
    void qualquerPessoaDaOrganizacaoOperaOsDadosDela() {
        UsuarioAutenticado colega = cadastroService.adicionarMembro(
                empresa.organizacaoId(), "Bruno Costa", "bruno@criare.com", "segredo123");
        Cotacao cotacao = criarCotacao(5);
        Proposta proposta = enviarProposta(fornecedorA, cotacao, "500.00");

        // A cotação foi publicada pela proprietária; o colega negocia e fecha
        assertEquals(cotacao.getId(), cotacaoService.buscarParaUsuario(cotacao.getId(), colega).getId());
        assertEquals(1, propostaService.listarPorCotacao(cotacao.getId(), colega).size());
        Negociacao negociacao = negociacaoService.criarNegociacao(proposta.getId(), colega);
        negociacaoService.finalizarNegociacao(negociacao.getId(), new BigDecimal("480.00"), colega);
        assertEquals(StatusNegociacao.FINALIZADA, negociacao.getStatus());
    }

    @Test
    void fornecedorNaoVePropostaDoConcorrente() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "500.00");

        assertThrows(RecursoNaoEncontradoException.class,
                () -> propostaService.buscarParaUsuario(propostaA.getId(), fornecedorB));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> propostaService.deletarProposta(propostaA.getId(), fornecedorB));
        assertEquals(propostaA.getId(),
                propostaService.buscarParaUsuario(propostaA.getId(), fornecedorA).getId());
    }

    @Test
    void fornecedorNaoFechaNegocioNemVeNegociacaoDosOutros() {
        Cotacao cotacao = criarCotacao(5);
        Proposta propostaA = enviarProposta(fornecedorA, cotacao, "500.00");
        Negociacao negociacao = negociacaoService.criarNegociacao(propostaA.getId(), empresa);

        // O fornecedor da negociação a enxerga, mas fechar é decisão da empresa: 403
        assertThrows(AcessoNegadoException.class,
                () -> negociacaoService.finalizarNegociacao(negociacao.getId(), BigDecimal.TEN, fornecedorA));
        // Para quem não participa, ela não existe: 404
        assertThrows(RecursoNaoEncontradoException.class,
                () -> negociacaoService.buscarParaParticipante(negociacao.getId(), fornecedorB));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> mensagemService.listarMensagens(negociacao.getId(), fornecedorB));
    }

    // ---------------------------------------------------------------- helpers

    private UsuarioAutenticado cadastrar(TipoOrganizacao tipo, String razaoSocial, String cnpj, String email) {
        return cadastroService.cadastrar(new NovaOrganizacao(
                tipo, razaoSocial, cnpj, "Pessoa da " + razaoSocial, email, "segredo123"));
    }

    private Cotacao criarCotacao(int diasDePrazo) {
        return cotacaoService.criarCotacao(novaCotacao(diasDePrazo), empresa);
    }

    private Cotacao novaCotacao(int diasDePrazo) {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico("Compra de notebooks");
        cotacao.setRequisitos("10 notebooks com 16 GB de RAM");
        cotacao.setCategoria(CategoriaCotacao.TECNOLOGIA);
        cotacao.setDataLimite(LocalDateTime.now().plusDays(diasDePrazo));
        return cotacao;
    }

    private Proposta enviarProposta(UsuarioAutenticado fornecedor, Cotacao cotacao, String valor) {
        Proposta proposta = new Proposta();
        proposta.setValor(new BigDecimal(valor));
        proposta.setDescricao("Entrega em 10 dias");
        return propostaService.criarProposta(proposta, fornecedor, cotacao.getId());
    }
}
