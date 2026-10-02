package com.gestao.compras.aplicacao;

import com.gestao.compras.aplicacao.dto.MensagemResponse;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.TipoRemetente;
import org.springframework.stereotype.Component;

@Component
public class MensagemMapper {

    public MensagemResponse toResponse(MensagemNegociacao mensagem) {
        // O lado de quem escreveu: a organização, e a pessoa dela
        String remetenteNome = mensagem.getTipoRemetente() == TipoRemetente.EMPRESA
                ? mensagem.getNegociacao().getEmpresa().getRazaoSocial()
                : mensagem.getNegociacao().getFornecedor().getRazaoSocial();

        return new MensagemResponse(
                mensagem.getId(),
                mensagem.getMensagem(),
                mensagem.getValorOfertado(),
                mensagem.getTipoRemetente(),
                mensagem.getRemetente().getId(),
                remetenteNome,
                mensagem.getRemetente().getNome(),
                mensagem.getDataEnvio(),
                mensagem.getNegociacao().getId()
        );
    }
}
