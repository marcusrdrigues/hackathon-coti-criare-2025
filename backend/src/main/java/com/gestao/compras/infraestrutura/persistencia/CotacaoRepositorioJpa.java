package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compartilhado.infraestrutura.persistencia.PaginasJpa;
import com.gestao.compras.aplicacao.porta.CotacaoRepositorio;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das cotações com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class CotacaoRepositorioJpa implements CotacaoRepositorio {

    /** Campo da API -> propriedade da entidade. Só o que está aqui ordena as listas. */
    private static final Map<String, String> PROPRIEDADES = Map.of(
            "dataCriacao", "dataCriacao",
            "dataLimite", "dataLimite",
            "nomeServico", "nomeServico");

    private final CotacaoJpa jpa;

    @Override
    public Cotacao salvar(Cotacao cotacao) {
        return jpa.save(cotacao);
    }

    @Override
    public Optional<Cotacao> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public long contar() {
        return jpa.count();
    }

    @Override
    public Pagina<Cotacao> buscarDaEmpresa(UUID empresaId, StatusCotacao status, String busca, PedidoDePagina pedido) {
        Specification<Cotacao> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("empresa").get("id"), empresaId));
            if (status != null) {
                condicoes.add(cb.equal(raiz.get("status"), status));
            }
            if (busca != null) {
                String termo = PaginasJpa.contendo(busca);
                condicoes.add(cb.or(
                        cb.like(cb.lower(raiz.<String>get("nomeServico")), termo, PaginasJpa.ESCAPE),
                        cb.like(cb.lower(raiz.<String>get("requisitos")), termo, PaginasJpa.ESCAPE)));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        return PaginasJpa.pagina(jpa.findAll(filtro, PaginasJpa.ordenada(pedido, PROPRIEDADES)), pedido);
    }

    @Override
    public Map<StatusCotacao, Long> contarDaEmpresaPorSituacao(UUID empresaId) {
        Map<StatusCotacao, Long> totais = new EnumMap<>(StatusCotacao.class);
        jpa.contarPorSituacao(empresaId).forEach(c -> totais.put(c.getStatus(), c.getTotal()));
        return totais;
    }

    @Override
    public Pagina<Cotacao> buscarAbertasVigentes(LocalDateTime agora, CategoriaCotacao categoria, String busca,
                                                 PedidoDePagina pedido) {
        Specification<Cotacao> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("status"), StatusCotacao.ABERTA));
            condicoes.add(cb.or(
                    cb.isNull(raiz.get("dataLimite")),
                    cb.greaterThan(raiz.<LocalDateTime>get("dataLimite"), agora)));
            if (categoria != null) {
                condicoes.add(cb.equal(raiz.get("categoria"), categoria));
            }
            if (busca != null) {
                String termo = PaginasJpa.contendo(busca);
                condicoes.add(cb.or(
                        cb.like(cb.lower(raiz.<String>get("nomeServico")), termo, PaginasJpa.ESCAPE),
                        cb.like(cb.lower(raiz.<String>get("requisitos")), termo, PaginasJpa.ESCAPE),
                        cb.like(cb.lower(raiz.get("empresa").<String>get("razaoSocial")), termo, PaginasJpa.ESCAPE)));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        return PaginasJpa.pagina(jpa.findAll(filtro, PaginasJpa.ordenada(pedido, PROPRIEDADES)), pedido);
    }
}
