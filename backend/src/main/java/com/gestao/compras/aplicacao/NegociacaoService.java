package com.gestao.compras.aplicacao;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
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
    private final AcessoCompras acesso;
    private final ApplicationEventPublisher eventos;

    /**
     * Abre a negociação com o fornecedor da proposta escolhida.
     * Se a proposta ainda não foi aceita, ela é aceita aqui mesmo (na mesma
     * transação), e a cotação passa para EM_NEGOCIACAO.
     */
    @Transactional
    public Negociacao criarNegociacao(UUID propostaId, UsuarioAutenticado usuario) {
        Proposta proposta = acesso.propostaRecebida(propostaId, usuario);

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
        inicial.setRemetente(proposta.getEnviadaPor());
        inicial.setMensagem(proposta.getDescricao());
        inicial.setValorOfertado(proposta.getValor());
        inicial.setDataEnvio(proposta.getDataEnvio() != null ? proposta.getDataEnvio() : negociacao.getDataInicio());
        negociacao.getMensagens().add(mensagemRepositorio.salvar(inicial));

        eventos.publishEvent(new NegociacaoAlteradaEvento(negociacao.getId(), NegociacaoAlteradaEvento.Tipo.INICIADA));
        return negociacao;
    }

    /** Sem checar quem pede: para uso interno do sistema. Quem atende um usuário passa pelo {@link AcessoCompras}. */
    @Transactional(readOnly = true)
    public Negociacao buscarPorId(UUID id) {
        return negociacaoRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(AcessoCompras.NEGOCIACAO_NAO_ENCONTRADA));
    }

    /** Só a empresa e o fornecedor da negociação podem vê-la. */
    @Transactional(readOnly = true)
    public Negociacao buscarParaParticipante(UUID id, UsuarioAutenticado usuario) {
        return acesso.negociacaoVisivel(id, usuario);
    }

    /** Uma página das negociações da organização de quem pede: as em andamento primeiro. */
    @Transactional(readOnly = true)
    public Pagina<Negociacao> buscarDoUsuario(UsuarioAutenticado usuario, SituacaoNegociacao situacao,
                                              PedidoDePagina pedido) {
        return negociacaoRepositorio.buscarDaOrganizacao(usuario.organizacaoId(), usuario.ehEmpresa(), situacao, pedido);
    }

    /**
     * Fecha o negócio: grava o valor final, fecha a cotação e recusa as demais
     * propostas que ainda estavam pendentes.
     */
    @Transactional
    public Negociacao finalizarNegociacao(UUID id, BigDecimal valorFinal, UsuarioAutenticado usuario) {
        Negociacao negociacao = acesso.negociacaoDaEmpresa(id, usuario);

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
    public Negociacao cancelarNegociacao(UUID id, UsuarioAutenticado usuario) {
        Negociacao negociacao = acesso.negociacaoDaEmpresa(id, usuario);

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
                ? mensagemRepositorio.contarNaoLidasDaEmpresa(usuario.organizacaoId(), TipoRemetente.FORNECEDOR)
                : mensagemRepositorio.contarNaoLidasDoFornecedor(usuario.organizacaoId(), TipoRemetente.EMPRESA);
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
}
