package com.gestao.configurations;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Empresa;
import com.gestao.entities.Fornecedor;
import com.gestao.entities.Proposta;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.repositories.EmpresaRepository;
import com.gestao.services.CotacaoService;
import com.gestao.services.EmpresaService;
import com.gestao.services.FornecedorService;
import com.gestao.services.NegociacaoService;
import com.gestao.services.PropostaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Popula o banco com dados de exemplo para apresentações.
 * Só roda com o profile "demo" e só quando ainda não existe nenhuma empresa.
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=demo</pre>
 */
@Slf4j
@Component
@Profile("demo")
@Order(2)
@RequiredArgsConstructor
public class DadosDemonstracao implements CommandLineRunner {

    public static final String SENHA_DEMO = "demo123";

    private final EmpresaRepository empresaRepository;
    private final EmpresaService empresaService;
    private final FornecedorService fornecedorService;
    private final CotacaoService cotacaoService;
    private final PropostaService propostaService;
    private final NegociacaoService negociacaoService;

    @Override
    public void run(String... args) {
        if (empresaRepository.count() > 0) {
            log.info("Dados de demonstração ignorados: o banco já tem empresas cadastradas.");
            return;
        }

        Empresa criare = empresaService.cadastrarEmpresa(
                empresa("Criare Consulting", "11.222.333/0001-81", "empresa@demo.com"));

        Fornecedor tech = fornecedorService.cadastrarFornecedor(
                fornecedor("Tech Soluções Ltda", "45.236.789/0001-12", "fornecedor@demo.com"));
        Fornecedor info = fornecedorService.cadastrarFornecedor(
                fornecedor("InfoWorld Distribuidora", "78.345.129/0001-29", "infoworld@demo.com"));
        Fornecedor limpa = fornecedorService.cadastrarFornecedor(
                fornecedor("Limpa Bem Serviços", "90.817.263/0001-80", "limpabem@demo.com"));

        Cotacao notebooks = cotacaoService.criarCotacao(cotacao(
                "Aquisição de 10 notebooks",
                "10 notebooks com 16 GB de RAM, SSD de 512 GB e garantia on-site de 3 anos para o time de TI.",
                CategoriaCotacao.TECNOLOGIA, new BigDecimal("60000.00"), 15), criare.getId());

        Cotacao limpeza = cotacaoService.criarCotacao(cotacao(
                "Limpeza pós-obra do galpão B",
                "Limpeza completa de 800 m² após reforma, incluindo vidros e retirada de entulho leve.",
                CategoriaCotacao.LIMPEZA_MANUTENCAO, new BigDecimal("8000.00"), 10), criare.getId());

        cotacaoService.criarCotacao(cotacao(
                "Cadeiras ergonômicas",
                "25 cadeiras ergonômicas com regulagem de altura e apoio lombar.",
                CategoriaCotacao.MOBILIARIO, null, 20), criare.getId());

        Proposta propostaTech = propostaService.criarProposta(
                proposta("58000.00", "Entrega em 10 dias úteis, frete incluso."), tech.getId(), notebooks.getId());
        propostaService.criarProposta(
                proposta("61500.00", "Garantia estendida para 4 anos."), info.getId(), notebooks.getId());
        propostaService.criarProposta(
                proposta("7200.00", "Equipe de 6 pessoas, conclusão em 3 dias."), limpa.getId(), limpeza.getId());

        negociacaoService.criarNegociacao(propostaTech.getId());

        log.info("Dados de demonstração criados. Logins: empresa@demo.com / fornecedor@demo.com (senha: {})", SENHA_DEMO);
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
