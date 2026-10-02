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

import java.time.LocalDateTime;
import java.util.UUID;

/** A pessoa que entra no sistema. Pertence a uma organização por meio de {@link Membro}. */
@Getter
@Entity
@Table(name = "tb_usuario")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", length = 150, nullable = false)
    private String nome;

    /** Normalizado (minúsculas, sem espaços) e único na plataforma. */
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    /** Hash BCrypt da senha. */
    @Column(name = "senha_hash", length = 100, nullable = false)
    private String senhaHash;

    /** Perfil da plataforma (não de uma organização). Só nasce pela configuração do servidor. */
    @Column(name = "superadmin", nullable = false)
    private boolean superadmin;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "desativado_em")
    private LocalDateTime desativadoEm;

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.criadoEm = LocalDateTime.now();
    }

    public boolean ativo() {
        return desativadoEm == null;
    }
}
