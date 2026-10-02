package com.gestao.identidade.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma pessoa numa organização, com um papel. Quem sai não é apagado
 * ({@code removidoEm}): o que essa pessoa fez continua com o nome dela.
 */
@Getter
@Entity
@Table(name = "tb_membro")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", length = 20, nullable = false)
    private Papel papel;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "removido_em")
    private LocalDateTime removidoEm;

    public Membro(Usuario usuario, Organizacao organizacao, Papel papel) {
        this.usuario = usuario;
        this.organizacao = organizacao;
        this.papel = papel;
        this.criadoEm = LocalDateTime.now();
    }

    public boolean ativo() {
        return removidoEm == null && usuario.ativo();
    }

    /** Sai da organização. O registro fica, para o histórico continuar com o nome da pessoa. */
    public void remover(LocalDateTime agora) {
        this.removidoEm = agora;
    }
}
