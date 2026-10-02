package com.gestao.compras.aplicacao;

import com.gestao.compras.aplicacao.dto.CotacaoRequest;
import com.gestao.compras.aplicacao.dto.CotacaoResponse;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusProposta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
public class CotacaoMapper {

    public Cotacao toEntity(CotacaoRequest request) {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico(request.nomeServico());
        cotacao.setRequisitos(request.requisitos());
        cotacao.setCategoria(request.categoria());
        cotacao.setOrcamentoEstimado(request.orcamentoEstimado());
        cotacao.setDataLimite(request.dataLimite());
        return cotacao;
    }

    public CotacaoResponse toResponse(Cotacao cotacao) {
        List<Proposta> propostas = cotacao.getPropostas() != null ? cotacao.getPropostas() : List.of();

        // Melhor oferta = menor valor entre as propostas que ainda estão no jogo
        BigDecimal melhorOferta = propostas.stream()
                .filter(p -> p.getStatus() != StatusProposta.RECUSADA)
                .map(Proposta::getValor)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        return new CotacaoResponse(
                cotacao.getId(),
                cotacao.getNomeServico(),
                cotacao.getRequisitos(),
                cotacao.getCategoria(),
                cotacao.getCategoria() != null ? cotacao.getCategoria().getDescricao() : null,
                cotacao.getOrcamentoEstimado(),
                cotacao.getDataCriacao(),
                cotacao.getDataLimite(),
                cotacao.getStatus(),
                cotacao.getEmpresa().getId(),
                cotacao.getEmpresa().getRazaoSocial(),
                propostas.size(),
                melhorOferta
        );
    }
}
