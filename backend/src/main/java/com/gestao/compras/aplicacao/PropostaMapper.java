package com.gestao.compras.aplicacao;

import com.gestao.compras.aplicacao.dto.PropostaRequest;
import com.gestao.compras.aplicacao.dto.PropostaResponse;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
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
