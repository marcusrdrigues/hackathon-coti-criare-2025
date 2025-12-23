package com.gestao.entities;

import com.gestao.enums.TipoRemetente;  // ← MUDOU
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "tb_mensagem_negociacao")
public class MensagemNegociacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "mensagem", length = 1000, nullable = false)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_remetente", nullable = false)
    private TipoRemetente tipoRemetente;

    @Column(name = "remetente_id", nullable = false)
    private UUID remetenteId;

    @Column(name = "data_envio", nullable = false)
    private LocalDate dataEnvio;

    @ManyToOne
    @JoinColumn(name = "negociacao_id", nullable = false)
    private Negociacao negociacao;
}