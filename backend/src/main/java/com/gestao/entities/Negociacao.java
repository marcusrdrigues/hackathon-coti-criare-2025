package com.gestao.entities;

import com.gestao.enums.StatusNegociacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_negociacao")
public class Negociacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "valor_final", precision = 15, scale = 2)
    private BigDecimal valorFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusNegociacao status;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "datafinalizacao")
    private LocalDate dataFinalizacao;

    @OneToOne
    @JoinColumn(name = "proposta_id", nullable = false, unique = true)
    private Proposta proposta;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @OneToMany(mappedBy = "negociacao")
    @OrderBy("dataEnvio ASC")
    private List<MensagemNegociacao> mensagens = new ArrayList<>();

    /**
     * Último valor ofertado por qualquer uma das partes.
     * Se ninguém fez contraproposta ainda, vale o valor da proposta original.
     */
    public BigDecimal getUltimaOferta() {
        // A lista já vem em ordem cronológica (@OrderBy); vale a última com valor
        if (mensagens != null) {
            for (int i = mensagens.size() - 1; i >= 0; i--) {
                BigDecimal valor = mensagens.get(i).getValorOfertado();
                if (valor != null) {
                    return valor;
                }
            }
        }
        return proposta != null ? proposta.getValor() : null;
    }
}
