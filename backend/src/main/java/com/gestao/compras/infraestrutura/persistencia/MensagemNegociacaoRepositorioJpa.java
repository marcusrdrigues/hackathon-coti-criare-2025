package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.aplicacao.porta.MensagemNegociacaoRepositorio;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.TipoRemetente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das mensagens com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class MensagemNegociacaoRepositorioJpa implements MensagemNegociacaoRepositorio {

    private final MensagemNegociacaoJpa jpa;

    @Override
    public MensagemNegociacao salvar(MensagemNegociacao mensagem) {
        return jpa.save(mensagem);
    }

    @Override
    public Optional<MensagemNegociacao> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<MensagemNegociacao> listarDaNegociacao(UUID negociacaoId) {
        return jpa.daNegociacao(negociacaoId);
    }

    @Override
    public long contarDaOutraParte(UUID negociacaoId, TipoRemetente leitor) {
        return jpa.countByNegociacaoIdAndTipoRemetenteNot(negociacaoId, leitor);
    }

    @Override
    public long contarDaOutraParteDepoisDe(UUID negociacaoId, TipoRemetente leitor, LocalDateTime lidaEm) {
        return jpa.countByNegociacaoIdAndTipoRemetenteNotAndDataEnvioAfter(negociacaoId, leitor, lidaEm);
    }

    @Override
    public List<NaoLidas> contarNaoLidasDaEmpresa(UUID empresaId, TipoRemetente remetente) {
        return jpa.naoLidasDaEmpresa(empresaId, remetente);
    }

    @Override
    public List<NaoLidas> contarNaoLidasDoFornecedor(UUID fornecedorId, TipoRemetente remetente) {
        return jpa.naoLidasDoFornecedor(fornecedorId, remetente);
    }
}
