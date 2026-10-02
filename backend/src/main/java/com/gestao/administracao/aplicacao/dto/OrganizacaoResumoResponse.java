package com.gestao.administracao.aplicacao.dto;

import com.gestao.identidade.dominio.TipoOrganizacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma organização vista pelo superadmin: quem é, desde quando e quanto ela usa a plataforma. */
@Schema(description = "Organização com os totais de uso")
public record OrganizacaoResumoResponse(
        UUID id,
        TipoOrganizacao tipo,
        String razaoSocial,
        String cnpj,
        LocalDateTime criadaEm,
        @Schema(description = "Pessoas ativas na equipe") long pessoas,
        @Schema(description = "Cotações publicadas (empresas)") long cotacoes,
        @Schema(description = "Propostas enviadas (fornecedores)") long propostas) {
}
