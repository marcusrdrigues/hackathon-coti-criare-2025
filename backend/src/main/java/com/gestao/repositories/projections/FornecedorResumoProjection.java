package com.gestao.repositories.projections;

import java.util.UUID;

public interface FornecedorResumoProjection {
    UUID getId();
    String getNome();
    String getEmail();
    Long getTotalPropostas();
}
