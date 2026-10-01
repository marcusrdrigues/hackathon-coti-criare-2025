package com.gestao.services;

import com.gestao.entities.*;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusProposta;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final CotacaoService cotacaoService;
    private final FornecedorService fornecedorService;

    // Criar proposta
    public Proposta criarProposta(Proposta proposta, UUID fornecedorId, UUID cotacaoId) {
        // Buscar fornecedor
        Fornecedor fornecedor = fornecedorService.buscarPorId(fornecedorId);
        proposta.setFornecedor(fornecedor);

        // Buscar cotação
        Cotacao cotacao = cotacaoService.buscarPorId(cotacaoId);

        // Validar se cotação está ABERTA
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Cotação não está mais aberta para propostas!");
        }

        // Validar se fornecedor já enviou proposta para esta cotação
        if (propostaRepository.existsByFornecedorIdAndCotacaoId(fornecedorId, cotacaoId)) {
            throw new BusinessException("Você já enviou uma proposta para esta cotação!");
        }

        proposta.setCotacao(cotacao);
        proposta.setStatus(StatusProposta.ENVIADA);

        return propostaRepository.save(proposta);
    }

    // Buscar proposta por ID
    public Proposta buscarPorId(UUID id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposta não encontrada!"));
    }

    // Listar propostas de uma cotação
    public List<Proposta> listarPorCotacao(UUID cotacaoId) {
        return propostaRepository.findByCotacaoId(cotacaoId);
    }

    // Listar propostas de um fornecedor
    public List<Proposta> listarPorFornecedor(UUID fornecedorId) {
        return propostaRepository.findByFornecedorId(fornecedorId);
    }

    // Listar propostas por status
    public List<Proposta> listarPorStatus(StatusProposta status) {
        return propostaRepository.findByStatus(status);
    }

    // Atualizar status da proposta
    public Proposta atualizarStatus(UUID id, StatusProposta novoStatus) {
        Proposta proposta = buscarPorId(id);
        proposta.setStatus(novoStatus);
        return propostaRepository.save(proposta);
    }

    // Aceitar proposta (usado pela empresa)
    public Proposta aceitarProposta(UUID id) {
        Proposta proposta = buscarPorId(id);

        // Validar se proposta pode ser aceita
        if (proposta.getStatus() != StatusProposta.ENVIADA &&
                proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new BusinessException("Esta proposta não pode ser aceita!");
        }

        // Atualizar status da proposta
        proposta.setStatus(StatusProposta.ACEITA);

        // Atualizar status da cotação para EM_NEGOCIACAO
        cotacaoService.atualizarStatus(proposta.getCotacao().getId(), StatusCotacao.EM_NEGOCIACAO);

        return propostaRepository.save(proposta);
    }

    // Recusar proposta
    public Proposta recusarProposta(UUID id) {
        return atualizarStatus(id, StatusProposta.RECUSADA);
    }

    // Atualizar proposta
    public Proposta atualizarProposta(UUID id, Proposta propostaAtualizada) {
        Proposta proposta = buscarPorId(id);

        // Só permite atualizar se estiver ENVIADA
        if (proposta.getStatus() != StatusProposta.ENVIADA) {
            throw new BusinessException("Só é possível editar propostas que ainda não foram analisadas!");
        }

        proposta.setValor(propostaAtualizada.getValor());
        proposta.setDescricao(propostaAtualizada.getDescricao());

        return propostaRepository.save(proposta);
    }

    // Contar propostas de uma cotação
    public long contarPropostasPorCotacao(UUID cotacaoId) {
        return propostaRepository.countByCotacaoId(cotacaoId);
    }

    // Deletar proposta
    public void deletarProposta(UUID id) {
        Proposta proposta = buscarPorId(id);
        propostaRepository.delete(proposta);
    }
}