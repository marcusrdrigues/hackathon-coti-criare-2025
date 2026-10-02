package com.gestao.compras.aplicacao;

import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.compras.aplicacao.porta.MensagemNegociacaoRepositorio;
import com.gestao.compras.dominio.MensagemEnviadaEvento;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.TipoRemetente;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MensagemNegociacaoService {

    private final MensagemNegociacaoRepositorio mensagemRepositorio;
    private final NegociacaoService negociacaoService;
    private final ApplicationEventPublisher eventos;

    /**
     * Envia uma mensagem e/ou uma nova oferta de valor dentro da negociação.
     * Quem envia é sempre o usuário do token, e ele precisa participar dela.
     */
    @Transactional
    public MensagemNegociacao enviarMensagem(UUID negociacaoId, String mensagem, BigDecimal valorOfertado,
                                             UsuarioAutenticado remetente) {
        Negociacao negociacao = negociacaoService.buscarPorId(negociacaoId);
        negociacaoService.verificarParticipante(negociacao, remetente);

        if (negociacao.getStatus() != StatusNegociacao.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Negociação não está ativa!");
        }

        boolean semTexto = mensagem == null || mensagem.isBlank();
        if (semTexto && valorOfertado == null) {
            throw new RegraDeNegocioException("Informe uma mensagem ou um valor de contraproposta!");
        }

        MensagemNegociacao nova = new MensagemNegociacao();
        nova.setNegociacao(negociacao);
        nova.setMensagem(semTexto ? "Nova oferta de valor" : mensagem.trim());
        nova.setValorOfertado(valorOfertado);
        nova.setTipoRemetente(TipoRemetente.de(remetente.tipo()));
        nova.setRemetenteId(remetente.id());
        nova.setDataEnvio(LocalDateTime.now());

        nova = mensagemRepositorio.salvar(nova);
        negociacao.getMensagens().add(nova);
        // Quem responde já viu tudo o que veio antes
        negociacaoService.registrarLeitura(negociacao, remetente);

        eventos.publishEvent(new MensagemEnviadaEvento(nova.getId()));
        return nova;
    }

    @Transactional(readOnly = true)
    public List<MensagemNegociacao> listarMensagens(UUID negociacaoId, UsuarioAutenticado usuario) {
        negociacaoService.buscarParaParticipante(negociacaoId, usuario);
        return mensagemRepositorio.listarDaNegociacao(negociacaoId);
    }

    /** Sem checar quem pede: para uso interno do sistema, como os avisos em tempo real. */
    @Transactional(readOnly = true)
    public MensagemNegociacao buscarPorId(UUID id) {
        return mensagemRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem não encontrada!"));
    }
}
