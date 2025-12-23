package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Empresa;
import com.gestao.enums.StatusCotacao;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.CotacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private CotacaoRepository cotacaoRepository;
    private EmpresaService empresaService;

    // Criar cotação
    public Cotacao criarCotacao(Cotacao cotacao, UUID empresaId) {
        // Buscar empresa
        Empresa empresa = empresaService.buscarPorId(empresaId);
        cotacao.setEmpresa(empresa);

        // Definir status inicial e data de criação
        cotacao.setStatus(StatusCotacao.ABERTA);
        cotacao.setDataCriacao(LocalDateTime.now());

        return cotacaoRepository.save(cotacao);
    }

    // Buscar cotação por ID
    public Cotacao buscarPorId(UUID id) {
        return cotacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação não encontrada!"));
    }

    // Listar cotações de uma empresa
    public List<Cotacao> listarPorEmpresa(UUID empresaId) {
        return cotacaoRepository.findByEmpresaId(empresaId);
    }

    // Listar cotações abertas (para fornecedores verem)
    public List<Cotacao> listarCotacoesAbertas() {
        return cotacaoRepository.findByStatusOrderByDataCriacaoDesc(StatusCotacao.ABERTA);
    }

    // Listar cotações por status
    public List<Cotacao> listarPorStatus(StatusCotacao status) {
        return cotacaoRepository.findByStatus(status);
    }

    // Listar cotações de uma empresa por status
    public List<Cotacao> listarPorEmpresaEStatus(UUID empresaId, StatusCotacao status) {
        return cotacaoRepository.findByEmpresaIdAndStatus(empresaId, status);
    }

    // Atualizar status da cotação
    public Cotacao atualizarStatus(UUID id, StatusCotacao novoStatus) {
        Cotacao cotacao = buscarPorId(id);
        cotacao.setStatus(novoStatus);
        return cotacaoRepository.save(cotacao);
    }

    // Atualizar cotação
    public Cotacao atualizarCotacao(UUID id, Cotacao cotacaoAtualizada) {
        Cotacao cotacao = buscarPorId(id);

        // Só permite atualizar se estiver ABERTA
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new BusinessException("Só é possível editar cotações abertas!");
        }

        cotacao.setNomeServico(cotacaoAtualizada.getNomeServico());
        cotacao.setRequisitos(cotacaoAtualizada.getRequisitos());
        cotacao.setDataLimite(cotacaoAtualizada.getDataLimite());

        return cotacaoRepository.save(cotacao);
    }

    // Cancelar cotação
    public Cotacao cancelarCotacao(UUID id) {
        return atualizarStatus(id, StatusCotacao.CANCELADA);
    }

    // Deletar cotação
    public void deletarCotacao(UUID id) {
        Cotacao cotacao = buscarPorId(id);
        cotacaoRepository.delete(cotacao);
    }

    // Listar todas as cotações
    public List<Cotacao> listarTodas() {
        return cotacaoRepository.findAll();
    }
}