package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.identidade.aplicacao.porta.ConviteRepositorio;
import com.gestao.identidade.aplicacao.porta.MembroRepositorio;
import com.gestao.identidade.aplicacao.porta.OrganizacaoRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Convite;
import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.MembroRemovidoEvento;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A equipe de uma organização (spec 001, R2). Todos da organização veem quem faz parte dela;
 * convidar, cancelar convite e remover são do proprietário.
 */
@Service
@RequiredArgsConstructor
public class EquipeService {

    static final String SO_PROPRIETARIO = "Só o proprietário da organização gerencia a equipe.";

    private final MembroRepositorio membroRepositorio;
    private final ConviteRepositorio conviteRepositorio;
    private final OrganizacaoRepositorio organizacaoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final CredenciaisService credenciais;
    private final RefreshTokenService sessoes;
    private final ApplicationEventPublisher eventos;

    /** O convite e o token do link, que só existe aqui: o banco guarda o hash. */
    public record ConviteCriado(Convite convite, String token) {}

    @Transactional(readOnly = true)
    public List<Membro> listarMembros(UsuarioAutenticado usuario) {
        return membroRepositorio.listarAtivosDaOrganizacao(usuario.organizacaoId());
    }

    @Transactional(readOnly = true)
    public List<Convite> listarConvites(UsuarioAutenticado usuario) {
        exigirProprietario(usuario);
        return conviteRepositorio.listarPendentes(usuario.organizacaoId(), Instant.now());
    }

    /**
     * Gera o link de convite. O e-mail não pode ter conta na plataforma. Um convite novo para
     * o mesmo e-mail cancela o anterior: só o link mais recente vale.
     */
    @Transactional
    public ConviteCriado convidar(UsuarioAutenticado usuario, String nome, String email) {
        exigirProprietario(usuario);
        String emailNormalizado = Documentos.normalizarEmail(email);
        credenciais.validarEmailDisponivel(emailNormalizado);

        Instant agora = Instant.now();
        conviteRepositorio.listarPendentes(usuario.organizacaoId(), agora).stream()
                .filter(anterior -> anterior.getEmail().equals(emailNormalizado))
                .forEach(anterior -> anterior.cancelar(agora));

        String token = TokenAleatorio.gerar();
        Convite convite = conviteRepositorio.salvar(new Convite(
                organizacaoRepositorio.buscarPorId(usuario.organizacaoId()).orElseThrow(),
                nome.trim(), emailNormalizado, TokenAleatorio.hash(token),
                usuarioRepositorio.buscarPorId(usuario.usuarioId()).orElseThrow(), agora));
        return new ConviteCriado(convite, token);
    }

    @Transactional
    public void cancelarConvite(UsuarioAutenticado usuario, UUID conviteId) {
        exigirProprietario(usuario);
        Instant agora = Instant.now();
        Convite convite = conviteRepositorio.buscarPorId(conviteId)
                .filter(c -> c.getOrganizacao().getId().equals(usuario.organizacaoId()))
                .filter(c -> c.situacao(agora) == Convite.Situacao.PENDENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado!"));
        convite.cancelar(agora);
    }

    /**
     * Tira a pessoa da organização: as sessões dela deixam de valer na hora e a conexão em tempo
     * real é encerrada. O que ela fez continua registrado com o nome dela.
     */
    @Transactional
    public void removerMembro(UsuarioAutenticado usuario, UUID membroId) {
        exigirProprietario(usuario);
        Membro membro = membroRepositorio.buscarPorId(membroId)
                .filter(m -> m.getRemovidoEm() == null)
                .filter(m -> m.getOrganizacao().getId().equals(usuario.organizacaoId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado!"));

        // Só o proprietário remove e ninguém remove a si mesmo: a organização nunca fica sem proprietário
        UUID pessoa = membro.getUsuario().getId();
        if (pessoa.equals(usuario.usuarioId())) {
            throw new RegraDeNegocioException("Você não pode remover a si mesmo da equipe.");
        }

        membro.remover(LocalDateTime.now());
        sessoes.revogarTodas(pessoa);
        eventos.publishEvent(new MembroRemovidoEvento(pessoa));
    }

    private static void exigirProprietario(UsuarioAutenticado usuario) {
        if (!usuario.ehProprietario()) {
            throw new AcessoNegadoException(SO_PROPRIETARIO);
        }
    }
}
