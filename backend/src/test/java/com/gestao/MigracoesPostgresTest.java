package com.gestao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sobe a aplicação inteira contra um PostgreSQL real. Se o contexto carrega,
 * o Flyway aplicou as migrações e o Hibernate ({@code ddl-auto=validate})
 * concordou com elas; os testes abaixo conferem o resultado no banco.
 */
@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class MigracoesPostgresTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void bancoEhPostgres() {
        String versao = jdbc.queryForObject("select version()", String.class);
        assertTrue(versao.startsWith("PostgreSQL 16"), versao);
    }

    @Test
    void todasAsMigracoesForamAplicadasComSucesso() {
        List<String> falhas = jdbc.queryForList(
                "select version from flyway_schema_history where not success", String.class);
        assertTrue(falhas.isEmpty(), "migrações com falha: " + falhas);

        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version is not null", Integer.class);
        assertTrue(aplicadas >= 1);
    }

    @Test
    void esquemaTemTodasAsTabelas() {
        List<String> tabelas = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_name like 'tb\\_%'
                order by table_name
                """, String.class);
        assertEquals(List.of(
                "tb_convite", "tb_convite_aud", "tb_cotacao", "tb_cotacao_aud", "tb_membro", "tb_membro_aud",
                "tb_mensagem_negociacao", "tb_negociacao", "tb_negociacao_aud", "tb_organizacao",
                "tb_organizacao_aud", "tb_proposta", "tb_proposta_aud", "tb_refresh_token", "tb_revisao",
                "tb_revisao_entidade", "tb_usuario", "tb_usuario_aud"), tabelas);
    }
}
