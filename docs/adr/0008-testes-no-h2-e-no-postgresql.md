# 0008. Testes no H2 e também no PostgreSQL com Testcontainers

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

Os testes de integração rodavam só num H2 em memória (modo PostgreSQL). Ele é rápido e não exige nada instalado, mas só imita o PostgreSQL: tipos como `TIMESTAMP WITH TIME ZONE`, consultas agregadas do dashboard e algumas restrições se comportam diferente. Um teste verde no H2 não garante um deploy verde.

## Decisão

- Manter o H2 como banco padrão dos testes, para o ciclo local continuar rápido e sem dependências.
- Acrescentar suítes que rodam num **PostgreSQL 16 real via Testcontainers** (`PostgresTestcontainersConfig`, com `@ServiceConnection`):
  - `MigracoesPostgresTest`: o Flyway aplica tudo, o Hibernate valida e as tabelas existem.
  - `FluxoCotacaoPostgresTest` e `AutenticacaoPostgresTest`: herdam as suítes existentes e repetem os mesmos cenários no PostgreSQL.
- As suítes do PostgreSQL compartilham um único contexto do Spring e, por isso, um único contêiner. Sem Docker elas são puladas (`disabledWithoutDocker`); no CI sempre rodam.

## Alternativas consideradas

- **Só Testcontainers**: obrigaria a ter Docker para rodar qualquer teste e deixaria o ciclo local mais lento.
- **Só H2**: o cenário anterior, que esconde diferenças de dialeto.
- **PostgreSQL fixo no CI (service container)**: já é usado no teste de fumaça, mas não roda no `./mvnw verify` local de quem tem Docker.

## Consequências

- Os cenários de negócio e de autenticação são validados nos dois bancos, sem duplicar código de teste.
- O CI fica um pouco mais lento por baixar a imagem e subir o contêiner.
