package com.gestao.services;

import com.gestao.entities.*;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.enums.StatusProposta;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.NegociacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NegociacaoService {

    private final NegociacaoRepository negociacaoRepository;
    private final PropostaService propostaService;

    // Criar negociação (após aceitar proposta)
    public Negociacao criarNegociacao(UUID propostaId) {
        // Buscar proposta
        Proposta proposta = propostaService.buscarPorId(propostaId);

        // Validar se proposta foi aceita
        if (proposta.getStatus() != StatusProposta.ACEITA) {
            throw new BusinessException("Só é possível criar negociação com proposta aceita!");
        }

        // Validar se já existe negociação para esta proposta
        if (negociacaoRepository.existsByPropostaId(propostaId)) {
            throw new BusinessException("Já existe uma negociação para esta proposta!");
        }

        // Criar negociação
        Negociacao negociacao = new Negociacao();
        negociacao.setProposta(proposta);
        negociacao.setEmpresa(proposta.getCotacao().getEmpresa());
        negociacao.setFornecedor(proposta.getFornecedor());
        negociacao.setStatus(StatusNegociacao.EM_ANDAMENTO);
        negociacao.setDataInicio(LocalDateTime.now());

        return negociacaoRepository.save(negociacao);
    }

    // Buscar negociação por ID
    public Negociacao buscarPorId(UUID id) {
        return negociacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negociação não encontrada!"));
    }

    // Buscar negociação por proposta
    public Negociacao buscarPorProposta(UUID propostaId) {
        return negociacaoRepository.findByPropostaId(propostaId)
                .orElseThrow(() -> new ResourceNotFoundException("Negociação não encontrada!"));
    }

    // Listar negociações de uma empresa
    public List<Negociacao> listarPorEmpresa(UUID empresaId) {
        return negociacaoRepository.findByEmpresaId(empresaId);
    }

    // Listar negociações de um fornecedor
    public List<Negociacao> listarPorFornecedor(UUID fornecedorId) {
        return negociacaoRepository.findByFornecedorId(fornecedorId);
    }

    // Listar negociações por status
    public List<Negociacao> listarPorStatus(StatusNegociacao status) {
        return negociacaoRepository.findByStatus(status);
    }

    // Finalizar negociação
    public Negociacao finalizarNegociacao(UUID id, BigDecimal valorFinal) {
        Negociacao negociacao = buscarPorId(id);

        // Validar se está em andamento
        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está em andamento!");
        }

        negociacao.setValorFinal(valorFinal);
        negociacao.setStatus(StatusNegociacao.FINALIZADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        // Atualizar status da cotação para FECHADA
        Cotacao cotacao = negociacao.getProposta().getCotacao();
        cotacao.setStatus(StatusCotacao.FECHADA);

        return negociacaoRepository.save(negociacao);
    }

    // Cancelar negociação
    public Negociacao cancelarNegociacao(UUID id) {
        Negociacao negociacao = buscarPorId(id);

        negociacao.setStatus(StatusNegociacao.CANCELADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        // Voltar cotação para ABERTA
        Cotacao cotacao = negociacao.getProposta().getCotacao();
        cotacao.setStatus(StatusCotacao.ABERTA);

        return negociacaoRepository.save(negociacao);
    }

    // Listar todas as negociações
    public List<Negociacao> listarTodas() {
        return negociacaoRepository.findAll();
    }
}