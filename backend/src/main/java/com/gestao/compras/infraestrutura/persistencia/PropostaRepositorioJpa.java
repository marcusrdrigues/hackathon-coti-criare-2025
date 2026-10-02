package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compartilhado.infraestrutura.persistencia.PaginasJpa;
import com.gestao.compras.aplicacao.SituacaoProposta;
import com.gestao.compras.aplicacao.porta.PropostaRepositorio;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das propostas com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class PropostaRepositorioJpa implements PropostaRepositorio {

    private final PropostaJpa jpa;

    @Override
    public Proposta salvar(Proposta proposta) {
        return jpa.save(proposta);
    }

    @Override
    public Optional<Proposta> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Proposta> listarDaCotacao(UUID cotacaoId) {
        return jpa.daCotacao(cotacaoId);
    }

    @Override
    public boolean existeDoFornecedorNaCotacao(UUID fornecedorId, UUID cotacaoId) {
        return jpa.existsByFornecedorIdAndCotacaoId(fornecedorId, cotacaoId);
    }

    @Override
    public void excluir(Proposta proposta) {
        jpa.delete(proposta);
    }

    @Override
    public Pagina<Proposta> buscarDoFornecedor(UUID fornecedorId, SituacaoProposta situacao, PedidoDePagina pedido) {
        Specification<Proposta> filtro = (raiz, consulta, cb) -> {
            Join<Proposta, Negociacao> negociacao = raiz.join("negociacao", JoinType.LEFT);
            Predicate doFornecedor = cb.equal(raiz.get("fornecedor").get("id"), fornecedorId);
            Predicate naSituacao = situacao == SituacaoProposta.ANDAMENTO
                    // Negociação em andamento, ou ainda sem negociação e esperando a empresa
                    ? cb.or(cb.equal(negociacao.get("status"), StatusNegociacao.EM_ANDAMENTO),
                            cb.and(cb.isNull(negociacao.get("id")),
                                    raiz.get("status").in(StatusProposta.ENVIADA, StatusProposta.EM_ANALISE)))
                    // Negócio fechado, ou proposta recusada (pela empresa, por cancelamento ou por outra escolha)
                    : cb.or(cb.equal(negociacao.get("status"), StatusNegociacao.FINALIZADA),
                            cb.equal(raiz.get("status"), StatusProposta.RECUSADA));
            // A ordem vai na consulta dos itens; a de contagem não aceita ordenação
            if (consulta != null && !Long.class.equals(consulta.getResultType())) {
                Expression<Integer> ativaPrimeiro = cb.<Integer>selectCase()
                        .when(cb.equal(negociacao.get("status"), StatusNegociacao.EM_ANDAMENTO), 0)
                        .otherwise(1);
                consulta.orderBy(cb.asc(ativaPrimeiro), cb.desc(raiz.get("dataEnvio")), cb.asc(raiz.get("id")));
            }
            return cb.and(doFornecedor, naSituacao);
        };
        return PaginasJpa.pagina(jpa.findAll(filtro, PaginasJpa.naOrdemDaConsulta(pedido)), pedido);
    }
}
