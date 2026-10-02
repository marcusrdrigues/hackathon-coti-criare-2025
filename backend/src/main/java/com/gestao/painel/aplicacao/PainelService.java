package com.gestao.painel.aplicacao;

import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.identidade.aplicacao.OrganizacaoService;
import com.gestao.painel.aplicacao.dto.CategoriaResumoResponse;
import com.gestao.painel.aplicacao.dto.FornecedorResumoResponse;
import com.gestao.painel.aplicacao.dto.PainelEmpresaResponse;
import com.gestao.painel.aplicacao.dto.PainelFornecedorResponse;
import com.gestao.painel.aplicacao.porta.CategoriaResumoProjection;
import com.gestao.painel.aplicacao.porta.ConsultasDoPainel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Números dos painéis da empresa e do fornecedor. Só lê, por consultas próprias
 * (ver {@link ConsultasDoPainel}): é o lado de leitura, separado das regras de escrita.
 */
@Service
@RequiredArgsConstructor
public class PainelService {

    private static final int TOP_FORNECEDORES = 5;

    private final ConsultasDoPainel consultas;
    private final OrganizacaoService organizacaoService;

    @Transactional(readOnly = true)
    public PainelEmpresaResponse resumoEmpresa(UUID empresaId) {
        organizacaoService.buscarPorId(empresaId); // 404 se a organização não existir

        List<FornecedorResumoResponse> top = consultas.fornecedoresQueMaisPropuseram(empresaId, TOP_FORNECEDORES)
                .stream()
                .map(f -> new FornecedorResumoResponse(f.getId(), f.getNome(), f.getCnpj(), f.getTotalPropostas()))
                .toList();

        return new PainelEmpresaResponse(
                consultas.contarCotacoesDaEmpresa(empresaId, StatusCotacao.ABERTA),
                consultas.contarCotacoesDaEmpresa(empresaId, StatusCotacao.EM_NEGOCIACAO),
                consultas.contarCotacoesDaEmpresa(empresaId, StatusCotacao.FECHADA),
                consultas.contarPropostasRecebidas(empresaId),
                resumirCategorias(consultas.cotacoesPorCategoria(empresaId)),
                top);
    }

    @Transactional(readOnly = true)
    public PainelFornecedorResponse resumoFornecedor(UUID fornecedorId) {
        organizacaoService.buscarPorId(fornecedorId);

        return new PainelFornecedorResponse(
                consultas.contarCotacoesAbertasEmVigor(LocalDateTime.now()),
                consultas.contarPropostasDoFornecedor(fornecedorId,
                        List.of(StatusProposta.ENVIADA, StatusProposta.EM_ANALISE)),
                consultas.contarNegociacoesDoFornecedor(fornecedorId, StatusNegociacao.EM_ANDAMENTO),
                consultas.contarNegociacoesDoFornecedor(fornecedorId, StatusNegociacao.FINALIZADA),
                consultas.somarValorFinalDoFornecedor(fornecedorId, StatusNegociacao.FINALIZADA));
    }

    /**
     * Converte a contagem por categoria em percentuais. Cotações antigas sem
     * categoria entram em "Outros".
     */
    private List<CategoriaResumoResponse> resumirCategorias(List<CategoriaResumoProjection> linhas) {
        Map<CategoriaCotacao, Long> totais = new EnumMap<>(CategoriaCotacao.class);
        for (CategoriaResumoProjection linha : linhas) {
            CategoriaCotacao categoria = linha.getCategoria() != null ? linha.getCategoria() : CategoriaCotacao.OUTROS;
            totais.merge(categoria, linha.getTotal(), Long::sum);
        }

        long totalGeral = totais.values().stream().mapToLong(Long::longValue).sum();
        if (totalGeral == 0) {
            return List.of();
        }

        return totais.entrySet().stream()
                .sorted(Map.Entry.<CategoriaCotacao, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new CategoriaResumoResponse(
                        e.getKey().name(),
                        e.getKey().getDescricao(),
                        e.getValue(),
                        (int) Math.round(e.getValue() * 100.0 / totalGeral)))
                .toList();
    }
}
