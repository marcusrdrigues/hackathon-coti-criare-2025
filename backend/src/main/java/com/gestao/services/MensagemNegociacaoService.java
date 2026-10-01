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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MensagemNegociacaoService {

    private final MensagemNegociacaoRepository mensagemRepository;
    private final NegociacaoService negociacaoService;

    // Enviar mensagem
    public MensagemNegociacao enviarMensagem(UUID negociacaoId, String mensagem,
                                             TipoRemetente tipoRemetente, UUID remetenteId) {
        // Buscar negociação
        Negociacao negociacao = negociacaoService.buscarPorId(negociacaoId);

        // Validar se negociação está ativa
        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new BusinessException("Negociação não está ativa!");
        }

        // Criar mensagem
        MensagemNegociacao mensagemNegociacao = new MensagemNegociacao();
        mensagemNegociacao.setNegociacao(negociacao);
        mensagemNegociacao.setMensagem(mensagem);
        mensagemNegociacao.setTipoRemetente(tipoRemetente);
        mensagemNegociacao.setRemetenteId(remetenteId);
        mensagemNegociacao.setDataEnvio(LocalDate.now());

        return mensagemRepository.save(mensagemNegociacao);
    }

    // Buscar mensagens de uma negociação
    public List<MensagemNegociacao> listarMensagens(UUID negociacaoId) {
        return mensagemRepository.findByNegociacaoIdOrderByDataEnvioAsc(negociacaoId);
    }

    // Buscar mensagem por ID
    public MensagemNegociacao buscarPorId(UUID id) {
        return mensagemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada!"));
    }

    // Deletar mensagem
    public void deletarMensagem(UUID id) {
        MensagemNegociacao mensagem = buscarPorId(id);
        mensagemRepository.delete(mensagem);
    }
}