package com.gestao.identidade.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Convite para a equipe de uma organização. O link carrega um token aleatório; aqui
 * fica só o hash dele. Vale por {@link #VALIDADE} e uma vez só.
 */
@Getter
@Entity
@Table(name = "tb_convite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Convite {

    public static final Duration VALIDADE = Duration.ofHours(72);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    /** Como quem convidou chamou a pessoa; ela pode corrigir ao aceitar. */
    @Column(name = "nome", length = 150, nullable = false)
    private String nome;

    /** Normalizado. Vira o e-mail de acesso de quem aceitar. */
    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "convidado_por", nullable = false)
    private Usuario convidadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "aceito_em")
    private Instant aceitoEm;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    public Convite(Organizacao organizacao, String nome, String email, String tokenHash, Usuario convidadoPor,
                   Instant agora) {
        this.organizacao = organizacao;
        this.nome = nome;
        this.email = email;
        this.tokenHash = tokenHash;
        this.convidadoPor = convidadoPor;
        this.criadoEm = agora;
        this.expiraEm = agora.plus(VALIDADE);
    }

    public Situacao situacao(Instant agora) {
        if (aceitoEm != null) {
            return Situacao.ACEITO;
        }
        if (canceladoEm != null) {
            return Situacao.CANCELADO;
        }
        return expiraEm.isAfter(agora) ? Situacao.PENDENTE : Situacao.VENCIDO;
    }

    public void aceitar(Instant agora) {
        exigirPendente(agora);
        this.aceitoEm = agora;
    }

    public void cancelar(Instant agora) {
        exigirPendente(agora);
        this.canceladoEm = agora;
    }

    private void exigirPendente(Instant agora) {
        if (situacao(agora) != Situacao.PENDENTE) {
            throw new IllegalStateException("O convite não está mais pendente.");
        }
    }

    public enum Situacao { PENDENTE, ACEITO, CANCELADO, VENCIDO }
}
