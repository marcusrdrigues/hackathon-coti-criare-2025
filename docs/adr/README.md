# Registros de decisão de arquitetura (ADRs)

Cada arquivo registra uma decisão importante: o contexto, o que foi decidido, as alternativas e o preço que se paga por ela. Quem chega ao projeto consegue entender o *porquê* sem precisar perguntar.

Uma decisão registrada não se edita. Se ela mudar, um ADR novo a substitui, e o antigo passa para o status "Substituída por ADR-XXXX".

| # | Decisão | Status |
|---|---|---|
| [0001](0001-registrar-decisoes-em-adrs.md) | Registrar decisões de arquitetura em ADRs | Aceita |
| [0002](0002-monorepo.md) | Back-end e front-end num monorepo | Aceita |
| [0003](0003-access-token-curto-e-refresh-token-opaco.md) | Access token JWT curto e refresh token opaco em cookie | Aceita |
| [0004](0004-jwt-assinado-com-hs256.md) | JWT assinado com HS256 pelo resource server do Spring | Aceita |
| [0005](0005-autorizacao-por-perfil-e-por-posse.md) | Autorização por perfil no controller e por posse no service | Aceita |
| [0006](0006-proxy-da-vercel-para-a-api.md) | Front-end e API na mesma origem pelo proxy da Vercel | Aceita |
| [0007](0007-flyway-com-validacao-do-hibernate.md) | Esquema versionado com Flyway e validado pelo Hibernate | Aceita |
| [0008](0008-testes-no-h2-e-no-postgresql.md) | Testes no H2 e também no PostgreSQL com Testcontainers | Aceita |
| [0009](0009-demo-publica-com-dados-restaurados.md) | Demo pública com contas prontas e dados restaurados todo dia | Aceita |
| [0010](0010-design-tokens-e-tema-do-sistema.md) | Design tokens e tema que segue o sistema | Aceita |
| [0011](0011-componentes-proprios-no-lugar-do-bootstrap.md) | Componentes próprios no lugar do Bootstrap | Aceita |
| [0012](0012-tempo-real-com-websocket-e-stomp.md) | Tempo real com WebSocket e STOMP | Aceita |
| [0013](0013-monolito-modular-com-clean-architecture.md) | Monólito modular com Clean Architecture, verificado por teste | Aceita |
| [0014](0014-rastreio-por-requisicao-e-logs-estruturados.md) | Rastreio por requisição e logs estruturados, sem dados sensíveis | Aceita |
| [0015](0015-pessoas-e-organizacoes-separadas.md) | Pessoas e organizações separadas, com papéis e autoria | Aceita |

Modelo para um ADR novo: [template.md](template.md).
