package com.gestao.demonstracao.aplicacao;

import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.MensagemNegociacaoService;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.demonstracao.aplicacao.porta.BaseDaDemonstracao;
import com.gestao.identidade.aplicacao.EmpresaService;
import com.gestao.identidade.aplicacao.FornecedorService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.Empresa;
import com.gestao.identidade.dominio.Fornecedor;
import com.gestao.identidade.dominio.TipoUsuario;
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
 * - na inicialização, popula o banco se ainda não houver nenhuma empresa
 * - todo dia (app.demo.reset-cron), apaga tudo e popula de novo, para a demo
 *   pública voltar ao estado inicial depois de ser usada por visitantes
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

    private final EmpresaService empresaService;
    private final FornecedorService fornecedorService;
    private final CotacaoService cotacaoService;
    private final PropostaService propostaService;
    private final NegociacaoService negociacaoService;
    private final MensagemNegociacaoService mensagemService;

    @Override
    @Transactional
    public void run(String... args) {
        if (base.temDados()) {
            log.info("Dados de demonstração ignorados: o banco já tem empresas cadastradas.");
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
        // Empresas
        Empresa criare = empresaService.cadastrarEmpresa(
                empresa("Criare Consulting", "11.222.333/0001-81", EMAIL_EMPRESA));
        Empresa hospital = empresaService.cadastrarEmpresa(
                empresa("Hospital Santa Vida", "27.384.915/0001-02", "hospital@demo.com"));

        // Fornecedores
        Fornecedor tech = fornecedorService.cadastrarFornecedor(
                fornecedor("Tech Soluções Ltda", "45.236.789/0001-12", EMAIL_FORNECEDOR));
        Fornecedor info = fornecedorService.cadastrarFornecedor(
                fornecedor("InfoWorld Distribuidora", "78.345.129/0001-29", "infoworld@demo.com"));
        Fornecedor limpa = fornecedorService.cadastrarFornecedor(
                fornecedor("Limpa Bem Serviços", "90.817.263/0001-80", "limpabem@demo.com"));
        Fornecedor clima = fornecedorService.cadastrarFornecedor(
                fornecedor("Clima Frio Ar-Condicionado", "56.102.938/0001-77", "climafrio@demo.com"));

        UsuarioAutenticado comoCriare = new UsuarioAutenticado(criare.getId(), TipoUsuario.EMPRESA);
        UsuarioAutenticado comoTech = new UsuarioAutenticado(tech.getId(), TipoUsuario.FORNECEDOR);

        // 1. Notebooks: duas propostas e uma negociação em andamento com a Tech
        Cotacao notebooks = cotacaoService.criarCotacao(cotacao(
                "Aquisição de 10 notebooks",
                "10 notebooks com 16 GB de RAM, SSD de 512 GB e garantia on-site de 3 anos para o time de TI.",
                CategoriaCotacao.TECNOLOGIA, new BigDecimal("60000.00"), 15), criare.getId());
        Proposta notebooksTech = propostaService.criarProposta(
                proposta("58000.00", "Entrega em 10 dias úteis, frete incluso."), tech.getId(), notebooks.getId());
        propostaService.criarProposta(
                proposta("61500.00", "Garantia estendida para 4 anos."), info.getId(), notebooks.getId());
        Negociacao negociacaoNotebooks = negociacaoService.criarNegociacao(notebooksTech.getId(), criare.getId());
        mensagemService.enviarMensagem(negociacaoNotebooks.getId(),
                "Gostamos da proposta. Conseguem chegar a R$ 55.000 mantendo o prazo?",
                new BigDecimal("55000.00"), comoCriare);

        // 2. Limpeza e cadeiras: abertas, aguardando a empresa
        Cotacao limpeza = cotacaoService.criarCotacao(cotacao(
                "Limpeza pós-obra do galpão B",
                "Limpeza completa de 800 m² após reforma, incluindo vidros e retirada de entulho leve.",
                CategoriaCotacao.LIMPEZA_MANUTENCAO, new BigDecimal("8000.00"), 10), criare.getId());
        propostaService.criarProposta(
                proposta("7200.00", "Equipe de 6 pessoas, conclusão em 3 dias."), limpa.getId(), limpeza.getId());

        cotacaoService.criarCotacao(cotacao(
                "Cadeiras ergonômicas",
                "25 cadeiras ergonômicas com regulagem de altura e apoio lombar.",
                CategoriaCotacao.MOBILIARIO, null, 20), criare.getId());

        // 3. Licenças: negócio já fechado com a Tech (aparece no histórico e nos dashboards)
        Cotacao licencas = cotacaoService.criarCotacao(cotacao(
                "Licenças de software de design",
                "15 licenças anuais de software de design gráfico, com suporte em português.",
                CategoriaCotacao.TECNOLOGIA, new BigDecimal("13000.00"), 7), criare.getId());
        Proposta licencasTech = propostaService.criarProposta(
                proposta("12000.00", "Licenças anuais com suporte e treinamento de 4 horas."), tech.getId(), licencas.getId());
        Negociacao negociacaoLicencas = negociacaoService.criarNegociacao(licencasTech.getId(), criare.getId());
        mensagemService.enviarMensagem(negociacaoLicencas.getId(), "Fechamos hoje por R$ 11.000?",
                new BigDecimal("11000.00"), comoCriare);
        mensagemService.enviarMensagem(negociacaoLicencas.getId(), "Conseguimos R$ 11.500 com o treinamento incluso.",
                new BigDecimal("11500.00"), comoTech);
        negociacaoService.finalizarNegociacao(negociacaoLicencas.getId(), new BigDecimal("11500.00"), criare.getId());

        // 4. Hospital: oportunidades de outra empresa no mural do fornecedor
        Cotacao arCondicionado = cotacaoService.criarCotacao(cotacao(
                "Manutenção de 15 aparelhos de ar-condicionado",
                "Manutenção preventiva trimestral em 15 aparelhos split, com relatório técnico.",
                CategoriaCotacao.SERVICOS, new BigDecimal("5000.00"), 12), hospital.getId());
        propostaService.criarProposta(
                proposta("4800.00", "Visita trimestral, peças à parte."), clima.getId(), arCondicionado.getId());

        cotacaoService.criarCotacao(cotacao(
                "Material de escritório para o trimestre",
                "Papel A4, toners, pastas e itens de papelaria para 3 setores administrativos.",
                CategoriaCotacao.SUPRIMENTOS, null, 9), hospital.getId());

        log.info("Dados de demonstração criados. Logins: {} / {} (senha: {})", EMAIL_EMPRESA, EMAIL_FORNECEDOR, SENHA_DEMO);
    }

    private Empresa empresa(String nome, String cnpj, String email) {
        Empresa e = new Empresa();
        e.setRazaoSocial(nome);
        e.setCnpj(cnpj);
        e.setEmail(email);
        e.setSenha(SENHA_DEMO);
        return e;
    }

    private Fornecedor fornecedor(String nome, String cnpj, String email) {
        Fornecedor f = new Fornecedor();
        f.setNomeCompleto(nome);
        f.setCnpj(cnpj);
        f.setEmail(email);
        f.setSenha(SENHA_DEMO);
        return f;
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
