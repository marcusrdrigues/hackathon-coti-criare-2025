package com.gestao.administracao.aplicacao;

import com.gestao.administracao.aplicacao.dto.OrganizacaoResumoResponse;
import com.gestao.administracao.aplicacao.porta.ConsultasDaAdministracao;
import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso do superadmin. Nesta fase, só consulta: nada aqui altera dados das organizações. */
@Service
@RequiredArgsConstructor
public class AdministracaoService {

    private final ConsultasDaAdministracao consultas;

    @Transactional(readOnly = true)
    public Pagina<OrganizacaoResumoResponse> organizacoes(PedidoDePagina pedido) {
        return consultas.organizacoes(pedido);
    }
}
