# 0007. Esquema versionado com Flyway e validado pelo Hibernate

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

O esquema era gerado pelo Hibernate (`ddl-auto=update`). Essa opção não remove colunas, não cria restrições com nome e não deixa registro do que mudou. Também não há como revisar uma mudança de banco num PR. Além disso, o banco de produção (Neon) já existia, com dados, quando a decisão foi tomada.

## Decisão

- O esquema vive em migrações SQL do Flyway (`backend/src/main/resources/db/migration`), começando por `V1__esquema_inicial.sql`, com chaves, restrições e índices nomeados.
- O Hibernate passa a só validar (`spring.jpa.hibernate.ddl-auto=validate`). Se uma entidade divergir do banco, a aplicação não sobe.
- Para o banco que já existia, `baseline-on-migrate=true` com `baseline-version=1`: o Flyway registra a V1 como já aplicada e segue dali.
- Os testes usam as mesmas migrações (H2 em modo PostgreSQL e PostgreSQL real, ver [ADR 0008](0008-testes-no-h2-e-no-postgresql.md)).

## Alternativas consideradas

- **Continuar com `ddl-auto=update`**: sem histórico nem revisão, e com risco de deixar o banco num estado que ninguém consegue reproduzir.
- **Liquibase**: equivalente em recursos, mas com XML/YAML no lugar de SQL puro. SQL é mais fácil de revisar.

## Consequências

- Toda mudança de entidade vem com uma migração nova (`V2__...`). Migrações aplicadas nunca são editadas.
- O CI aplica as migrações num PostgreSQL de verdade antes do deploy.
