package com.gestao.compras.aplicacao;

import com.gestao.compras.aplicacao.dto.CotacaoRequest;
import com.gestao.compras.aplicacao.dto.CotacaoResponse;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
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

    /** A visão da empresa dona: com a melhor oferta. */
    public CotacaoResponse toResponse(Cotacao cotacao) {
        List<Proposta> propostas = propostas(cotacao);

        // Melhor oferta = menor valor entre as propostas que ainda estão no jogo
        BigDecimal melhorOferta = propostas.stream()
                .filter(p -> p.getStatus() != StatusProposta.RECUSADA)
                .map(Proposta::getValor)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        return resposta(cotacao, propostas.size(), melhorOferta, null);
    }

    /**
     * A visão de quem pede: só a empresa dona vê a melhor oferta. Os outros não veem os lances,
     * e o fornecedor vê a proposta que ele mesmo enviou.
     */
    public CotacaoResponse paraQuemPede(Cotacao cotacao, UsuarioAutenticado usuario) {
        if (usuario.ehEmpresa() && cotacao.getEmpresa().getId().equals(usuario.organizacaoId())) {
            return toResponse(cotacao);
        }
        List<Proposta> propostas = propostas(cotacao);
        CotacaoResponse.MinhaProposta minha = propostas.stream()
                .filter(p -> usuario.ehFornecedor() && p.getFornecedor() != null
                        && usuario.organizacaoId().equals(p.getFornecedor().getId()))
                .findFirst()
                .map(p -> new CotacaoResponse.MinhaProposta(p.getId(), p.getValor(), p.getStatus()))
                .orElse(null);
        return resposta(cotacao, propostas.size(), null, minha);
    }

    private static List<Proposta> propostas(Cotacao cotacao) {
        return cotacao.getPropostas() != null ? cotacao.getPropostas() : List.of();
    }

    private static CotacaoResponse resposta(Cotacao cotacao, long quantidade, BigDecimal melhorOferta,
                                            CotacaoResponse.MinhaProposta minha) {
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
                quantidade,
                melhorOferta,
                minha
        );
    }
}
