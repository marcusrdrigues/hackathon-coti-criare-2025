package com.gestao.auditoria.infraestrutura.persistencia;

import com.gestao.auditoria.dominio.OrigemDaRevisao;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Uma revisão do histórico: tudo o que uma transação mudou nos dados auditados, com quem
 * mudou, de qual organização, quando e com qual identificador de rastreio (ver docs/adr/0021).
 */
@Getter
@Entity
@Table(name = "tb_revisao")
@RevisionEntity(AutorDaRevisao.class)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Revisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "id")
    private long id;

    /** Milissegundos desde 1970 (UTC). */
    @RevisionTimestamp
    @Column(name = "momento", nullable = false)
    private long momento;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "organizacao_id")
    private UUID organizacaoId;

    @Column(name = "trace_id", length = 32)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", length = 10, nullable = false)
    private OrigemDaRevisao origem;

    /** Nomes das entidades que mudaram nesta revisão. */
    @ModifiedEntityNames
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_revisao_entidade", joinColumns = @JoinColumn(name = "revisao_id"))
    @Column(name = "entidade", length = 255)
    private Set<String> entidades = new HashSet<>();

    void preencher(UUID usuarioId, UUID organizacaoId, String traceId, OrigemDaRevisao origem) {
        this.usuarioId = usuarioId;
        this.organizacaoId = organizacaoId;
        this.traceId = traceId;
        this.origem = origem;
    }
}
