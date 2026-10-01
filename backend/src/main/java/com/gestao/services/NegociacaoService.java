package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.MensagemNegociacao;
import com.gestao.entities.Negociacao;
import com.gestao.entities.Proposta;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.enums.StatusProposta;
import com.gestao.enums.TipoRemetente;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.MensagemNegociacaoRepository;
import com.gestao.repositories.NegociacaoRepository;
import com.gestao.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NegociacaoService {

    private final NegociacaoRepository negociacaoRepository;
    private final PropostaRepository propostaRepository;
    private final MensagemNegociacaoRepository mensagemRepository;
    private final PropostaService propostaService;

    /**
     * Abre a negociação com o fornecedor da proposta escolhida.
     * Se a proposta ainda não foi aceita, ela é aceita aqui mesmo (na mesma
     * transação), e a cotação passa para EM_NEGOCIACAO.
     */
    @Transactional
    public Negociacao criarNegociacao(UUID propostaId) {
        if (negociacaoRepository.existsByPropostaId(propostaId)) {
            throw new BusinessException("Já existe uma negociação para esta proposta!");
        }

        Proposta proposta = propostaService.buscarPorId(propostaId);
        if (proposta.getStatus() == StatusProposta.ENVIADA || proposta.getStatus() == StatusProposta.EM_ANALISE) {
            proposta = propostaService.aceitarProposta(propostaId);
        }
        if (proposta.getStatus() != StatusProposta.ACEITA) {
            throw new BusinessException("Só é possível negociar uma proposta enviada ou aceita!");
        }

        Negociacao negociacao = new Negociacao();
        negociacao.setProposta(proposta);
        negociacao.setEmpresa(proposta.getCotacao().getEmpresa());
        negociacao.setFornecedor(proposta.getFornecedor());
        negociacao.setStatus(StatusNegociacao.EM_ANDAMENTO);
        negociacao.setDataInicio(LocalDateTime.now());
        negociacao = negociacaoRepository.save(negociacao);
        proposta.setNegociacao(negociacao);

        // A proposta original abre o histórico da negociação
        MensagemNegociacao inicial = new MensagemNegociacao();
        inicial.setNegociacao(negociacao);
        inicial.setTipoRemetente(TipoRemetente.FORNECEDOR);
        inicial.setRemetenteId(proposta.getFornecedor().getId());
        inicial.setMensagem(proposta.getDescricao());
        inicial.setValorOfertado(proposta.getValor());
        inicial.setDataEnvio(proposta.getDataEnvio() != null ? proposta.getDataEnvio() : negociacao.getDataInicio());
        negociacao.getMensagens().add(mensagemRepository.save(inicial));

        return negociacao;
    }

    @Transactional(readOnly = true)
    public Negociacao buscarPorId(UUID id) {
        return negociacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negociação não encontrada!"));
    }

    @Transactional(readOnly = true)
    public Negociacao buscarPorProposta(UUID propostaId) {
        return negociacaoRepository.findByPropostaId(propostaId)
                .orElseThrow(() -> new ResourceNotFoundException("Negociação não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarPorEmpresa(UUID empresaId) {
        return negociacaoRepository.findByEmpresaId(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarPorFornecedor(UUID fornecedorId) {
        return negociacaoRepository.findByFornecedorId(fornecedorId);
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarPorStatus(StatusNegociacao status) {
        return negociacaoRepository.findByStatus(status);
    }

    /**
     * Fecha o negócio: grava o valor final, fecha a cotação e recusa as demais
     * propostas que ainda estavam pendentes.
     */
    @Transactional
    public Negociacao finalizarNegociacao(UUID id, BigDecimal valorFinal) {
        Negociacao negociacao = buscarPorId(id);

        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está em andamento!");
        }

        negociacao.setValorFinal(valorFinal != null ? valorFinal : negociacao.getUltimaOferta());
        negociacao.setStatus(StatusNegociacao.FINALIZADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        Proposta vencedora = negociacao.getProposta();
        Cotacao cotacao = vencedora.getCotacao();
        cotacao.setStatus(StatusCotacao.FECHADA);

        for (Proposta outra : propostaRepository.findByCotacaoId(cotacao.getId())) {
            if (!outra.getId().equals(vencedora.getId())
                    && (outra.getStatus() == StatusProposta.ENVIADA || outra.getStatus() == StatusProposta.EM_ANALISE)) {
                outra.setStatus(StatusProposta.RECUSADA);
            }
        }

        return negociacaoRepository.save(negociacao);
    }

    /**
     * Encerra a negociação sem acordo: a proposta é recusada e a cotação
     * volta a ficar aberta para negociar com outro fornecedor.
     */
    @Transactional
    public Negociacao cancelarNegociacao(UUID id) {
        Negociacao negociacao = buscarPorId(id);

        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está em andamento!");
        }

        negociacao.setStatus(StatusNegociacao.CANCELADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        Proposta proposta = negociacao.getProposta();
        proposta.setStatus(StatusProposta.RECUSADA);
        proposta.getCotacao().setStatus(StatusCotacao.ABERTA);

        return negociacaoRepository.save(negociacao);
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarTodas() {
        return negociacaoRepository.findAll();
    }
}
