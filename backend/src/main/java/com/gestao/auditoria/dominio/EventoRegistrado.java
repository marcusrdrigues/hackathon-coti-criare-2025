package com.gestao.auditoria.dominio;

import com.gestao.compartilhado.dominio.EventoDeSeguranca;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Um evento de segurança gravado (spec 003, R2). Só de acréscimo: não tem método que altere
 * nada depois de criado. O e-mail chega mascarado e nenhuma credencial entra aqui.
 */
@Getter
@Entity
@Table(name = "tb_evento_seguranca")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoRegistrado {

    static final int TAMANHO_EMAIL = 100;
    static final int TAMANHO_DETALHE = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 40, nullable = false, updatable = false)
    private EventoDeSeguranca.Tipo tipo;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "organizacao_id", updatable = false)
    private UUID organizacaoId;

    @Column(name = "email_mascarado", length = TAMANHO_EMAIL, updatable = false)
    private String emailMascarado;

    @Column(name = "detalhe", length = TAMANHO_DETALHE, updatable = false)
    private String detalhe;

    @Column(name = "trace_id", length = 32, updatable = false)
    private String traceId;

    /**
     * @param usuarioId     quem agiu (do evento ou, se ele não trouxer, da requisição)
     * @param organizacaoId a organização dessa pessoa
     */
    public EventoRegistrado(EventoDeSeguranca evento, UUID usuarioId, UUID organizacaoId, String traceId,
                            Instant ocorridoEm) {
        this.tipo = evento.tipo();
        this.ocorridoEm = ocorridoEm;
        this.usuarioId = usuarioId;
        this.organizacaoId = organizacaoId;
        this.emailMascarado = cortar(evento.emailMascarado(), TAMANHO_EMAIL);
        this.detalhe = cortar(evento.detalhe(), TAMANHO_DETALHE);
        this.traceId = cortar(traceId, 32);
    }

    /** Um e-mail digitado no login pode ter qualquer tamanho: o registro guarda até o limite da coluna. */
    private static String cortar(String texto, int limite) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }
}
