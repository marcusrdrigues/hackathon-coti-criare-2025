package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.aplicacao.porta.PropostaRepositorio;
import com.gestao.compras.dominio.Proposta;
import lombok.RequiredArgsConstructor;
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
    public List<Proposta> listarDoFornecedor(UUID fornecedorId) {
        return jpa.doFornecedor(fornecedorId);
    }

    @Override
    public boolean existeDoFornecedorNaCotacao(UUID fornecedorId, UUID cotacaoId) {
        return jpa.existsByFornecedorIdAndCotacaoId(fornecedorId, cotacaoId);
    }

    @Override
    public void excluir(Proposta proposta) {
        jpa.delete(proposta);
    }
}
