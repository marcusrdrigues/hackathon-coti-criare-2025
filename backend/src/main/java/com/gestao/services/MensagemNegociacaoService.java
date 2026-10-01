package com.gestao.services;

import com.gestao.entities.MensagemNegociacao;
import com.gestao.entities.Negociacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.enums.TipoRemetente;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.MensagemNegociacaoRepository;
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
     * O remetente precisa ser a empresa ou o fornecedor desta negociação.
     */
    @Transactional
    public MensagemNegociacao enviarMensagem(UUID negociacaoId, String mensagem, BigDecimal valorOfertado,
                                             TipoRemetente tipoRemetente, UUID remetenteId) {
        Negociacao negociacao = negociacaoService.buscarPorId(negociacaoId);

        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está ativa!");
        }

        UUID participante = tipoRemetente == TipoRemetente.EMPRESA
                ? negociacao.getEmpresa().getId()
                : negociacao.getFornecedor().getId();
        if (!participante.equals(remetenteId)) {
            throw new BusinessException("O remetente não participa desta negociação!");
        }

        boolean semTexto = mensagem == null || mensagem.isBlank();
        if (semTexto && valorOfertado == null) {
            throw new BusinessException("Informe uma mensagem ou um valor de contraproposta!");
        }

        MensagemNegociacao nova = new MensagemNegociacao();
        nova.setNegociacao(negociacao);
        nova.setMensagem(semTexto ? "Nova oferta de valor" : mensagem.trim());
        nova.setValorOfertado(valorOfertado);
        nova.setTipoRemetente(tipoRemetente);
        nova.setRemetenteId(remetenteId);
        nova.setDataEnvio(LocalDateTime.now());

        nova = mensagemRepository.save(nova);
        negociacao.getMensagens().add(nova);
        return nova;
    }

    @Transactional(readOnly = true)
    public List<MensagemNegociacao> listarMensagens(UUID negociacaoId) {
        return mensagemRepository.findByNegociacaoIdOrderByDataEnvioAsc(negociacaoId);
    }

    @Transactional(readOnly = true)
    public MensagemNegociacao buscarPorId(UUID id) {
        return mensagemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada!"));
    }

    @Transactional
    public void deletarMensagem(UUID id) {
        MensagemNegociacao mensagem = buscarPorId(id);
        mensagemRepository.delete(mensagem);
    }
}
