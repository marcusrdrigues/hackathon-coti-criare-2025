package com.gestao.demonstracao.aplicacao;

import com.gestao.compartilhado.dominio.Mascaras;
import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.MensagemNegociacaoService;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.demonstracao.aplicacao.porta.BaseDaDemonstracao;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.TipoOrganizacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dados de exemplo para apresentações e para a demo pública.
 * Só existe com o profile "demo":
 * - na inicialização, popula o banco se ainda não houver nenhuma organização
 * - todo dia (app.demo.reset-cron), apaga tudo e popula de novo, para a demo
 *   pública voltar ao estado inicial depois de ser usada por visitantes
 *
 * <p>A Criare e a Tech têm duas pessoas cada: a proprietária, que é a conta de exemplo
 * da tela de login, e um membro da equipe que também participa das negociações.
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=demo</pre>
 */
@Slf4j
@Component
@Profile("demo")
@Order(2)
@RequiredArgsConstructor
public class DadosDemonstracao implements CommandLineRunner {

    public static final String SENHA_DEMO = "demo1234";
    public static final String EMAIL_EMPRESA = "empresa@demo.com";
    public static final String EMAIL_FORNECEDOR = "fornecedor@demo.com";

    private final BaseDaDemonstracao base;

    private final CadastroService cadastroService;
    private final CotacaoService cotacaoService;
    private final PropostaService propostaService;
    private final NegociacaoService negociacaoService;
    private final MensagemNegociacaoService mensagemService;

    @Override
    @Transactional
    public void run(String... args) {
        if (base.temDados()) {
            log.info("Dados de demonstração ignorados: o banco já tem organizações cadastradas.");
            return;
        }
        popular();
    }

    /** Volta a demo ao estado inicial. Também apaga as sessões, então todos precisam entrar de novo. */
    @Scheduled(cron = "${app.demo.reset-cron:0 0 4 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public void restaurar() {
        base.apagarTudo();
        popular();
        log.info("Dados de demonstração restaurados.");
    }

