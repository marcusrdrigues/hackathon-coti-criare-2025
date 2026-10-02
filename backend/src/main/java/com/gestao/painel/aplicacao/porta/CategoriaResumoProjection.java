package com.gestao.painel.aplicacao.porta;

import com.gestao.compras.dominio.CategoriaCotacao;

public interface CategoriaResumoProjection {
    CategoriaCotacao getCategoria();
    Long getTotal();
}
