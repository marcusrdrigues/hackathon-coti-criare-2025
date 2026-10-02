package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compartilhado.infraestrutura.persistencia.PaginasJpa;
import com.gestao.compras.aplicacao.SituacaoNegociacao;
import com.gestao.compras.aplicacao.porta.NegociacaoRepositorio;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.StatusNegociacao;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das negociações com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class NegociacaoRepositorioJpa implements NegociacaoRepositorio {

    private final NegociacaoJpa jpa;

    @Override
    public Negociacao salvar(Negociacao negociacao) {
        return jpa.save(negociacao);
    }

    @Override
    public Optional<Negociacao> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existeParaProposta(UUID propostaId) {
        return jpa.existsByPropostaId(propostaId);
    }

    @Override
    public long contar() {
        return jpa.count();
    }

    @Override
    public Pagina<Negociacao> buscarDaOrganizacao(UUID organizacaoId, boolean comoEmpresa, SituacaoNegociacao situacao,
                                                  PedidoDePagina pedido) {
        Specification<Negociacao> filtro = (raiz, consulta, cb) -> {
            Predicate participa = cb.equal(raiz.get(comoEmpresa ? "empresa" : "fornecedor").get("id"), organizacaoId);
            Predicate naSituacao = situacao == SituacaoNegociacao.ANDAMENTO
                    ? cb.equal(raiz.get("status"), StatusNegociacao.EM_ANDAMENTO)
                    : cb.conjunction();
            // A ordem vai na consulta dos itens; a de contagem não aceita ordenação
            if (consulta != null && !Long.class.equals(consulta.getResultType())) {
                Expression<Integer> emAndamentoPrimeiro = cb.<Integer>selectCase()
                        .when(cb.equal(raiz.get("status"), StatusNegociacao.EM_ANDAMENTO), 0)
                        .otherwise(1);
                consulta.orderBy(cb.asc(emAndamentoPrimeiro), cb.desc(raiz.get("dataInicio")), cb.asc(raiz.get("id")));
            }
            return cb.and(participa, naSituacao);
        };
        return PaginasJpa.pagina(jpa.findAll(filtro, PaginasJpa.naOrdemDaConsulta(pedido)), pedido);
    }
}
