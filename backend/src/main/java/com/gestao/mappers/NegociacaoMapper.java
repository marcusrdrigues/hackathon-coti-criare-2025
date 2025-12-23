package com.gestao.mappers;

import com.gestao.dtos.negociacao.NegociacaoResponse;
import com.gestao.entities.Negociacao;
import org.springframework.stereotype.Component;

@Component
public class NegociacaoMapper {

    public NegociacaoResponse toResponse(Negociacao negociacao) {
        return new NegociacaoResponse(
                negociacao.getId(),
                negociacao.getValorFinal(),
                negociacao.getStatus(),
                negociacao.getDataInicio(),
                negociacao.getDataFinalizacao(),
                negociacao.getEmpresa().getId(),
                negociacao.getEmpresa().getRazaoSocial(),
                negociacao.getFornecedor().getId(),
                negociacao.getFornecedor().getNomeCompleto(),
                negociacao.getProposta().getId()
        );
    }
}