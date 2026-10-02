package com.gestao.compras.aplicacao.porta;

import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.TipoRemetente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das mensagens de negociação. */
public interface MensagemNegociacaoRepositorio {

    MensagemNegociacao salvar(MensagemNegociacao mensagem);

    Optional<MensagemNegociacao> buscarPorId(UUID id);

    /** Histórico em ordem cronológica. */
    List<MensagemNegociacao> listarDaNegociacao(UUID negociacaoId);

    /** Mensagens que não foram enviadas por quem lê (quem ainda não leu nada). */
    long contarDaOutraParte(UUID negociacaoId, TipoRemetente leitor);

    /** Mensagens da outra parte enviadas depois da última leitura. */
    long contarDaOutraParteDepoisDe(UUID negociacaoId, TipoRemetente leitor, LocalDateTime lidaEm);

    /** Mensagens do remetente que a empresa ainda não viu, agrupadas por negociação. */
    List<NaoLidas> contarNaoLidasDaEmpresa(UUID empresaId, TipoRemetente remetente);

    /** Mensagens do remetente que o fornecedor ainda não viu, agrupadas por negociação. */
    List<NaoLidas> contarNaoLidasDoFornecedor(UUID fornecedorId, TipoRemetente remetente);

    /** Quantas mensagens não lidas há em cada negociação. */
    interface NaoLidas {
        UUID getNegociacaoId();

        Long getTotal();
    }
}
