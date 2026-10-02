package com.gestao.demonstracao.infraestrutura.persistencia;

import com.gestao.demonstracao.aplicacao.porta.BaseDaDemonstracao;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Adaptador: limpa a base da demo direto pelo JDBC, sem conhecer as classes dos
 * outros módulos. Roda dentro da transação de quem chama.
 */
@Repository
@Profile("demo")
@RequiredArgsConstructor
class BaseDaDemonstracaoJdbc implements BaseDaDemonstracao {

    /** Das tabelas que dependem para as que são referenciadas. */
    private static final String[] LIMPEZA = {
            "DELETE FROM tb_mensagem_negociacao",
            "DELETE FROM tb_negociacao",
            "DELETE FROM tb_proposta",
            "DELETE FROM tb_cotacao",
            "DELETE FROM tb_refresh_token",
            "DELETE FROM tb_convite",
            "DELETE FROM tb_membro",
            "DELETE FROM tb_usuario",
            "DELETE FROM tb_organizacao"};

    private final JdbcTemplate jdbc;

    @Override
    public boolean temDados() {
        Integer organizacoes = jdbc.queryForObject("SELECT COUNT(*) FROM tb_organizacao", Integer.class);
        return organizacoes != null && organizacoes > 0;
    }

    @Override
    public void apagarTudo() {
        jdbc.batchUpdate(LIMPEZA);
    }
}
