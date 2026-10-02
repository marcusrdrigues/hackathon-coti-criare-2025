package com.gestao.administracao.infraestrutura.persistencia;

import com.gestao.administracao.aplicacao.dto.OrganizacaoResumoResponse;
import com.gestao.administracao.aplicacao.porta.ConsultasDaAdministracao;
import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;

/** Adaptador: as consultas do superadmin com JPQL, convertendo a paginação de e para o Spring Data. */
@Repository
@RequiredArgsConstructor
class ConsultasDaAdministracaoJpa implements ConsultasDaAdministracao {

    /** Campo da API -> propriedade da entidade. Só o que está aqui pode ordenar a consulta. */
    private static final Map<String, String> PROPRIEDADES = Map.of(
            "razaoSocial", "razaoSocial",
            "criadaEm", "criadaEm");

    private final AdministracaoJpa jpa;

    @Override
    public Pagina<OrganizacaoResumoResponse> organizacoes(PedidoDePagina pedido) {
        String propriedade = Objects.requireNonNull(PROPRIEDADES.get(pedido.ordem().campo()), "ordem não permitida");
        Sort ordem = Sort.by(pedido.ordem().crescente() ? Sort.Direction.ASC : Sort.Direction.DESC, propriedade)
                .and(Sort.by("id")); // desempate: a mesma ordem em todas as páginas
        Page<AdministracaoJpa.OrganizacaoComTotais> pagina =
                jpa.organizacoes(PageRequest.of(pedido.numero(), pedido.tamanho(), ordem));
        return Pagina.de(pagina.getContent().stream().map(ConsultasDaAdministracaoJpa::paraResposta).toList(),
                pedido, pagina.getTotalElements());
    }

    private static OrganizacaoResumoResponse paraResposta(AdministracaoJpa.OrganizacaoComTotais linha) {
        return new OrganizacaoResumoResponse(linha.getId(), linha.getTipo(), linha.getRazaoSocial(), linha.getCnpj(),
                linha.getCriadaEm(), valor(linha.getPessoas()), valor(linha.getCotacoes()), valor(linha.getPropostas()));
    }

    private static long valor(Long total) {
        return total == null ? 0 : total;
    }
}
