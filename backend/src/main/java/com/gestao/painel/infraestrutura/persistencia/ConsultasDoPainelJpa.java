package com.gestao.painel.infraestrutura.persistencia;

import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.painel.aplicacao.porta.CategoriaResumoProjection;
import com.gestao.painel.aplicacao.porta.ConsultasDoPainel;
import com.gestao.painel.aplicacao.porta.FornecedorResumoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Adaptador: implementa as consultas dos painéis com JPQL. */
@Repository
@RequiredArgsConstructor
class ConsultasDoPainelJpa implements ConsultasDoPainel {

    private final PainelJpa jpa;

    @Override
    public long contarCotacoesDaEmpresa(UUID empresaId, StatusCotacao status) {
        return jpa.contarCotacoesDaEmpresa(empresaId, status);
    }

    @Override
    public long contarPropostasRecebidas(UUID empresaId) {
        return jpa.contarPropostasRecebidas(empresaId);
    }

    @Override
    public List<FornecedorResumoProjection> fornecedoresQueMaisPropuseram(UUID empresaId, int limite) {
        return jpa.fornecedoresQueMaisPropuseram(empresaId, PageRequest.of(0, limite));
    }

    @Override
    public List<CategoriaResumoProjection> cotacoesPorCategoria(UUID empresaId) {
        return jpa.cotacoesPorCategoria(empresaId);
    }

    @Override
    public long contarCotacoesAbertasEmVigor(LocalDateTime agora) {
        return jpa.contarCotacoesEmVigor(StatusCotacao.ABERTA, agora);
    }

    @Override
    public long contarPropostasDoFornecedor(UUID fornecedorId, Collection<StatusProposta> status) {
        return jpa.contarPropostasDoFornecedor(fornecedorId, status);
    }

    @Override
    public long contarNegociacoesDoFornecedor(UUID fornecedorId, StatusNegociacao status) {
        return jpa.contarNegociacoesDoFornecedor(fornecedorId, status);
    }

    @Override
    public BigDecimal somarValorFinalDoFornecedor(UUID fornecedorId, StatusNegociacao status) {
        return Objects.requireNonNullElse(jpa.somarValorFinalDoFornecedor(fornecedorId, status), BigDecimal.ZERO);
    }
}
