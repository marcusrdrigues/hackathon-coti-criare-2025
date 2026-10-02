package com.gestao.temporeal.aplicacao;

import com.gestao.compras.aplicacao.MensagemMapper;
import com.gestao.compras.aplicacao.MensagemNegociacaoService;
import com.gestao.compras.aplicacao.NegociacaoMapper;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.dominio.MensagemEnviadaEvento;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.NegociacaoAlteradaEvento;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.PropostaRecebidaEvento;
import com.gestao.compras.dominio.TipoRemetente;
import com.gestao.temporeal.aplicacao.porta.CanalTempoReal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Leva os eventos de domínio para quem está conectado.
 *
 * Só roda depois do commit ({@link TransactionalEventListener}): se a operação
 * falhar e for desfeita, ninguém recebe aviso de algo que não aconteceu. Lê os
 * dados numa transação própria, já gravados. Uma falha aqui nunca derruba a
 * requisição que gerou o evento: o dado já está salvo e a tela se atualiza na
 * próxima leitura.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificadorTempoReal {

    private static final int RESUMO_MAXIMO = 80;

    private final CanalTempoReal canal;
    private final MensagemNegociacaoService mensagemService;
    private final NegociacaoService negociacaoService;
    private final PropostaService propostaService;
    private final MensagemMapper mensagemMapper;
    private final NegociacaoMapper negociacaoMapper;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoEnviarMensagem(MensagemEnviadaEvento evento) {
        try {
            publicarMensagem(mensagemService.buscarPorId(evento.mensagemId()));
        } catch (RuntimeException e) {
            log.warn("Não foi possível publicar a mensagem {} em tempo real: {}", evento.mensagemId(), e.getMessage());
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoAlterarNegociacao(NegociacaoAlteradaEvento evento) {
        try {
            publicarAlteracao(negociacaoService.buscarPorId(evento.negociacaoId()), evento.tipo());
        } catch (RuntimeException e) {
            log.warn("Não foi possível publicar a negociação {} em tempo real: {}", evento.negociacaoId(), e.getMessage());
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoReceberProposta(PropostaRecebidaEvento evento) {
        try {
            avisarEmpresa(propostaService.buscarPorId(evento.propostaId()));
        } catch (RuntimeException e) {
            log.warn("Não foi possível avisar sobre a proposta {}: {}", evento.propostaId(), e.getMessage());
        }
    }

    private void publicarMensagem(MensagemNegociacao mensagem) {
        Negociacao negociacao = mensagem.getNegociacao();
        canal.publicarNaNegociacao(negociacao.getId(),
                EventoNegociacao.mensagem(mensagemMapper.toResponse(mensagem), negociacaoMapper.toResponse(negociacao)));

        boolean daEmpresa = mensagem.getTipoRemetente() == TipoRemetente.EMPRESA;
        UUID destinatario = daEmpresa ? negociacao.getFornecedor().getId() : negociacao.getEmpresa().getId();
        String quem = daEmpresa ? negociacao.getEmpresa().getRazaoSocial() : negociacao.getFornecedor().getRazaoSocial();
        String texto = mensagem.getValorOfertado() != null
                ? "Nova oferta de " + moeda(mensagem.getValorOfertado())
                : resumo(mensagem.getMensagem());
        avisar(destinatario, new AvisoTempoReal(AvisoTempoReal.Tipo.MENSAGEM, negociacao.getId(),
                cotacaoDe(negociacao), quem, texto));
    }

    private void publicarAlteracao(Negociacao negociacao, NegociacaoAlteradaEvento.Tipo tipo) {
        if (tipo != NegociacaoAlteradaEvento.Tipo.INICIADA) {
            canal.publicarNaNegociacao(negociacao.getId(),
                    EventoNegociacao.status(negociacaoMapper.toResponse(negociacao)));
        }

        // Quem decide é a empresa; quem precisa saber é o fornecedor
        String empresa = negociacao.getEmpresa().getRazaoSocial();
        String cotacao = negociacao.getProposta().getCotacao().getNomeServico();
        AvisoTempoReal aviso = switch (tipo) {
            case INICIADA -> new AvisoTempoReal(AvisoTempoReal.Tipo.NEGOCIACAO_INICIADA, negociacao.getId(),
                    cotacaoDe(negociacao), empresa, "Quer negociar a sua proposta para " + cotacao);
            case FINALIZADA -> new AvisoTempoReal(AvisoTempoReal.Tipo.NEGOCIACAO_FINALIZADA, negociacao.getId(),
                    cotacaoDe(negociacao), empresa, "Negócio fechado por " + moeda(negociacao.getValorFinal()));
            case CANCELADA -> new AvisoTempoReal(AvisoTempoReal.Tipo.NEGOCIACAO_CANCELADA, negociacao.getId(),
                    cotacaoDe(negociacao), empresa, "Encerrou a negociação de " + cotacao + " sem acordo");
        };
        avisar(negociacao.getFornecedor().getId(), aviso);
    }

    private void avisarEmpresa(Proposta proposta) {
        avisar(proposta.getCotacao().getEmpresa().getId(), new AvisoTempoReal(AvisoTempoReal.Tipo.PROPOSTA_RECEBIDA,
                null, proposta.getCotacao().getId(), proposta.getFornecedor().getRazaoSocial(),
                "Nova proposta de " + moeda(proposta.getValor()) + " para " + proposta.getCotacao().getNomeServico()));
    }

    private void avisar(UUID usuarioId, AvisoTempoReal aviso) {
        canal.avisar(usuarioId, aviso);
    }

    private static UUID cotacaoDe(Negociacao negociacao) {
        return negociacao.getProposta().getCotacao().getId();
    }

    private static String moeda(BigDecimal valor) {
        return valor == null ? "" : NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor);
    }

    private static String resumo(String texto) {
        if (texto == null) {
            return "";
        }
        String limpo = texto.strip().replaceAll("\\s+", " ");
        return limpo.length() <= RESUMO_MAXIMO ? limpo : limpo.substring(0, RESUMO_MAXIMO - 1) + "…";
    }
}
