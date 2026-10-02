package com.gestao.identidade.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** A empresa compradora ou o fornecedor: quem é parte no negócio. As pessoas entram por {@link Membro}. */
@Getter
@Entity
@Table(name = "tb_organizacao")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoOrganizacao tipo;

    @Column(name = "razao_social", length = 200, nullable = false)
    private String razaoSocial;

    /** Só os 14 dígitos. */
    @Column(name = "cnpj", length = 14, nullable = false)
    private String cnpj;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    public Organizacao(TipoOrganizacao tipo, String razaoSocial, String cnpj) {
        this.tipo = tipo;
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.criadaEm = LocalDateTime.now();
    }

    public boolean ehEmpresa() {
        return tipo == TipoOrganizacao.EMPRESA;
    }
}
