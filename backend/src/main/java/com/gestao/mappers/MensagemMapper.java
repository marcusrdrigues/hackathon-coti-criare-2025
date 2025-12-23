package com.gestao.mappers;

import com.gestao.dtos.mensagem.MensagemResponse;
import com.gestao.entities.MensagemNegociacao;
import com.gestao.enums.TipoRemetente;  // ← MUDOU
import org.springframework.stereotype.Component;

@Component
public class MensagemMapper {

    public MensagemResponse toResponse(MensagemNegociacao mensagem) {
        String remetenteNome = mensagem.getTipoRemetente() == TipoRemetente.EMPRESA
                ? mensagem.getNegociacao().getEmpresa().getRazaoSocial()
                : mensagem.getNegociacao().getFornecedor().getNomeCompleto();

        return new MensagemResponse(
                mensagem.getId(),
                mensagem.getMensagem(),
                mensagem.getTipoRemetente(),
                mensagem.getRemetenteId(),
                remetenteNome,
                mensagem.getDataEnvio(),
                mensagem.getNegociacao().getId()
        );
    }
}