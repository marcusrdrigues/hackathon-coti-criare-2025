# 0013. Monólito modular com Clean Architecture, verificado por teste

- **Status:** Aceita
- **Data:** 2026-10-02

## Contexto

O back-end era organizado por camada técnica (`controllers`, `services`, `repositories`, `entities`…). Cada funcionalidade se espalhava por dez pastas, e nada impedia que uma parte do sistema mexesse direto nos dados de outra: o envio em tempo real e a demo, por exemplo, usavam os repositórios de cotação e negociação. As próximas fases (organizações, auditoria, IA) vão acrescentar muito código, e precisam de fronteiras claras.

Houve a opção de partir para microserviços já, mantendo o monorepo.

## Decisão

**Um único deploy, dividido em módulos de negócio**, cada um com as camadas da Clean Architecture:

| Módulo | Responsabilidade |
|---|---|
| `identidade` | Contas, login, sessões e segurança da API |
| `compras` | Cotações, propostas, negociações e mensagens |
| `painel` | Números dos painéis: só leitura, com consultas próprias |
| `temporeal` | WebSocket: leva os eventos de `compras` a quem está conectado |
| `demonstracao` | Contas e dados da demo pública |
| `compartilhado` | Exceções de negócio, validação de documentos e configuração web comum |

Dentro de cada módulo:

- **`dominio`**: entidades, status e eventos. Não conhece aplicação nem infraestrutura.
- **`aplicacao`**: casos de uso, DTOs e **portas** (interfaces) para tudo que é externo: persistência, emissão de token, hash de senha, canal de tempo real. Não conhece infraestrutura, web nem Spring Data.
- **`infraestrutura`**: os **adaptadores**. Controllers REST, implementações das portas com Spring Data JPA ou JDBC, segurança e STOMP.

Regras entre módulos:

- Um módulo só usa o que o outro expõe como interface nomeada: `dominio`, `aplicacao` e `dto`. No caso da identidade, também a `seguranca` (quem está logado), usada pelos controllers e pelo WebSocket dos outros módulos. As portas e a infraestrutura são privadas.
- Sem dependência circular. `compartilhado` é aberto e não depende de ninguém.
- **Cotação, proposta e negociação formam um módulo só (`compras`).** Separadas, elas criavam dependência circular, e o motivo é de negócio: abrir uma negociação aceita a proposta e muda a cotação na mesma transação; fechar o negócio encerra a cotação e recusa as outras propostas. São partes do mesmo contexto.
- `painel` é o lado de leitura (CQRS no mesmo banco): consulta as tabelas de compras com JPQL próprio e nunca passa pelas regras de escrita.

Tudo isso é verificado em [`ArquiteturaTest`](../../backend/src/test/java/com/gestao/ArquiteturaTest.java). O Spring Modulith confere módulos, interfaces nomeadas e ciclos, e o ArchUnit confere a direção das camadas. Quebrar uma regra quebra o build.

## Alternativas consideradas

- **Microserviços agora**: abrir e fechar uma negociação viraria uma saga com compensação entre três serviços, cada um com seu banco. No plano gratuito do Render, cada serviço tem a sua partida a frio. Seriam semanas de infraestrutura (gateway, mensageria, rastreio distribuído) sem ganho para o usuário, para um sistema com uma pessoa desenvolvendo e carga baixa.
- **Só a autenticação como serviço à parte**: ensinaria JWKS e contrato entre serviços, mas o cadastro viraria um fluxo distribuído sem necessidade real.
- **Manter as camadas técnicas**: o código continuaria funcionando, mas sem fronteiras, e cada fase nova aumentaria o acoplamento.
- **Clean Architecture "pura", com modelos de domínio separados das entidades JPA**: dobraria classes e mapeamentos sem benefício agora (ver Consequências).

## Consequências

- **Um módulo pode virar serviço sem reescrever o resto**, porque os outros só falam com a API dele. Os critérios para extrair: escala, ritmo de deploy ou tecnologia diferentes do resto, medidos e não supostos. O primeiro candidato é a IA (fase 7), que tem custo, latência e escala próprios.
- **Concessão consciente:** as entidades do domínio levam anotações do JPA e se relacionam entre módulos (cotação → empresa). Separar modelo de domínio e modelo de persistência fica para quando um módulo precisar de outro banco.
- Portas na aplicação permitem testar os casos de uso sem banco e trocar a implementação (outro banco, outro provedor de token) sem mexer nas regras.
- Quem cria código novo precisa colocá-lo no módulo e na camada certos. O teste de arquitetura avisa quando não estiver.
