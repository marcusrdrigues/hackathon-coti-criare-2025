package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.EventoDeSeguranca.Tipo;
import com.gestao.compartilhado.dominio.Mascaras;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.MembroRepositorio;
import com.gestao.identidade.aplicacao.porta.RedefinicaoDeSenhaRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.RedefinicaoDeSenha;
import com.gestao.identidade.dominio.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Esqueci minha senha" (spec 004). O pedido nunca revela se o e-mail tem conta: a resposta
 * é a mesma, e o e-mail sai depois, fora da requisição. O link vale 30 minutos e uma vez só,
 * e a troca encerra todas as sessões da pessoa.
 */
@Slf4j
@Service
public class RedefinicaoDeSenhaService {

    static final Duration VALIDADE = Duration.ofMinutes(30);
    static final String LINK_INVALIDO = "Este link não vale mais. Peça um link novo.";

    private final RedefinicaoDeSenhaRepositorio repositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final MembroRepositorio membroRepositorio;
    private final CredenciaisService credenciais;
    private final RefreshTokenService sessoes;
    private final TentativasLoginService tentativas;
    private final LimiteDePedidosDeRedefinicao limite;
    private final EventosDaIdentidade eventosDeSeguranca;
    private final ApplicationEventPublisher eventos;
    private final Set<String> dominiosSemEnvio;

    @SuppressWarnings("java:S107") // as dependências de um caso de uso que toca sessão, senha e e-mail
    public RedefinicaoDeSenhaService(RedefinicaoDeSenhaRepositorio repositorio,
                                     UsuarioRepositorio usuarioRepositorio,
                                     MembroRepositorio membroRepositorio,
                                     CredenciaisService credenciais,
                                     RefreshTokenService sessoes,
                                     TentativasLoginService tentativas,
                                     LimiteDePedidosDeRedefinicao limite,
                                     EventosDaIdentidade eventosDeSeguranca,
                                     ApplicationEventPublisher eventos,
                                     @Value("${app.redefinicao.dominios-sem-envio:demo.com}") String dominiosSemEnvio) {
        this.repositorio = repositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.membroRepositorio = membroRepositorio;
        this.credenciais = credenciais;
        this.sessoes = sessoes;
        this.tentativas = tentativas;
        this.limite = limite;
        this.eventosDeSeguranca = eventosDeSeguranca;
        this.eventos = eventos;
        this.dominiosSemEnvio = Arrays.stream(dominiosSemEnvio.split(","))
                .map(String::trim).map(String::toLowerCase).filter(d -> !d.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Cria o link e manda o e-mail, se a conta recebe: ativa, numa organização, fora das contas
     * de exemplo e dentro do limite por hora. Em qualquer outro caso, não faz nada.
     */
    @Transactional
    public void pedir(String email) {
        String normalizado = Documentos.normalizarEmail(email);
        Optional<Usuario> conta = normalizado == null ? Optional.empty()
                : usuarioRepositorio.buscarPorEmail(normalizado).filter(this::recebeOLink);
        if (conta.isEmpty()) {
            return;
        }
        if (!limite.permitir(normalizado)) {
            log.warn("Pedidos de redefinição de senha acima do limite para {}.", Mascaras.email(normalizado));
            return;
        }
        Usuario usuario = conta.get();
        Instant agora = Instant.now();
        repositorio.substituirAbertos(usuario.getId(), agora);
        String token = TokenAleatorio.gerar();
        repositorio.salvar(new RedefinicaoDeSenha(usuario.getId(), TokenAleatorio.hash(token), agora,
                agora.plus(VALIDADE)));
        eventosDeSeguranca.publicar(Tipo.REDEFINICAO_DE_SENHA_PEDIDA, usuario, null);
        eventos.publishEvent(new LinkDeRedefinicaoCriado(usuario.getEmail(), usuario.getNome(), token, VALIDADE));
    }

    /** Confere o link antes de mostrar a tela da senha nova. */
    @Transactional(readOnly = true)
    public void consultar(String token) {
        aberto(token, Instant.now());
    }

    /**
     * Troca a senha, gasta o link, encerra todas as sessões e zera o bloqueio de login. O link
     * é marcado como usado antes de tudo: dois envios ao mesmo tempo não trocam a senha duas vezes.
     */
    @Transactional
    public void confirmar(String token, String senha) {
        Instant agora = Instant.now();
        RedefinicaoDeSenha link = aberto(token, agora);
        if (!repositorio.usar(link.getId(), agora)) {
            throw new RecursoNaoEncontradoException(LINK_INVALIDO);
        }
        Usuario usuario = usuarioRepositorio.buscarPorId(link.getUsuarioId())
                .filter(this::recebeOLink)
                .orElseThrow(() -> new RecursoNaoEncontradoException(LINK_INVALIDO));
        usuario.trocarSenha(credenciais.gerarHash(senha));
        sessoes.revogarTodas(usuario.getId());
        tentativas.limpar(usuario.getEmail());
        eventosDeSeguranca.publicar(Tipo.SENHA_REDEFINIDA, usuario, null);
    }

    /** Limpeza diária dos links vencidos há mais de um dia. */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void apagarVencidos() {
        int apagados = repositorio.apagarVencidosAntesDe(Instant.now().minus(Duration.ofDays(1)));
        if (apagados > 0) {
            log.info("{} links de redefinição de senha vencidos removidos.", apagados);
        }
    }

    /** Um motivo só para qualquer link que não serve: vencido, usado, substituído ou inventado. */
    private RedefinicaoDeSenha aberto(String token, Instant agora) {
        if (token == null || token.isBlank()) {
            throw new RecursoNaoEncontradoException(LINK_INVALIDO);
        }
        return repositorio.buscarPorHash(TokenAleatorio.hash(token.trim()))
                .filter(link -> link.valeEm(agora))
                .orElseThrow(() -> new RecursoNaoEncontradoException(LINK_INVALIDO));
    }

    /**
     * O superadmin fica de fora (a senha dele vem da configuração), e as contas de exemplo
     * também: os e-mails delas não existem.
     */
    private boolean recebeOLink(Usuario usuario) {
        return usuario.ativo()
                && !usuario.isSuperadmin()
                && !dominiosSemEnvio.contains(dominio(usuario.getEmail()))
                && membroRepositorio.buscarAtivoDoUsuario(usuario.getId()).filter(Membro::ativo).isPresent();
    }

    private static String dominio(String email) {
        int arroba = email.lastIndexOf('@');
        return arroba < 0 ? "" : email.substring(arroba + 1).toLowerCase();
    }
}
