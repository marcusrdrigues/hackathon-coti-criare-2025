package com.gestao.painel.aplicacao.porta;

import java.util.UUID;

public interface FornecedorResumoProjection {
    UUID getId();
    String getNome();
    String getEmail();
    Long getTotalPropostas();
}
