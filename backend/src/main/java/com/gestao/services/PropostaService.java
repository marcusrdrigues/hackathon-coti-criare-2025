package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Fornecedor;
import com.gestao.entities.Proposta;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusProposta;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final CotacaoService cotacaoService;
    private final FornecedorService fornecedorService;

    @Transactional
    public Proposta criarProposta(Proposta proposta, UUID fornecedorId, UUID cotacaoId) {
        Fornecedor fornecedor = fornecedorService.buscarPorId(fornecedorId);
        Cotacao cotacao = cotacaoService.buscarPorId(cotacaoId);

        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Cotação não está mais aberta para propostas!");
        }
        if (cotacao.isPrazoEncerrado()) {
            throw new BusinessException("O prazo para envio de propostas desta cotação já terminou!");
        }
        if (propostaRepository.existsByFornecedorIdAndCotacaoId(fornecedorId, cotacaoId)) {
            throw new BusinessException("Você já enviou uma proposta para esta cotação!");
        }

        proposta.setFornecedor(fornecedor);
        proposta.setCotacao(cotacao);
        proposta.setStatus(StatusProposta.ENVIADA);
        proposta.setDataEnvio(LocalDateTime.now());

        return propostaRepository.save(proposta);
    }

    @Transactional(readOnly = true)
    public Proposta buscarPorId(UUID id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposta não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<Proposta> listarPorCotacao(UUID cotacaoId) {
        return propostaRepository.findByCotacaoId(cotacaoId);
    }

    @Transactional(readOnly = true)
    public List<Proposta> listarPorFornecedor(UUID fornecedorId) {
        return propostaRepository.findByFornecedorId(fornecedorId);
    }

    @Transactional(readOnly = true)
    public List<Proposta> listarPorStatus(StatusProposta status) {
        return propostaRepository.findByStatus(status);
    }

    /**
     * Endpoint genérico de status: só permite marcar uma proposta recém-enviada
     * como "em análise". Aceitar e recusar têm endpoints próprios com regras.
     */
    @Transactional
    public Proposta atualizarStatus(UUID id, StatusProposta novoStatus) {
        Proposta proposta = buscarPorId(id);

        if (novoStatus != StatusProposta.EM_ANALISE || proposta.getStatus() != StatusProposta.ENVIADA) {
            throw new BusinessException("Use os endpoints de aceitar/recusar para mudar o status desta proposta.");
        }

        proposta.setStatus(novoStatus);
        return propostaRepository.save(proposta);
    }

    /**
     * Aceitar uma proposta significa escolher o fornecedor com quem negociar:
     * a cotação vai para EM_NEGOCIACAO e não aceita outra negociação em paralelo.
     */
    @Transactional
    public Proposta aceitarProposta(UUID id) {
        Proposta proposta = buscarPorId(id);

        if (proposta.getStatus() != StatusProposta.ENVIADA && proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new BusinessException("Esta proposta não pode ser aceita!");
        }

        Cotacao cotacao = proposta.getCotacao();
        if (cotacao.getStatus() == StatusCotacao.EM_NEGOCIACAO) {
            throw new BusinessException("Já existe uma negociação em andamento para esta cotação!");
        }
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Esta cotação já está encerrada!");
        }

        proposta.setStatus(StatusProposta.ACEITA);
        cotacao.setStatus(StatusCotacao.EM_NEGOCIACAO);

        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta recusarProposta(UUID id) {
        Proposta proposta = buscarPorId(id);

        if (proposta.getStatus() != StatusProposta.ENVIADA && proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new BusinessException("Só é possível recusar propostas que ainda aguardam análise!");
        }

        proposta.setStatus(StatusProposta.RECUSADA);
        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta atualizarProposta(UUID id, UUID fornecedorId, Proposta propostaAtualizada) {
        Proposta proposta = buscarPorId(id);

        if (!proposta.getFornecedor().getId().equals(fornecedorId)) {
            throw new BusinessException("Esta proposta pertence a outro fornecedor!");
        }
        if (proposta.getStatus() != StatusProposta.ENVIADA) {
            throw new BusinessException("Só é possível editar propostas que ainda não foram analisadas!");
        }
        if (proposta.getCotacao().getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Cotação não está mais aberta para propostas!");
        }

        proposta.setValor(propostaAtualizada.getValor());
        proposta.setDescricao(propostaAtualizada.getDescricao());

        return propostaRepository.save(proposta);
    }

    @Transactional(readOnly = true)
    public long contarPropostasPorCotacao(UUID cotacaoId) {
        return propostaRepository.countByCotacaoId(cotacaoId);
    }

    /** O fornecedor pode retirar a proposta enquanto ela não foi aceita. */
    @Transactional
    public void deletarProposta(UUID id) {
        Proposta proposta = buscarPorId(id);
        if (proposta.getStatus() == StatusProposta.ACEITA) {
            throw new BusinessException("Propostas aceitas não podem ser excluídas!");
        }
        propostaRepository.delete(proposta);
    }
}
