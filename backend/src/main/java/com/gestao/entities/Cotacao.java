package com.gestao.entities;

import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.StatusCotacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_cotacao")
public class Cotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_servico", length = 200, nullable = false)
    private String nomeServico;

    @Column(name = "requisitos", length = 1000, nullable = false)
    private String requisitos;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaCotacao categoria;

    /** Valor de referência opcional, exibido aos fornecedores no mural. */
    @Column(name = "orcamento_estimado", precision = 15, scale = 2)
    private BigDecimal orcamentoEstimado;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_limite")
    private LocalDateTime dataLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusCotacao status;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToMany(mappedBy = "cotacao")
    private List<Proposta> propostas = new ArrayList<>();

    /** Indica se o prazo para envio de propostas já passou. */
    public boolean isPrazoEncerrado() {
        return dataLimite != null && dataLimite.isBefore(LocalDateTime.now());
    }
}