    private void popular() {
        // Empresas compradoras
        UsuarioAutenticado ana = empresa("Criare Consulting", "11.222.333/0001-81", "Ana Ribeiro", EMAIL_EMPRESA);
        UsuarioAutenticado bruno = membro(ana, "Bruno Costa", "bruno.compras@demo.com");
        UsuarioAutenticado helena = empresa("Hospital Santa Vida", "27.384.915/0001-02", "Helena Duarte",
                "hospital@demo.com");

        // Fornecedores
        UsuarioAutenticado carlos = fornecedor("Tech Soluções Ltda", "45.236.789/0001-12", "Carlos Mendes",
                EMAIL_FORNECEDOR);
        UsuarioAutenticado daniela = membro(carlos, "Daniela Rocha", "daniela.vendas@demo.com");
        UsuarioAutenticado info = fornecedor("InfoWorld Distribuidora", "78.345.129/0001-29", "Eduardo Lima",
                "infoworld@demo.com");
        UsuarioAutenticado limpa = fornecedor("Limpa Bem Serviços", "90.817.263/0001-80", "Fernanda Alves",
                "limpabem@demo.com");
        UsuarioAutenticado clima = fornecedor("Clima Frio Ar-Condicionado", "56.102.938/0001-77", "Gustavo Pires",
                "climafrio@demo.com");

        // 1. Notebooks: duas propostas e uma negociação em andamento com a Tech
        Cotacao notebooks = cotacaoService.criarCotacao(cotacao(
                "Aquisição de 10 notebooks",
                "10 notebooks com 16 GB de RAM, SSD de 512 GB e garantia on-site de 3 anos para o time de TI.",
                CategoriaCotacao.TECNOLOGIA, new BigDecimal("60000.00"), 15), ana);
        Proposta notebooksTech = propostaService.criarProposta(
                proposta("58000.00", "Entrega em 10 dias úteis, frete incluso."), carlos, notebooks.getId());
        propostaService.criarProposta(
                proposta("61500.00", "Garantia estendida para 4 anos."), info, notebooks.getId());
        Negociacao negociacaoNotebooks = negociacaoService.criarNegociacao(notebooksTech.getId(), ana.organizacaoId());
        mensagemService.enviarMensagem(negociacaoNotebooks.getId(),
                "Gostamos da proposta. Conseguem chegar a R$ 55.000 mantendo o prazo?",
                new BigDecimal("55000.00"), ana);
        mensagemService.enviarMensagem(negociacaoNotebooks.getId(),
                "Assumo a negociação daqui. Podemos fechar em R$ 56.500 com entrega em 8 dias úteis.",
                new BigDecimal("56500.00"), daniela);

        // 2. Limpeza e cadeiras: abertas, aguardando a empresa (a de cadeiras foi criada pelo Bruno)
        Cotacao limpeza = cotacaoService.criarCotacao(cotacao(
                "Limpeza pós-obra do galpão B",
                "Limpeza completa de 800 m² após reforma, incluindo vidros e retirada de entulho leve.",
                CategoriaCotacao.LIMPEZA_MANUTENCAO, new BigDecimal("8000.00"), 10), ana);
        propostaService.criarProposta(
                proposta("7200.00", "Equipe de 6 pessoas, conclusão em 3 dias."), limpa, limpeza.getId());

        cotacaoService.criarCotacao(cotacao(
                "Cadeiras ergonômicas",
                "25 cadeiras ergonômicas com regulagem de altura e apoio lombar.",
                CategoriaCotacao.MOBILIARIO, null, 20), bruno);

        // 3. Licenças: negócio já fechado com a Tech (aparece no histórico e nos dashboards)
        Cotacao licencas = cotacaoService.criarCotacao(cotacao(
                "Licenças de software de design",
                "15 licenças anuais de software de design gráfico, com suporte em português.",
                CategoriaCotacao.TECNOLOGIA, new BigDecimal("13000.00"), 7), bruno);
        Proposta licencasTech = propostaService.criarProposta(
                proposta("12000.00", "Licenças anuais com suporte e treinamento de 4 horas."), carlos, licencas.getId());
        Negociacao negociacaoLicencas = negociacaoService.criarNegociacao(licencasTech.getId(), bruno.organizacaoId());
        mensagemService.enviarMensagem(negociacaoLicencas.getId(), "Fechamos hoje por R$ 11.000?",
                new BigDecimal("11000.00"), bruno);
        mensagemService.enviarMensagem(negociacaoLicencas.getId(), "Conseguimos R$ 11.500 com o treinamento incluso.",
                new BigDecimal("11500.00"), carlos);
        negociacaoService.finalizarNegociacao(negociacaoLicencas.getId(), new BigDecimal("11500.00"),
                ana.organizacaoId());

        // 4. Hospital: oportunidades de outra empresa no mural do fornecedor
        Cotacao arCondicionado = cotacaoService.criarCotacao(cotacao(
                "Manutenção de 15 aparelhos de ar-condicionado",
                "Manutenção preventiva trimestral em 15 aparelhos split, com relatório técnico.",
                CategoriaCotacao.SERVICOS, new BigDecimal("5000.00"), 12), helena);
        propostaService.criarProposta(
                proposta("4800.00", "Visita trimestral, peças à parte."), clima, arCondicionado.getId());

        cotacaoService.criarCotacao(cotacao(
                "Material de escritório para o trimestre",
                "Papel A4, toners, pastas e itens de papelaria para 3 setores administrativos.",
                CategoriaCotacao.SUPRIMENTOS, null, 9), helena);

        log.info("Dados de demonstração criados. Contas de exemplo: {} e {}.",
                Mascaras.email(EMAIL_EMPRESA), Mascaras.email(EMAIL_FORNECEDOR));
    }

    private UsuarioAutenticado empresa(String razaoSocial, String cnpj, String pessoa, String email) {
        return cadastroService.cadastrar(
                new NovaOrganizacao(TipoOrganizacao.EMPRESA, razaoSocial, cnpj, pessoa, email, SENHA_DEMO));
    }

    private UsuarioAutenticado fornecedor(String razaoSocial, String cnpj, String pessoa, String email) {
        return cadastroService.cadastrar(
                new NovaOrganizacao(TipoOrganizacao.FORNECEDOR, razaoSocial, cnpj, pessoa, email, SENHA_DEMO));
    }

    private UsuarioAutenticado membro(UsuarioAutenticado colega, String pessoa, String email) {
        return cadastroService.adicionarMembro(colega.organizacaoId(), pessoa, email, SENHA_DEMO);
    }

    private Cotacao cotacao(String nome, String requisitos, CategoriaCotacao categoria,
                            BigDecimal orcamento, int diasDePrazo) {
        Cotacao c = new Cotacao();
        c.setNomeServico(nome);
        c.setRequisitos(requisitos);
        c.setCategoria(categoria);
        c.setOrcamentoEstimado(orcamento);
        c.setDataLimite(LocalDateTime.now().plusDays(diasDePrazo));
        return c;
    }

    private Proposta proposta(String valor, String descricao) {
        Proposta p = new Proposta();
        p.setValor(new BigDecimal(valor));
        p.setDescricao(descricao);
        return p;
    }
}
