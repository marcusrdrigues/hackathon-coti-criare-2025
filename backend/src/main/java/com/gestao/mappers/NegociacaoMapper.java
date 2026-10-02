package com.gestao.mappers;

import com.gestao.dtos.negociacao.NegociacaoResponse;
import com.gestao.entities.Cotacao;
import com.gestao.entities.Negociacao;
import com.gestao.entities.Proposta;
import org.springframework.stereotype.Component;

@Component
public class NegociacaoMapper {

    public NegociacaoResponse toResponse(Negociacao negociacao) {
        return toResponse(negociacao, null);
    }

    /** Com o total de mensagens não lidas de quem está pedindo. */
    public NegociacaoResponse toResponse(Negociacao negociacao, Integer naoLidas) {
        Proposta proposta = negociacao.getProposta();
        Cotacao cotacao = proposta.getCotacao();

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
                proposta.getId(),
                proposta.getValor(),
                negociacao.getUltimaOferta(),
                cotacao.getId(),
                cotacao.getNomeServico(),
                cotacao.getRequisitos(),
                cotacao.getStatus(),
                naoLidas
        );
    }
}
