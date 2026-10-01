package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.Fornecedor;
import com.gestao.entities.Proposta;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusProposta;
import com.gestao.eventos.PropostaRecebidaEvento;
import com.gestao.exceptions.AcessoNegadoException;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.PropostaRepository;
import com.gestao.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventos;

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

        Proposta salva = propostaRepository.save(proposta);
        eventos.publishEvent(new PropostaRecebidaEvento(salva.getId()));
        return salva;
    }

    @Transactional(readOnly = true)
    public Proposta buscarPorId(UUID id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposta não encontrada!"));
    }

    /** Só o fornecedor que enviou e a empresa dona da cotação podem ver a proposta. */
    @Transactional(readOnly = true)
    public Proposta buscarParaUsuario(UUID id, UsuarioAutenticado usuario) {
        Proposta proposta = buscarPorId(id);
        boolean autor = usuario.ehFornecedor() && proposta.getFornecedor().getId().equals(usuario.id());
        boolean empresaDaCotacao = usuario.ehEmpresa()
                && proposta.getCotacao().getEmpresa().getId().equals(usuario.id());
        if (!autor && !empresaDaCotacao) {
            throw new AcessoNegadoException("Você não tem acesso a esta proposta.");
        }
        return proposta;
    }

    /**
     * Lista as propostas de uma cotação. Só a empresa dona pode ver: um
     * fornecedor não pode descobrir o lance dos concorrentes.
     */
    @Transactional(readOnly = true)
    public List<Proposta> listarPorCotacao(UUID cotacaoId, UUID empresaId) {
        cotacaoService.buscarDaEmpresa(cotacaoId, empresaId);
        return propostaRepository.findByCotacaoId(cotacaoId);
    }

    @Transactional(readOnly = true)
    public List<Proposta> listarPorFornecedor(UUID fornecedorId) {
        return propostaRepository.findByFornecedorId(fornecedorId);
    }

    /**
     * Aceitar uma proposta significa escolher o fornecedor com quem negociar:
     * a cotação vai para EM_NEGOCIACAO e não aceita outra negociação em paralelo.
     * Usado pelo NegociacaoService, que já confere se a empresa é a dona.
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
    public Proposta recusarProposta(UUID id, UUID empresaId) {
        Proposta proposta = buscarPorId(id);
        verificarEmpresaDaCotacao(proposta, empresaId);

        if (proposta.getStatus() != StatusProposta.ENVIADA && proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new BusinessException("Só é possível recusar propostas que ainda aguardam análise!");
        }

        proposta.setStatus(StatusProposta.RECUSADA);
        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta atualizarProposta(UUID id, UUID fornecedorId, Proposta propostaAtualizada) {
        Proposta proposta = buscarPorId(id);
        verificarAutor(proposta, fornecedorId);

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
    public void deletarProposta(UUID id, UUID fornecedorId) {
        Proposta proposta = buscarPorId(id);
        verificarAutor(proposta, fornecedorId);

        if (proposta.getStatus() == StatusProposta.ACEITA) {
            throw new BusinessException("Propostas aceitas não podem ser excluídas!");
        }
        propostaRepository.delete(proposta);
    }

    void verificarEmpresaDaCotacao(Proposta proposta, UUID empresaId) {
        if (!proposta.getCotacao().getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta proposta foi enviada para a cotação de outra empresa.");
        }
    }

    private void verificarAutor(Proposta proposta, UUID fornecedorId) {
        if (!proposta.getFornecedor().getId().equals(fornecedorId)) {
            throw new AcessoNegadoException("Esta proposta pertence a outro fornecedor.");
        }
    }
}
