package com.gestao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL de verdade, num contêiner, para os testes que precisam do mesmo
 * banco da produção. O {@code @ServiceConnection} troca a URL do H2 dos testes
 * pela do contêiner; o Flyway roda as migrações e o Hibernate valida o esquema.
 *
 * <p>Os testes que importam esta configuração compartilham o mesmo contexto do
 * Spring e, portanto, o mesmo contêiner. Sem Docker na máquina, eles são pulados
 * (ver {@code @Testcontainers(disabledWithoutDocker = true)}); no CI sempre rodam.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfig {

    /** Mesma versão principal do docker-compose e do CI. */
    public static final String IMAGEM = "postgres:16-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(IMAGEM);
    }
}
