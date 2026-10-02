package com.gestao.compras.aplicacao;

import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.compras.aplicacao.porta.PropostaRepositorio;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.Proposta;
import com.gestao.compras.dominio.PropostaRecebidaEvento;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.identidade.aplicacao.OrganizacaoService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropostaService {

    private final PropostaRepositorio propostaRepositorio;
    private final OrganizacaoService organizacaoService;
    private final AcessoCompras acesso;
    private final ApplicationEventPublisher eventos;

    /** Envia em nome do fornecedor de quem está logado, registrando quem enviou. */
    @Transactional
    public Proposta criarProposta(Proposta proposta, UsuarioAutenticado autor, UUID cotacaoId) {
        UUID fornecedorId = autor.organizacaoId();
        Cotacao cotacao = acesso.cotacaoVisivel(cotacaoId, autor);

        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new RegraDeNegocioException("Cotação não está mais aberta para propostas!");
        }
        if (cotacao.isPrazoEncerrado()) {
            throw new RegraDeNegocioException("O prazo para envio de propostas desta cotação já terminou!");
        }
        if (propostaRepositorio.existeDoFornecedorNaCotacao(fornecedorId, cotacaoId)) {
            throw new RegraDeNegocioException("Você já enviou uma proposta para esta cotação!");
        }

        proposta.setFornecedor(organizacaoService.buscarPorId(fornecedorId));
        proposta.setEnviadaPor(organizacaoService.buscarUsuario(autor.usuarioId()));
        proposta.setCotacao(cotacao);
        proposta.setStatus(StatusProposta.ENVIADA);
        proposta.setDataEnvio(LocalDateTime.now());

        Proposta salva = propostaRepositorio.salvar(proposta);
        eventos.publishEvent(new PropostaRecebidaEvento(salva.getId()));
        return salva;
    }

    /** Sem checar quem pede: para uso interno do sistema. Quem atende um usuário passa pelo {@link AcessoCompras}. */
    @Transactional(readOnly = true)
    public Proposta buscarPorId(UUID id) {
        return propostaRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(AcessoCompras.PROPOSTA_NAO_ENCONTRADA));
    }

    /** Só o fornecedor que enviou e a empresa dona da cotação podem ver a proposta. */
    @Transactional(readOnly = true)
    public Proposta buscarParaUsuario(UUID id, UsuarioAutenticado usuario) {
        return acesso.propostaVisivel(id, usuario);
    }

    /**
     * Lista as propostas de uma cotação. Só a empresa dona pode ver: um
     * fornecedor não pode descobrir o lance dos concorrentes.
     */
    @Transactional(readOnly = true)
    public List<Proposta> listarPorCotacao(UUID cotacaoId, UsuarioAutenticado usuario) {
        acesso.cotacaoDaEmpresa(cotacaoId, usuario);
        return propostaRepositorio.listarDaCotacao(cotacaoId);
    }

    @Transactional(readOnly = true)
    public List<Proposta> listarPorFornecedor(UUID fornecedorId) {
        return propostaRepositorio.listarDoFornecedor(fornecedorId);
    }

    /**
     * Aceitar uma proposta significa escolher o fornecedor com quem negociar:
     * a cotação vai para EM_NEGOCIACAO e não aceita outra negociação em paralelo.
     * Usado pelo NegociacaoService, que já confere se a empresa é a dona.
     */
    @Transactional
    public Proposta aceitarProposta(UUID id) {
        Proposta proposta = buscarPorId(id);

        if (proposta.getStatus() != StatusProposta.ENVIADA && proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new RegraDeNegocioException("Esta proposta não pode ser aceita!");
        }

        Cotacao cotacao = proposta.getCotacao();
        if (cotacao.getStatus() == StatusCotacao.EM_NEGOCIACAO) {
            throw new RegraDeNegocioException("Já existe uma negociação em andamento para esta cotação!");
        }
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new RegraDeNegocioException("Esta cotação já está encerrada!");
        }

        proposta.setStatus(StatusProposta.ACEITA);
        cotacao.setStatus(StatusCotacao.EM_NEGOCIACAO);

        return propostaRepositorio.salvar(proposta);
    }

    @Transactional
    public Proposta recusarProposta(UUID id, UsuarioAutenticado usuario) {
        Proposta proposta = acesso.propostaRecebida(id, usuario);

        if (proposta.getStatus() != StatusProposta.ENVIADA && proposta.getStatus() != StatusProposta.EM_ANALISE) {
            throw new RegraDeNegocioException("Só é possível recusar propostas que ainda aguardam análise!");
        }

        proposta.setStatus(StatusProposta.RECUSADA);
        return propostaRepositorio.salvar(proposta);
    }

    /** O fornecedor pode retirar a proposta enquanto ela não foi aceita. */
    @Transactional
    public void deletarProposta(UUID id, UsuarioAutenticado usuario) {
        Proposta proposta = acesso.propostaEnviada(id, usuario);

        if (proposta.getStatus() == StatusProposta.ACEITA) {
            throw new RegraDeNegocioException("Propostas aceitas não podem ser excluídas!");
        }
        propostaRepositorio.excluir(proposta);
    }
}
