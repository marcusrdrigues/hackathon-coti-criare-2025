package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.aplicacao.porta.NegociacaoRepositorio;
import com.gestao.compras.dominio.Negociacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public List<Negociacao> listarDaEmpresa(UUID empresaId) {
        return jpa.daEmpresa(empresaId);
    }

    @Override
    public List<Negociacao> listarDoFornecedor(UUID fornecedorId) {
        return jpa.doFornecedor(fornecedorId);
    }

    @Override
    public long contar() {
        return jpa.count();
    }
}
