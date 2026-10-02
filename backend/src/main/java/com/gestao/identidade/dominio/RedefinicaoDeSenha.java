package com.gestao.identidade.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Um link de redefinição de senha (spec 004). O banco guarda só o hash do token. Vale até
 * vencer, ser usado ou ser substituído por um pedido mais novo, o que vier primeiro.
 * Fica fora do histórico de alterações: é uma credencial temporária.
 */
@Getter
@Entity
@Table(name = "tb_redefinicao_senha")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RedefinicaoDeSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "usada_em")
    private Instant usadaEm;

    @Column(name = "substituida_em")
    private Instant substituidaEm;

    public RedefinicaoDeSenha(UUID usuarioId, String tokenHash, Instant criadaEm, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    /** Ainda serve para trocar a senha: nem vencido, nem usado, nem substituído. */
    public boolean valeEm(Instant agora) {
        return usadaEm == null && substituidaEm == null && agora.isBefore(expiraEm);
    }
}
