package com.gestao.services;

import com.gestao.entities.MensagemNegociacao;
import com.gestao.entities.Negociacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.MensagemNegociacaoRepository;
import com.gestao.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MensagemNegociacaoService {

    private final MensagemNegociacaoRepository mensagemRepository;
    private final NegociacaoService negociacaoService;

    /**
     * Envia uma mensagem e/ou uma nova oferta de valor dentro da negociação.
     * Quem envia é sempre o usuário do token, e ele precisa participar dela.
     */
    @Transactional
    public MensagemNegociacao enviarMensagem(UUID negociacaoId, String mensagem, BigDecimal valorOfertado,
                                             UsuarioAutenticado remetente) {
        Negociacao negociacao = negociacaoService.buscarPorId(negociacaoId);
        negociacaoService.verificarParticipante(negociacao, remetente);

        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está ativa!");
        }

        boolean semTexto = mensagem == null || mensagem.isBlank();
        if (semTexto && valorOfertado == null) {
            throw new BusinessException("Informe uma mensagem ou um valor de contraproposta!");
        }

        MensagemNegociacao nova = new MensagemNegociacao();
        nova.setNegociacao(negociacao);
        nova.setMensagem(semTexto ? "Nova oferta de valor" : mensagem.trim());
        nova.setValorOfertado(valorOfertado);
        nova.setTipoRemetente(remetente.comoRemetente());
        nova.setRemetenteId(remetente.id());
        nova.setDataEnvio(LocalDateTime.now());

        nova = mensagemRepository.save(nova);
        negociacao.getMensagens().add(nova);
        return nova;
    }

    @Transactional(readOnly = true)
    public List<MensagemNegociacao> listarMensagens(UUID negociacaoId, UsuarioAutenticado usuario) {
        negociacaoService.buscarParaParticipante(negociacaoId, usuario);
        return mensagemRepository.findByNegociacaoIdOrderByDataEnvioAsc(negociacaoId);
    }

    @Transactional(readOnly = true)
    public MensagemNegociacao buscarPorId(UUID id, UsuarioAutenticado usuario) {
        MensagemNegociacao mensagem = mensagemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada!"));
        negociacaoService.verificarParticipante(mensagem.getNegociacao(), usuario);
        return mensagem;
    }
}
