package com.gestao.administracao.aplicacao.porta;

import com.gestao.administracao.aplicacao.dto.OrganizacaoResumoResponse;
import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;

import java.util.Set;

/** Porta: consultas só de leitura da área administrativa. */
public interface ConsultasDaAdministracao {

    /** Campos aceitos para ordenar a lista de organizações. */
    Set<String> ORDENS_DAS_ORGANIZACOES = Set.of("razaoSocial", "criadaEm");

    Pagina<OrganizacaoResumoResponse> organizacoes(PedidoDePagina pedido);
}
