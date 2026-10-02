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
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.LocalDateTime;
import java.util.UUID;

/** A pessoa que entra no sistema. Pertence a uma organização por meio de {@link Membro}. */
@Getter
@Audited
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
    @NotAudited
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

    /** O superadmin da plataforma. Só a configuração do servidor cria um (nenhuma rota faz isso). */
    public static Usuario superadmin(String nome, String email, String senhaHash) {
        Usuario usuario = new Usuario(nome, email, senhaHash);
        usuario.superadmin = true;
        return usuario;
    }

    public boolean ativo() {
        return desativadoEm == null;
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    public void desativar() {
        if (desativadoEm == null) {
            desativadoEm = LocalDateTime.now();
        }
    }

    public void reativar() {
        desativadoEm = null;
    }
}
