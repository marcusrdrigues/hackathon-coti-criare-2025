package com.gestao.compras.aplicacao;

import com.gestao.compras.aplicacao.dto.MensagemResponse;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.TipoRemetente;
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
                mensagem.getValorOfertado(),
                mensagem.getTipoRemetente(),
                mensagem.getRemetenteId(),
                remetenteNome,
                mensagem.getDataEnvio(),
                mensagem.getNegociacao().getId()
        );
    }
}
