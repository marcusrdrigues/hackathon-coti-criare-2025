package com.gestao.mappers;

import com.gestao.dtos.proposta.PropostaRequest;
import com.gestao.dtos.proposta.PropostaResponse;
import com.gestao.entities.Proposta;
import org.springframework.stereotype.Component;

@Component
public class PropostaMapper {

    public Proposta toEntity(PropostaRequest request) {
        Proposta proposta = new Proposta();
        proposta.setValor(request.valor());
        proposta.setDescricao(request.descricao());
        return proposta;
    }

    public PropostaResponse toResponse(Proposta proposta) {
        return new PropostaResponse(
                proposta.getId(),
                proposta.getValor(),
                proposta.getDescricao(),
                proposta.getStatus(),
                proposta.getFornecedor().getId(),
                proposta.getFornecedor().getNomeCompleto(),
                proposta.getCotacao().getId(),
                proposta.getCotacao().getNomeServico()
        );
    }
}