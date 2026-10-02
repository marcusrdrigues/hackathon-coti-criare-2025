package com.gestao.compras.aplicacao;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.compras.aplicacao.porta.MensagemNegociacaoRepositorio;
import com.gestao.compras.aplicacao.porta.NegociacaoRepositorio;
import com.gestao.compras.aplicacao.porta.PropostaRepositorio;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.NegociacaoAlteradaEvento;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.compras.dominio.TipoRemetente;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NegociacaoService {

    private final NegociacaoRepositorio negociacaoRepositorio;
    private final PropostaRepositorio propostaRepositorio;
    private final MensagemNegociacaoRepositorio mensagemRepositorio;
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

        if (negociacaoRepositorio.existeParaProposta(propostaId)) {
            throw new RegraDeNegocioException("Já existe uma negociação para esta proposta!");
        }

        if (proposta.getStatus() == StatusProposta.ENVIADA || proposta.getStatus() == StatusProposta.EM_ANALISE) {
            proposta = propostaService.aceitarProposta(propostaId);
        }
        if (proposta.getStatus() != StatusProposta.ACEITA) {
            throw new RegraDeNegocioException("Só é possível negociar uma proposta enviada ou aceita!");
        }

        Negociacao negociacao = new Negociacao();
        negociacao.setProposta(proposta);
        negociacao.setEmpresa(proposta.getCotacao().getEmpresa());
        negociacao.setFornecedor(proposta.getFornecedor());
        negociacao.setStatus(StatusNegociacao.EM_ANDAMENTO);
        negociacao.setDataInicio(LocalDateTime.now());
        // Quem abre a negociação já leu a proposta que a originou
        negociacao.setLidaEmpresaEm(negociacao.getDataInicio());
        negociacao = negociacaoRepositorio.salvar(negociacao);
        proposta.setNegociacao(negociacao);

        // A proposta original abre o histórico da negociação
        MensagemNegociacao inicial = new MensagemNegociacao();
        inicial.setNegociacao(negociacao);
        inicial.setTipoRemetente(TipoRemetente.FORNECEDOR);
        inicial.setRemetenteId(proposta.getFornecedor().getId());
        inicial.setMensagem(proposta.getDescricao());
        inicial.setValorOfertado(proposta.getValor());
        inicial.setDataEnvio(proposta.getDataEnvio() != null ? proposta.getDataEnvio() : negociacao.getDataInicio());
        negociacao.getMensagens().add(mensagemRepositorio.salvar(inicial));

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.INICIADA));
        return negociacao;
    }

    @Transactional(readOnly = true)
    public Negociacao buscarPorId(UUID id) {
        return negociacaoRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociação não encontrada!"));
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
        Negociacao negociacao = negociacaoRepositorio.buscarPorProposta(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociação não encontrada!"));
        verificarParticipante(negociacao, usuario);
        return negociacao;
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarDoUsuario(UsuarioAutenticado usuario) {
        return usuario.ehEmpresa()
                ? negociacaoRepositorio.listarDaEmpresa(usuario.id())
                : negociacaoRepositorio.listarDoFornecedor(usuario.id());
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
            throw new RegraDeNegocioException("Negociação não está em andamento!");
        }

        negociacao.setValorFinal(valorFinal != null ? valorFinal : negociacao.getUltimaOferta());
        negociacao.setStatus(StatusNegociacao.FINALIZADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        Proposta vencedora = negociacao.getProposta();
        Cotacao cotacao = vencedora.getCotacao();
        cotacao.setStatus(StatusCotacao.FECHADA);

        for (Proposta outra : propostaRepositorio.listarDaCotacao(cotacao.getId())) {
            if (!outra.getId().equals(vencedora.getId())
                    && (outra.getStatus() == StatusProposta.ENVIADA || outra.getStatus() == StatusProposta.EM_ANALISE)) {
                outra.setStatus(StatusProposta.RECUSADA);
            }
        }

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.FINALIZADA));
        return negociacaoRepositorio.salvar(negociacao);
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
            throw new RegraDeNegocioException("Negociação não está em andamento!");
        }

        negociacao.setStatus(StatusNegociacao.CANCELADA);
        negociacao.setDataFinalizacao(LocalDate.now());

        Proposta proposta = negociacao.getProposta();
        proposta.setStatus(StatusProposta.RECUSADA);
        proposta.getCotacao().setStatus(StatusCotacao.ABERTA);

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.CANCELADA));
        return negociacaoRepositorio.salvar(negociacao);
    }

    /**
     * Marca a negociação como lida agora por quem pediu: as mensagens da outra
     * parte enviadas até este momento deixam de contar como não lidas.
     */
    @Transactional
    public void marcarComoLida(UUID id, UsuarioAutenticado usuario) {
        registrarLeitura(buscarParaParticipante(id, usuario), usuario);
    }

    /** Mensagens da outra parte que o usuário ainda não viu. */
    @Transactional(readOnly = true)
    public int contarNaoLidas(Negociacao negociacao, UsuarioAutenticado usuario) {
        LocalDateTime lidaEm = usuario.ehEmpresa() ? negociacao.getLidaEmpresaEm() : negociacao.getLidaFornecedorEm();
        TipoRemetente leitor = TipoRemetente.de(usuario.tipo());
        long total = lidaEm == null
                ? mensagemRepositorio.contarDaOutraParte(negociacao.getId(), leitor)
                : mensagemRepositorio.contarDaOutraParteDepoisDe(negociacao.getId(), leitor, lidaEm);
        return (int) total;
    }

    /** Não lidas de todas as negociações do usuário, numa consulta só (id da negociação → total). */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> contarNaoLidas(UsuarioAutenticado usuario) {
        List<MensagemNegociacaoRepositorio.NaoLidas> contagens = usuario.ehEmpresa()
                ? mensagemRepositorio.contarNaoLidasDaEmpresa(usuario.id(), TipoRemetente.FORNECEDOR)
                : mensagemRepositorio.contarNaoLidasDoFornecedor(usuario.id(), TipoRemetente.EMPRESA);
        Map<UUID, Integer> porNegociacao = new HashMap<>();
        contagens.forEach(c -> porNegociacao.put(c.getNegociacaoId(), c.getTotal().intValue()));
        return porNegociacao;
    }

    void registrarLeitura(Negociacao negociacao, UsuarioAutenticado usuario) {
        if (usuario.ehEmpresa()) {
            negociacao.setLidaEmpresaEm(LocalDateTime.now());
        } else {
            negociacao.setLidaFornecedorEm(LocalDateTime.now());
        }
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
