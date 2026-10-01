package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Empresa;
import com.gestao.entities.Proposta;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusProposta;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.CotacaoRepository;
import com.gestao.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private final CotacaoRepository cotacaoRepository;
    private final PropostaRepository propostaRepository;
    private final EmpresaService empresaService;

    @Transactional
    public Cotacao criarCotacao(Cotacao cotacao, UUID empresaId) {
        Empresa empresa = empresaService.buscarPorId(empresaId);
        validarDataLimite(cotacao.getDataLimite());

        cotacao.setEmpresa(empresa);
        cotacao.setStatus(StatusCotacao.ABERTA);
        cotacao.setDataCriacao(LocalDateTime.now());
        if (cotacao.getCategoria() == null) {
            cotacao.setCategoria(CategoriaCotacao.OUTROS);
        }

        return cotacaoRepository.save(cotacao);
    }

    @Transactional(readOnly = true)
    public Cotacao buscarPorId(UUID id) {
        return cotacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<Cotacao> listarPorEmpresa(UUID empresaId) {
        return cotacaoRepository.findByEmpresaId(empresaId);
    }

    /** Mural do fornecedor: só cotações abertas e dentro do prazo. */
    @Transactional(readOnly = true)
    public List<Cotacao> listarCotacoesAbertas() {
        return cotacaoRepository.findAbertasVigentes(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Cotacao> listarPorStatus(StatusCotacao status) {
        return cotacaoRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Cotacao> listarPorEmpresaEStatus(UUID empresaId, StatusCotacao status) {
        return cotacaoRepository.findByEmpresaIdAndStatus(empresaId, status);
    }

    @Transactional
    public Cotacao atualizarStatus(UUID id, StatusCotacao novoStatus) {
        Cotacao cotacao = buscarPorId(id);
        cotacao.setStatus(novoStatus);
        return cotacaoRepository.save(cotacao);
    }

    @Transactional
    public Cotacao atualizarCotacao(UUID id, Cotacao cotacaoAtualizada) {
        Cotacao cotacao = buscarPorId(id);

        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Só é possível editar cotações abertas!");
        }
        validarDataLimite(cotacaoAtualizada.getDataLimite());

        cotacao.setNomeServico(cotacaoAtualizada.getNomeServico());
        cotacao.setRequisitos(cotacaoAtualizada.getRequisitos());
        cotacao.setDataLimite(cotacaoAtualizada.getDataLimite());
        cotacao.setOrcamentoEstimado(cotacaoAtualizada.getOrcamentoEstimado());
        if (cotacaoAtualizada.getCategoria() != null) {
            cotacao.setCategoria(cotacaoAtualizada.getCategoria());
        }

        return cotacaoRepository.save(cotacao);
    }

    /**
     * Cancela uma cotação aberta. Propostas que ainda aguardavam análise
     * passam para RECUSADA, para o fornecedor ver o desfecho no histórico.
     */
    @Transactional
    public Cotacao cancelarCotacao(UUID id) {
        Cotacao cotacao = buscarPorId(id);

        if (cotacao.getStatus() == StatusCotacao.EM_NEGOCIACAO) {
            throw new BusinessException("Encerre a negociação em andamento antes de cancelar a cotação.");
        }
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Esta cotação já está encerrada.");
        }

        for (Proposta proposta : propostaRepository.findByCotacaoId(id)) {
            if (proposta.getStatus() == StatusProposta.ENVIADA || proposta.getStatus() == StatusProposta.EM_ANALISE) {
                proposta.setStatus(StatusProposta.RECUSADA);
            }
        }

        cotacao.setStatus(StatusCotacao.CANCELADA);
        return cotacaoRepository.save(cotacao);
    }

    @Transactional
    public void deletarCotacao(UUID id) {
        Cotacao cotacao = buscarPorId(id);
        if (propostaRepository.countByCotacaoId(id) > 0) {
            throw new BusinessException("Cotações que já receberam propostas não podem ser excluídas. Cancele-a.");
        }
        cotacaoRepository.delete(cotacao);
    }

    @Transactional(readOnly = true)
    public List<Cotacao> listarTodas() {
        return cotacaoRepository.findAll();
    }

    private void validarDataLimite(LocalDateTime dataLimite) {
        if (dataLimite != null && dataLimite.isBefore(LocalDateTime.now())) {
            throw new BusinessException("A data limite precisa ser uma data futura.");
        }
    }
}
