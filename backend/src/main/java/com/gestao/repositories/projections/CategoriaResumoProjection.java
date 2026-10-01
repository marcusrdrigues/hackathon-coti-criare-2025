package com.gestao.repositories.projections;

import com.gestao.enums.CategoriaCotacao;

public interface CategoriaResumoProjection {
    CategoriaCotacao getCategoria();
    Long getTotal();
}
