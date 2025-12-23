package com.gestao.mappers;

import com.gestao.dtos.cotacao.CotacaoRequest;
import com.gestao.dtos.cotacao.CotacaoResponse;
import com.gestao.entities.Cotacao;
import org.springframework.stereotype.Component;

@Component
public class CotacaoMapper {

    public Cotacao toEntity(CotacaoRequest request) {
        Cotacao cotacao = new Cotacao();
        cotacao.setNomeServico(request.nomeServico());
        cotacao.setRequisitos(request.requisitos());
        cotacao.setDataLimite(request.dataLimite());
        return cotacao;
    }

    public CotacaoResponse toResponse(Cotacao cotacao) {
        return new CotacaoResponse(
                cotacao.getId(),
                cotacao.getNomeServico(),
                cotacao.getRequisitos(),
                cotacao.getDataCriacao(),
                cotacao.getDataLimite(),
                cotacao.getStatus(),
                cotacao.getEmpresa().getId(),
                cotacao.getEmpresa().getRazaoSocial(),
                cotacao.getPropostas() != null ? cotacao.getPropostas().size() : 0
        );
    }
}