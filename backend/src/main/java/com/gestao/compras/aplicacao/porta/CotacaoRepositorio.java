package com.gestao.compras.aplicacao.porta;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das cotações. As listas são paginadas (ver docs/adr/0020). */
public interface CotacaoRepositorio {

    Cotacao salvar(Cotacao cotacao);

    Optional<Cotacao> buscarPorId(UUID id);

    long contar();

    /**
     * Uma página das cotações da empresa.
     *
     * @param status opcional: só as desta situação
     * @param busca  opcional: texto no título ou nos requisitos
     */
    Pagina<Cotacao> buscarDaEmpresa(UUID empresaId, StatusCotacao status, String busca, PedidoDePagina pedido);

    /** Quantas cotações a empresa tem em cada situação (as que não têm nenhuma ficam de fora). */
    Map<StatusCotacao, Long> contarDaEmpresaPorSituacao(UUID empresaId);

    /**
     * Uma página do mural: abertas e dentro do prazo no momento informado.
     *
     * @param categoria opcional
     * @param busca     opcional: texto no título, nos requisitos ou no nome da empresa
     */
    Pagina<Cotacao> buscarAbertasVigentes(LocalDateTime agora, CategoriaCotacao categoria, String busca,
                                          PedidoDePagina pedido);
}
