package com.gestao.mappers;

import com.gestao.dtos.proposta.PropostaRequest;
import com.gestao.dtos.proposta.PropostaResponse;
import com.gestao.entities.Cotacao;
import com.gestao.entities.Negociacao;
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
        Cotacao cotacao = proposta.getCotacao();
        Negociacao negociacao = proposta.getNegociacao();

        return new PropostaResponse(
                proposta.getId(),
                proposta.getValor(),
                proposta.getDescricao(),
                proposta.getStatus(),
                proposta.getDataEnvio(),
                proposta.getFornecedor().getId(),
                proposta.getFornecedor().getNomeCompleto(),
                proposta.getFornecedor().getCnpj(),
                cotacao.getId(),
                cotacao.getNomeServico(),
                cotacao.getStatus(),
                cotacao.getEmpresa().getId(),
                cotacao.getEmpresa().getRazaoSocial(),
                negociacao != null ? negociacao.getId() : null,
                negociacao != null ? negociacao.getStatus() : null,
                negociacao != null ? negociacao.getValorFinal() : null
        );
    }
}
