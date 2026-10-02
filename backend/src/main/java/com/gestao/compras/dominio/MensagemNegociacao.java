package com.gestao.compras.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_mensagem_negociacao")
public class MensagemNegociacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "mensagem", length = 1000, nullable = false)
    private String mensagem;

    /** Preenchido quando a mensagem é uma oferta/contraproposta de valor. */
    @Column(name = "valor_ofertado", precision = 15, scale = 2)
    private BigDecimal valorOfertado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_remetente", nullable = false)
    private TipoRemetente tipoRemetente;

    @Column(name = "remetente_id", nullable = false)
    private UUID remetenteId;

    @Column(name = "data_hora_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @ManyToOne
    @JoinColumn(name = "negociacao_id", nullable = false)
    private Negociacao negociacao;
}
