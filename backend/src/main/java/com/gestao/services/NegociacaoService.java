package com.gestao.services;

import com.gestao.entities.Cotacao;
import com.gestao.entities.MensagemNegociacao;
import com.gestao.entities.Negociacao;
import com.gestao.entities.Proposta;
import com.gestao.enums.StatusCotacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.enums.StatusProposta;
import com.gestao.enums.TipoRemetente;
import com.gestao.eventos.NegociacaoAlteradaEvento;
import com.gestao.exceptions.AcessoNegadoException;
import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.MensagemNegociacaoRepository;
import com.gestao.repositories.NegociacaoRepository;
import com.gestao.repositories.PropostaRepository;
import com.gestao.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventos;

    /**
     * Abre a negociação com o fornecedor da proposta escolhida.
     * Se a proposta ainda não foi aceita, ela é aceita aqui mesmo (na mesma
     * transação), e a cotação passa para EM_NEGOCIACAO.
     */
    @Transactional
    public Negociacao criarNegociacao(UUID propostaId, UUID empresaId) {
        Proposta proposta = propostaService.buscarPorId(propostaId);
        propostaService.verificarEmpresaDaCotacao(proposta, empresaId);

        if (negociacaoRepository.existsByPropostaId(propostaId)) {
            throw new BusinessException("Já existe uma negociação para esta proposta!");
        }

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

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.INICIADA));
        return negociacao;
    }

    @Transactional(readOnly = true)
    public Negociacao buscarPorId(UUID id) {
        return negociacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negociação não encontrada!"));
    }

    /** Só a empresa e o fornecedor da negociação podem vê-la. */
    @Transactional(readOnly = true)
    public Negociacao buscarParaParticipante(UUID id, UsuarioAutenticado usuario) {
        Negociacao negociacao = buscarPorId(id);
        verificarParticipante(negociacao, usuario);
        return negociacao;
    }

    @Transactional(readOnly = true)
    public Negociacao buscarPorPropostaParaParticipante(UUID propostaId, UsuarioAutenticado usuario) {
        Negociacao negociacao = negociacaoRepository.findByPropostaId(propostaId)
                .orElseThrow(() -> new ResourceNotFoundException("Negociação não encontrada!"));
        verificarParticipante(negociacao, usuario);
        return negociacao;
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarDoUsuario(UsuarioAutenticado usuario) {
        return usuario.ehEmpresa()
                ? negociacaoRepository.findByEmpresaId(usuario.id())
                : negociacaoRepository.findByFornecedorId(usuario.id());
    }

    /**
     * Fecha o negócio: grava o valor final, fecha a cotação e recusa as demais
     * propostas que ainda estavam pendentes.
     */
    @Transactional
    public Negociacao finalizarNegociacao(UUID id, BigDecimal valorFinal, UUID empresaId) {
        Negociacao negociacao = buscarPorId(id);
        verificarEmpresa(negociacao, empresaId);

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

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.FINALIZADA));
        return negociacaoRepository.save(negociacao);
    }

    /**
     * Encerra a negociação sem acordo: a proposta é recusada e a cotação
     * volta a ficar aberta para negociar com outro fornecedor.
     */
    @Transactional
    public Negociacao cancelarNegociacao(UUID id, UUID empresaId) {
        Negociacao negociacao = buscarPorId(id);
        verificarEmpresa(negociacao, empresaId);

        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está em andamento!");
        }

        negociacao.setStatus(StatusNegociacao.CANCELADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        Proposta proposta = negociacao.getProposta();
        proposta.setStatus(StatusProposta.RECUSADA);
        proposta.getCotacao().setStatus(StatusCotacao.ABERTA);

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.CANCELADA));
        return negociacaoRepository.save(negociacao);
    }

    void verificarParticipante(Negociacao negociacao, UsuarioAutenticado usuario) {
        UUID participante = usuario.ehEmpresa()
                ? negociacao.getEmpresa().getId()
                : negociacao.getFornecedor().getId();
        if (!participante.equals(usuario.id())) {
            throw new AcessoNegadoException("Você não participa desta negociação.");
        }
    }

    /** Fechar ou encerrar é decisão da empresa compradora. */
    private void verificarEmpresa(Negociacao negociacao, UUID empresaId) {
        if (!negociacao.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Só a empresa desta negociação pode fechá-la ou encerrá-la.");
        }
    }
}
