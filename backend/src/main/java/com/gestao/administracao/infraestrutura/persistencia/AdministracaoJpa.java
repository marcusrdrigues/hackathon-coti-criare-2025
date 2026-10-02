package com.gestao.administracao.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.TipoOrganizacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consultas da área administrativa, em JPQL, sobre as organizações e o uso delas. */
interface AdministracaoJpa extends Repository<Organizacao, UUID> {

    /** A linha da lista de organizações, com os totais calculados no banco. */
    interface OrganizacaoComTotais {
        UUID getId();

        TipoOrganizacao getTipo();

        String getRazaoSocial();

        String getCnpj();

        LocalDateTime getCriadaEm();

        Long getPessoas();

        Long getCotacoes();

        Long getPropostas();
    }

    @Query(value = """
            SELECT o.id AS id, o.tipo AS tipo, o.razaoSocial AS razaoSocial, o.cnpj AS cnpj, o.criadaEm AS criadaEm,
                   (SELECT COUNT(m) FROM Membro m
                     WHERE m.organizacao = o AND m.removidoEm IS NULL AND m.usuario.desativadoEm IS NULL) AS pessoas,
                   (SELECT COUNT(c) FROM Cotacao c WHERE c.empresa = o) AS cotacoes,
                   (SELECT COUNT(p) FROM Proposta p WHERE p.fornecedor = o) AS propostas
            FROM Organizacao o""",
            countQuery = "SELECT COUNT(o) FROM Organizacao o")
    Page<OrganizacaoComTotais> organizacoes(Pageable pagina);
}
