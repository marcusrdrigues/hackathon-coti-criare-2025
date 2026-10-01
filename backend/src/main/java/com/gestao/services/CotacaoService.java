package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Empresa;
import com.gestao.entities.Proposta;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusProposta;
import com.gestao.exceptions.AcessoNegadoException;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.CotacaoRepository;
import com.gestao.repositories.PropostaRepository;
import com.gestao.security.UsuarioAutenticado;
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

    /**
     * Fornecedores enxergam qualquer cotação (é o que aparece no mural);
     * empresas só enxergam as próprias.
     */
    @Transactional(readOnly = true)
    public Cotacao buscarParaUsuario(UUID id, UsuarioAutenticado usuario) {
        Cotacao cotacao = buscarPorId(id);
        if (usuario.ehEmpresa()) {
            verificarDona(cotacao, usuario.id());
        }
        return cotacao;
    }

    /** Busca a cotação garantindo que ela pertence à empresa informada. */
    @Transactional(readOnly = true)
    public Cotacao buscarDaEmpresa(UUID id, UUID empresaId) {
        Cotacao cotacao = buscarPorId(id);
        verificarDona(cotacao, empresaId);
        return cotacao;
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
    public Cotacao atualizarCotacao(UUID id, UUID empresaId, Cotacao cotacaoAtualizada) {
        Cotacao cotacao = buscarDaEmpresa(id, empresaId);

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
    public Cotacao cancelarCotacao(UUID id, UUID empresaId) {
        Cotacao cotacao = buscarDaEmpresa(id, empresaId);

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
    public void deletarCotacao(UUID id, UUID empresaId) {
        Cotacao cotacao = buscarDaEmpresa(id, empresaId);
        if (propostaRepository.countByCotacaoId(id) > 0) {
            throw new BusinessException("Cotações que já receberam propostas não podem ser excluídas. Cancele-a.");
        }
        cotacaoRepository.delete(cotacao);
    }


    private void verificarDona(Cotacao cotacao, UUID empresaId) {
        if (!cotacao.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta cotação pertence a outra empresa.");
        }
    }

    private void validarDataLimite(LocalDateTime dataLimite) {
        if (dataLimite != null && dataLimite.isBefore(LocalDateTime.now())) {
            throw new BusinessException("A data limite precisa ser uma data futura.");
        }
    }
}
