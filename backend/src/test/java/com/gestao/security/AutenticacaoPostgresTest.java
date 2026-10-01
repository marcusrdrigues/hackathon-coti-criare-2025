package com.gestao.security;

import com.gestao.PostgresTestcontainersConfig;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Os cenários de {@link AutenticacaoIntegrationTest} num PostgreSQL real:
 * rotação e revogação do refresh token dependem de datas com fuso e de
 * atualizações em lote, que o H2 só imita.
 */
@Import(PostgresTestcontainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class AutenticacaoPostgresTest extends AutenticacaoIntegrationTest {
}
