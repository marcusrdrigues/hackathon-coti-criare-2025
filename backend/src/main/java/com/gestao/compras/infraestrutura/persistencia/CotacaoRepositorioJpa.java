package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.aplicacao.porta.CotacaoRepositorio;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das cotações com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class CotacaoRepositorioJpa implements CotacaoRepositorio {

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
    public List<Cotacao> listarDaEmpresa(UUID empresaId) {
        return jpa.daEmpresa(empresaId);
    }

    @Override
    public List<Cotacao> listarDaEmpresaPorStatus(UUID empresaId, StatusCotacao status) {
        return jpa.daEmpresaPorStatus(empresaId, status);
    }

    @Override
    public List<Cotacao> listarPorStatus(StatusCotacao status) {
        return jpa.porStatus(status);
    }

    @Override
    public List<Cotacao> listarAbertasVigentes(LocalDateTime agora) {
        return jpa.vigentesPorStatus(StatusCotacao.ABERTA, agora);
    }

    @Override
    public void excluir(Cotacao cotacao) {
        jpa.delete(cotacao);
    }

    @Override
    public long contar() {
        return jpa.count();
    }
}
