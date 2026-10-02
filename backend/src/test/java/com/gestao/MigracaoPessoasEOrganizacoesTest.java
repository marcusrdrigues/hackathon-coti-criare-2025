package com.gestao;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A migração V3 (pessoas e organizações) sobre dados no formato antigo, num PostgreSQL real.
 * O banco é levado até a V2, fica com as restrições nomeadas como o Hibernate nomeia (o banco
 * de produção nasceu dele, antes do Flyway), recebe uma empresa, um fornecedor e um negócio
 * inteiro gravados como a versão anterior gravava, e então as V2.1 e V3 são aplicadas.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracaoPessoasEOrganizacoesTest {

    private static final String EMPRESA = "00000000-0000-0000-0000-00000000000e";
    private static final String FORNECEDOR = "00000000-0000-0000-0000-00000000000f";
    private static final String COTACAO = "00000000-0000-0000-0000-0000000000c0";
    private static final String PROPOSTA = "00000000-0000-0000-0000-0000000000b0";
    private static final String NEGOCIACAO = "00000000-0000-0000-0000-0000000000a0";
    private static final String HASH_EMPRESA = "$2a$10$hashDaEmpresaGravadoNaVersaoAnterior";
    private static final String HASH_FORNECEDOR = "$2a$10$hashDoFornecedorGravadoNaVersaoAnterior";
    private static final String RAZAO_SOCIAL_LONGA = "Criare Consulting " + "e Participações ".repeat(11);

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(PostgresTestcontainersConfig.IMAGEM);

    private static JdbcTemplate jdbc;
    private static DriverManagerDataSource banco;

    @BeforeAll
    static void migrarDadosAntigos() {
        banco = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(banco);

        flyway(banco, "2").migrate();
        nomearComoOHibernate();
        gravarNoFormatoAntigo();
        flyway(banco, "3").migrate();
    }

    @Test
    void empresaEFornecedorViramOrganizacoesComOMesmoId() {
        List<Map<String, Object>> organizacoes = jdbc.queryForList(
                "select id::text as id, tipo, razao_social, cnpj from tb_organizacao order by tipo");

        assertEquals(2, organizacoes.size());
        assertEquals(Map.of("id", EMPRESA, "tipo", "EMPRESA", "razao_social", RAZAO_SOCIAL_LONGA,
                "cnpj", "11222333000181"), organizacoes.get(0));
        assertEquals(Map.of("id", FORNECEDOR, "tipo", "FORNECEDOR", "razao_social", "Tech Soluções",
                "cnpj", "45236789000112"), organizacoes.get(1));
    }

    @Test
    void cadaContaViraUmaPessoaProprietariaQueEntraComAMesmaSenha() {
        List<Map<String, Object>> pessoas = jdbc.queryForList("""
                select u.id::text as id, u.nome, u.email, u.senha_hash, m.organizacao_id::text as organizacao, m.papel
                from tb_usuario u join tb_membro m on m.usuario_id = u.id
                where not u.superadmin and u.desativado_em is null and m.removido_em is null
                order by u.email
                """);

        assertEquals(2, pessoas.size());
        assertEquals(Map.of("id", EMPRESA, "nome", RAZAO_SOCIAL_LONGA.substring(0, 150),
                "email", "compras@criare.com", "senha_hash", HASH_EMPRESA,
                "organizacao", EMPRESA, "papel", "PROPRIETARIO"), pessoas.get(0));
        assertEquals(Map.of("id", FORNECEDOR, "nome", "Tech Soluções",
                "email", "vendas@tech.com", "senha_hash", HASH_FORNECEDOR,
                "organizacao", FORNECEDOR, "papel", "PROPRIETARIO"), pessoas.get(1));
    }

    @Test
    void negocioContinuaLigadoAsOrganizacoesEGanhaAutoria() {
        assertEquals(EMPRESA, texto("select empresa_id::text from tb_cotacao"));
        assertEquals(EMPRESA, texto("select criada_por::text from tb_cotacao"));
        assertEquals(FORNECEDOR, texto("select enviada_por::text from tb_proposta"));
        assertEquals(List.of(EMPRESA, FORNECEDOR), jdbc.queryForList("""
                select remetente_id::text from tb_mensagem_negociacao order by data_hora_envio
                """, String.class));
    }

    @Test
    void sessoesAntigasSaoEncerradas() {
        assertEquals(0, jdbc.queryForObject("select count(*) from tb_refresh_token", Integer.class));
        List<String> colunas = jdbc.queryForList("""
                select column_name from information_schema.columns where table_name = 'tb_refresh_token'
                """, String.class);
        assertFalse(colunas.contains("tipo_usuario"), "a coluna tipo_usuario deveria ter saído: " + colunas);
    }

    @Test
    void chavesEstrangeirasApontamParaAsTabelasNovas() {
        List<String> destinos = jdbc.queryForList("""
                select tc.constraint_name || '>' || ccu.table_name
                from information_schema.table_constraints tc
                join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name
                where tc.constraint_type = 'FOREIGN KEY' and tc.constraint_name in (
                    'fk_cotacao_empresa', 'fk_proposta_fornecedor', 'fk_negociacao_empresa',
                    'fk_negociacao_fornecedor', 'fk_cotacao_criada_por', 'fk_proposta_enviada_por',
                    'fk_mensagem_remetente', 'fk_refresh_token_usuario')
                order by 1
                """, String.class);

        assertEquals(List.of(
                "fk_cotacao_criada_por>tb_usuario",
                "fk_cotacao_empresa>tb_organizacao",
                "fk_mensagem_remetente>tb_usuario",
                "fk_negociacao_empresa>tb_organizacao",
                "fk_negociacao_fornecedor>tb_organizacao",
                "fk_proposta_enviada_por>tb_usuario",
                "fk_proposta_fornecedor>tb_organizacao",
                "fk_refresh_token_usuario>tb_usuario"), destinos);
    }

    @Test
    void nenhumaChaveApontaMaisParaAsContasAntigas() {
        List<String> antigas = jdbc.queryForList("""
                select tc.constraint_name
                from information_schema.table_constraints tc
                join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name
                where tc.constraint_type = 'FOREIGN KEY' and ccu.table_name in ('tb_empresa', 'tb_fornecedor')
                  and tc.table_name not in ('tb_empresa', 'tb_fornecedor')
                """, String.class);
        assertEquals(List.of(), antigas);
    }

    @Test
    void v5ApagaAsTabelasAntigasSemPerderNada() {
        Flyway.configure().dataSource(banco).locations("classpath:db/migration").load().migrate();

        List<String> antigas = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_name in ('tb_empresa', 'tb_fornecedor', 'tb_perfil')
                """, String.class);
        assertEquals(List.of(), antigas);
        assertEquals(2, jdbc.queryForObject("select count(*) from tb_organizacao", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from tb_negociacao", Integer.class));
    }

    // ---------------------------------------------------------------- apoio

    /** Os nomes que o Hibernate gera: "FK" + hash para as chaves e "tabela_coluna_check" para as checagens. */
    private static void nomearComoOHibernate() {
        jdbc.execute("alter table tb_cotacao rename constraint fk_cotacao_empresa to fk8x1m2c3o4t5a6c7a8o9");
        jdbc.execute("alter table tb_proposta rename constraint fk_proposta_fornecedor to fkp1r2o3p4o5s6t7a8");
        jdbc.execute("alter table tb_negociacao rename constraint fk_negociacao_empresa to fkn1e2g3e4m5p6");
        jdbc.execute("alter table tb_negociacao rename constraint fk_negociacao_fornecedor to fkn1e2g3f4o5r6");
        jdbc.execute("alter table tb_refresh_token rename constraint ck_refresh_token_tipo "
                + "to tb_refresh_token_tipo_usuario_check");
    }

    private static Flyway flyway(DriverManagerDataSource banco, String versao) {
        return Flyway.configure().dataSource(banco).locations("classpath:db/migration").target(versao).load();
    }

    /** Como a versão anterior gravava: a conta de acesso dentro da empresa e do fornecedor. */
    private static void gravarNoFormatoAntigo() {
        jdbc.update("insert into tb_perfil (id, nome) values ('00000000-0000-0000-0000-000000000001', 'EMPRESA')");
        jdbc.update("insert into tb_perfil (id, nome) values ('00000000-0000-0000-0000-000000000002', 'FORNECEDOR')");
        jdbc.update("""
                insert into tb_empresa (id, nome, cnpj, email, senha, perfil_id)
                values (?::uuid, ?, '11.222.333/0001-81', 'compras@criare.com', ?, '00000000-0000-0000-0000-000000000001')
                """, EMPRESA, RAZAO_SOCIAL_LONGA, HASH_EMPRESA);
        jdbc.update("""
                insert into tb_fornecedor (id, nome, cnpj, email, senha, perfil_id)
                values (?::uuid, 'Tech Soluções', '45236789000112', 'vendas@tech.com', ?, '00000000-0000-0000-0000-000000000002')
                """, FORNECEDOR, HASH_FORNECEDOR);
        jdbc.update("""
                insert into tb_cotacao (id, nome_servico, requisitos, categoria, data_criacao, data_limite, status, empresa_id)
                values (?::uuid, 'Notebooks', '10 notebooks', 'TECNOLOGIA', now(), now() + interval '5 days',
                        'EM_NEGOCIACAO', ?::uuid)
                """, COTACAO, EMPRESA);
        jdbc.update("""
                insert into tb_proposta (id, valor_proposto, descricao, status, data_envio, fornecedor_id, cotacao_id)
                values (?::uuid, 1000.00, 'Entrega em 10 dias', 'ACEITA', now(), ?::uuid, ?::uuid)
                """, PROPOSTA, FORNECEDOR, COTACAO);
        jdbc.update("""
                insert into tb_negociacao (id, status, data_hora_inicio, proposta_id, empresa_id, fornecedor_id)
                values (?::uuid, 'EM_ANDAMENTO', now(), ?::uuid, ?::uuid, ?::uuid)
                """, NEGOCIACAO, PROPOSTA, EMPRESA, FORNECEDOR);
        jdbc.update("""
                insert into tb_mensagem_negociacao (id, mensagem, tipo_remetente, remetente_id, data_hora_envio, negociacao_id)
                values (gen_random_uuid(), 'Fecha em 900?', 'EMPRESA', ?::uuid, now() - interval '1 minute', ?::uuid)
                """, EMPRESA, NEGOCIACAO);
        jdbc.update("""
                insert into tb_mensagem_negociacao (id, mensagem, tipo_remetente, remetente_id, data_hora_envio, negociacao_id)
                values (gen_random_uuid(), 'Fecho em 950.', 'FORNECEDOR', ?::uuid, now(), ?::uuid)
                """, FORNECEDOR, NEGOCIACAO);
        jdbc.update("""
                insert into tb_refresh_token (id, token_hash, usuario_id, tipo_usuario, criado_em, expira_em)
                values (gen_random_uuid(), 'hash-de-uma-sessao-antiga', ?::uuid, 'EMPRESA', now(), now() + interval '7 days')
                """, EMPRESA);
    }

    private static String texto(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }
}
