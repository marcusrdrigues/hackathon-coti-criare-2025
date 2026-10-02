package com.gestao.compras.aplicacao;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.compras.aplicacao.porta.CotacaoRepositorio;
import com.gestao.compras.aplicacao.porta.PropostaRepositorio;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.identidade.aplicacao.OrganizacaoService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private final CotacaoRepositorio cotacaoRepositorio;
    private final PropostaRepositorio propostaRepositorio;
    private final OrganizacaoService organizacaoService;

    /** Publica em nome da empresa de quem está logado, registrando quem publicou. */
    @Transactional
    public Cotacao criarCotacao(Cotacao cotacao, UsuarioAutenticado autor) {
        validarDataLimite(cotacao.getDataLimite());

        cotacao.setEmpresa(organizacaoService.buscarPorId(autor.organizacaoId()));
        cotacao.setCriadaPor(organizacaoService.buscarUsuario(autor.usuarioId()));
        cotacao.setStatus(StatusCotacao.ABERTA);
        cotacao.setDataCriacao(LocalDateTime.now());
        if (cotacao.getCategoria() == null) {
            cotacao.setCategoria(CategoriaCotacao.OUTROS);
        }

        return cotacaoRepositorio.salvar(cotacao);
    }

    @Transactional(readOnly = true)
    public Cotacao buscarPorId(UUID id) {
        return cotacaoRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cotação não encontrada!"));
    }

    /**
     * Fornecedores enxergam qualquer cotação (é o que aparece no mural);
     * empresas só enxergam as próprias.
     */
    @Transactional(readOnly = true)
    public Cotacao buscarParaUsuario(UUID id, UsuarioAutenticado usuario) {
        Cotacao cotacao = buscarPorId(id);
        if (usuario.ehEmpresa()) {
            verificarDona(cotacao, usuario.organizacaoId());
        }
        return cotacao;
    }

    /** Busca a cotação garantindo que ela pertence à empresa informada. */
    @Transactional(readOnly = true)
    public Cotacao buscarDaEmpresa(UUID id, UUID empresaId) {
        Cotacao cotacao = buscarPorId(id);
        verificarDona(cotacao, empresaId);
        return cotacao;
    }

    @Transactional(readOnly = true)
    public List<Cotacao> listarPorEmpresa(UUID empresaId) {
        return cotacaoRepositorio.listarDaEmpresa(empresaId);
    }

    /** Mural do fornecedor: só cotações abertas e dentro do prazo. */
    @Transactional(readOnly = true)
    public List<Cotacao> listarCotacoesAbertas() {
        return cotacaoRepositorio.listarAbertasVigentes(LocalDateTime.now());
    }

    @Transactional
    public Cotacao atualizarCotacao(UUID id, UUID empresaId, Cotacao cotacaoAtualizada) {
        Cotacao cotacao = buscarDaEmpresa(id, empresaId);

        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new RegraDeNegocioException("Só é possível editar cotações abertas!");
        }
        validarDataLimite(cotacaoAtualizada.getDataLimite());

        cotacao.setNomeServico(cotacaoAtualizada.getNomeServico());
        cotacao.setRequisitos(cotacaoAtualizada.getRequisitos());
        cotacao.setDataLimite(cotacaoAtualizada.getDataLimite());
        cotacao.setOrcamentoEstimado(cotacaoAtualizada.getOrcamentoEstimado());
        if (cotacaoAtualizada.getCategoria() != null) {
            cotacao.setCategoria(cotacaoAtualizada.getCategoria());
        }

        return cotacaoRepositorio.salvar(cotacao);
    }

    /**
     * Cancela uma cotação aberta. Propostas que ainda aguardavam análise
     * passam para RECUSADA, para o fornecedor ver o desfecho no histórico.
     */
    @Transactional
    public Cotacao cancelarCotacao(UUID id, UUID empresaId) {
        Cotacao cotacao = buscarDaEmpresa(id, empresaId);

        if (cotacao.getStatus() == StatusCotacao.EM_NEGOCIACAO) {
            throw new RegraDeNegocioException("Encerre a negociação em andamento antes de cancelar a cotação.");
        }
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new RegraDeNegocioException("Esta cotação já está encerrada.");
        }

        for (Proposta proposta : propostaRepositorio.listarDaCotacao(id)) {
            if (proposta.getStatus() == StatusProposta.ENVIADA || proposta.getStatus() == StatusProposta.EM_ANALISE) {
                proposta.setStatus(StatusProposta.RECUSADA);
            }
        }

        cotacao.setStatus(StatusCotacao.CANCELADA);
        return cotacaoRepositorio.salvar(cotacao);
    }

    private void verificarDona(Cotacao cotacao, UUID empresaId) {
        if (!cotacao.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta cotação pertence a outra empresa.");
        }
    }

    private void validarDataLimite(LocalDateTime dataLimite) {
        if (dataLimite != null && dataLimite.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("A data limite precisa ser uma data futura.");
        }
    }
}
