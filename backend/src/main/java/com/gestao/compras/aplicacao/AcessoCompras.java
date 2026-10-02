package com.gestao.compras.aplicacao;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compras.aplicacao.porta.CotacaoRepositorio;
import com.gestao.compras.aplicacao.porta.NegociacaoRepositorio;
import com.gestao.compras.aplicacao.porta.PropostaRepositorio;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A política de acesso do módulo de compras, num lugar só (ADR 0016). Todo ponto de
 * entrada que recebe um id (REST, WebSocket e o que vier depois) passa por aqui.
 *
 * <p>Os dados pertencem à organização do token, e qualquer pessoa dela pode usá-los.
 * Um recurso de outra organização responde exatamente como um id que não existe
 * (404, mesma mensagem), para não revelar que o id é válido.
 */
@Component
@RequiredArgsConstructor
public class AcessoCompras {

    static final String COTACAO_NAO_ENCONTRADA = "Cotação não encontrada!";
    static final String PROPOSTA_NAO_ENCONTRADA = "Proposta não encontrada!";
    static final String NEGOCIACAO_NAO_ENCONTRADA = "Negociação não encontrada!";

    private final CotacaoRepositorio cotacaoRepositorio;
    private final PropostaRepositorio propostaRepositorio;
    private final NegociacaoRepositorio negociacaoRepositorio;

    // ---------------------------------------------------------------- cotação

    /**
     * A empresa vê as próprias cotações. O fornecedor vê as abertas (são o mural) e
     * aquelas para as quais já enviou proposta, para acompanhar o desfecho.
     */
    @Transactional(readOnly = true)
    public Cotacao cotacaoVisivel(UUID id, UsuarioAutenticado usuario) {
        Cotacao cotacao = cotacao(id);
        boolean daEmpresa = usuario.ehEmpresa() && ehDa(cotacao, usuario);
        boolean paraOFornecedor = usuario.ehFornecedor() && (cotacao.getStatus() == StatusCotacao.ABERTA
                || propostaRepositorio.existeDoFornecedorNaCotacao(usuario.organizacaoId(), id));
        return exigir(cotacao, daEmpresa || paraOFornecedor, COTACAO_NAO_ENCONTRADA);
    }

    /** Editar, cancelar e ver as propostas: só a empresa que publicou. */
    @Transactional(readOnly = true)
    public Cotacao cotacaoDaEmpresa(UUID id, UsuarioAutenticado usuario) {
        Cotacao cotacao = cotacao(id);
        return exigir(cotacao, usuario.ehEmpresa() && ehDa(cotacao, usuario), COTACAO_NAO_ENCONTRADA);
    }

    // ---------------------------------------------------------------- proposta

    /** O fornecedor que enviou e a empresa que recebeu. Um concorrente nunca vê o lance do outro. */
    @Transactional(readOnly = true)
    public Proposta propostaVisivel(UUID id, UsuarioAutenticado usuario) {
        Proposta proposta = proposta(id);
        return exigir(proposta, enviadaPor(proposta, usuario) || recebidaPor(proposta, usuario),
                PROPOSTA_NAO_ENCONTRADA);
    }

    /** Recusar ou abrir negociação: só a empresa dona da cotação. */
    @Transactional(readOnly = true)
    public Proposta propostaRecebida(UUID id, UsuarioAutenticado usuario) {
        Proposta proposta = proposta(id);
        return exigir(proposta, recebidaPor(proposta, usuario), PROPOSTA_NAO_ENCONTRADA);
    }

    /** Retirar: só o fornecedor que enviou. */
    @Transactional(readOnly = true)
    public Proposta propostaEnviada(UUID id, UsuarioAutenticado usuario) {
        Proposta proposta = proposta(id);
        return exigir(proposta, enviadaPor(proposta, usuario), PROPOSTA_NAO_ENCONTRADA);
    }

    // ---------------------------------------------------------------- negociação

    /** As duas organizações da negociação, e só elas. */
    @Transactional(readOnly = true)
    public Negociacao negociacaoVisivel(UUID id, UsuarioAutenticado usuario) {
        Negociacao negociacao = negociacao(id);
        return exigir(negociacao, participa(negociacao, usuario), NEGOCIACAO_NAO_ENCONTRADA);
    }

    /**
     * Fechar ou encerrar é decisão da empresa. O fornecedor da negociação a enxerga,
     * então recebe 403 (e não 404): não há o que esconder dele.
     */
    @Transactional(readOnly = true)
    public Negociacao negociacaoDaEmpresa(UUID id, UsuarioAutenticado usuario) {
        Negociacao negociacao = negociacaoVisivel(id, usuario);
        if (!usuario.ehEmpresa()) {
            throw new AcessoNegadoException("Só a empresa desta negociação pode fechá-la ou encerrá-la.");
        }
        return negociacao;
    }

    // ---------------------------------------------------------------- regras

    private static boolean ehDa(Cotacao cotacao, UsuarioAutenticado usuario) {
        return cotacao.getEmpresa().getId().equals(usuario.organizacaoId());
    }

    private static boolean enviadaPor(Proposta proposta, UsuarioAutenticado usuario) {
        return usuario.ehFornecedor() && proposta.getFornecedor().getId().equals(usuario.organizacaoId());
    }

    private static boolean recebidaPor(Proposta proposta, UsuarioAutenticado usuario) {
        return usuario.ehEmpresa() && ehDa(proposta.getCotacao(), usuario);
    }

    private static boolean participa(Negociacao negociacao, UsuarioAutenticado usuario) {
        UUID lado = usuario.ehEmpresa() ? negociacao.getEmpresa().getId() : negociacao.getFornecedor().getId();
        return lado.equals(usuario.organizacaoId());
    }

    private static <T> T exigir(T recurso, boolean permitido, String comoSeNaoExistisse) {
        if (!permitido) {
            throw new RecursoNaoEncontradoException(comoSeNaoExistisse);
        }
        return recurso;
    }

    private Cotacao cotacao(UUID id) {
        return cotacaoRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(COTACAO_NAO_ENCONTRADA));
    }

    private Proposta proposta(UUID id) {
        return propostaRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(PROPOSTA_NAO_ENCONTRADA));
    }

    private Negociacao negociacao(UUID id) {
        return negociacaoRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NEGOCIACAO_NAO_ENCONTRADA));
    }
}
