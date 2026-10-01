package com.gestao.services;

import com.gestao.dtos.dashboard.CategoriaResumoResponse;
import com.gestao.dtos.dashboard.DashboardEmpresaResponse;
import com.gestao.dtos.dashboard.DashboardFornecedorResponse;
import com.gestao.dtos.dashboard.FornecedorResumoResponse;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.enums.StatusProposta;
import com.gestao.repositories.CotacaoRepository;
import com.gestao.repositories.NegociacaoRepository;
import com.gestao.repositories.PropostaRepository;
import com.gestao.repositories.projections.CategoriaResumoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_FORNECEDORES = 5;

    private final CotacaoRepository cotacaoRepository;
    private final PropostaRepository propostaRepository;
    private final NegociacaoRepository negociacaoRepository;
    private final EmpresaService empresaService;
    private final FornecedorService fornecedorService;

    @Transactional(readOnly = true)
    public DashboardEmpresaResponse resumoEmpresa(UUID empresaId) {
        empresaService.buscarPorId(empresaId); // 404 se a empresa não existir

        long abertas = cotacaoRepository.countByEmpresaIdAndStatus(empresaId, StatusCotacao.ABERTA);
        long emNegociacao = cotacaoRepository.countByEmpresaIdAndStatus(empresaId, StatusCotacao.EM_NEGOCIACAO);
        long fechadas = cotacaoRepository.countByEmpresaIdAndStatus(empresaId, StatusCotacao.FECHADA);
        long propostas = propostaRepository.countByEmpresaId(empresaId);

        List<FornecedorResumoResponse> top = propostaRepository
                .topFornecedoresPorEmpresa(empresaId, PageRequest.of(0, TOP_FORNECEDORES))
                .stream()
                .map(f -> new FornecedorResumoResponse(f.getId(), f.getNome(), f.getEmail(), f.getTotalPropostas()))
                .toList();

        return new DashboardEmpresaResponse(abertas, emNegociacao, fechadas, propostas,
                resumirCategorias(cotacaoRepository.resumirPorCategoria(empresaId)), top);
    }

    @Transactional(readOnly = true)
    public DashboardFornecedorResponse resumoFornecedor(UUID fornecedorId) {
        fornecedorService.buscarPorId(fornecedorId);

        return new DashboardFornecedorResponse(
                cotacaoRepository.countAbertasVigentes(LocalDateTime.now()),
                propostaRepository.countByFornecedorIdAndStatusIn(fornecedorId,
                        List.of(StatusProposta.ENVIADA, StatusProposta.EM_ANALISE)),
                negociacaoRepository.countByFornecedorIdAndStatus(fornecedorId, StatusNegociacao.EM_ANDAMENTO),
                negociacaoRepository.countByFornecedorIdAndStatus(fornecedorId, StatusNegociacao.FINALIZADA),
                Objects.requireNonNullElse(
                        negociacaoRepository.somarValorFinalPorFornecedorEStatus(fornecedorId, StatusNegociacao.FINALIZADA),
                        BigDecimal.ZERO)
        );
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
