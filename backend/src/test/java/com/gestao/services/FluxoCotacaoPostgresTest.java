package com.gestao.services;

import com.gestao.PostgresTestcontainersConfig;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Os mesmos cenários de {@link FluxoCotacaoIntegrationTest}, agora num
 * PostgreSQL real: pega diferenças que o H2 esconde (tipos, consultas do
 * dashboard, restrições do banco).
 */
@Import(PostgresTestcontainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class FluxoCotacaoPostgresTest extends FluxoCotacaoIntegrationTest {
}
