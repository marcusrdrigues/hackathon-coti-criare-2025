package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Prepara a V3 (pessoas e organizações): remove as chaves estrangeiras que ligam o negócio
 * às tabelas antigas de conta, {@code tb_empresa} e {@code tb_fornecedor}.
 *
 * <p>As chaves são encontradas pelo que ligam, e não pelo nome. O banco de produção nasceu do
 * Hibernate, antes do Flyway, e por isso as chaves dele têm nomes gerados ({@code fk3x9k…}), e
 * não os da V1. Uma migração em SQL que as removesse pelo nome falharia ali.
 */
public class V2_1__remove_chaves_das_contas_antigas extends BaseJavaMigration {

    private static final List<String> TABELAS_DO_NEGOCIO = List.of(
            "tb_cotacao", "tb_proposta", "tb_negociacao", "tb_mensagem_negociacao", "tb_refresh_token");
    private static final Set<String> CONTAS_ANTIGAS = Set.of("tb_empresa", "tb_fornecedor");
    /** Nomes de restrição que o Hibernate e a V1 geram: letras, números e sublinhado. */
    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Za-z0-9_]{1,63}");

    private record Chave(String tabela, String nome) {}

    @Override
    public void migrate(Context context) throws SQLException {
        Connection conexao = context.getConnection();
        List<Chave> antigas = chavesParaContasAntigas(conexao);
        try (Statement comando = conexao.createStatement()) {
            for (Chave chave : antigas) {
                // DDL não aceita parâmetro no lugar de um nome. A tabela vem da lista fixa acima e o
                // nome da chave vem do catálogo do próprio banco, conferido contra IDENTIFICADOR.
                String sql = "ALTER TABLE " + chave.tabela() + " DROP CONSTRAINT " + entreAspas(chave.nome());
                comando.execute(sql); // NOSONAR java:S2077: identificadores do catálogo, validados
            }
        }
    }

    private static List<Chave> chavesParaContasAntigas(Connection conexao) throws SQLException {
        DatabaseMetaData metadados = conexao.getMetaData();
        List<Chave> chaves = new ArrayList<>();
        for (String tabela : TABELAS_DO_NEGOCIO) {
            try (ResultSet importadas = metadados.getImportedKeys(conexao.getCatalog(), conexao.getSchema(), tabela)) {
                while (importadas.next()) {
                    String referenciada = importadas.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT);
                    String nome = importadas.getString("FK_NAME");
                    Chave chave = new Chave(tabela, nome);
                    // Uma chave composta aparece uma vez por coluna
                    if (CONTAS_ANTIGAS.contains(referenciada) && !chaves.contains(chave)) {
                        chaves.add(chave);
                    }
                }
            }
        }
        return chaves;
    }

    private static String entreAspas(String identificador) {
        if (!IDENTIFICADOR.matcher(identificador).matches()) {
            throw new IllegalStateException("Nome de restrição inesperado: " + identificador);
        }
        return '"' + identificador + '"';
    }
}
