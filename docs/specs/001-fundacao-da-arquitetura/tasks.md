# 001 · Tarefas

Cada passo é entregue com o CI verde (testes, ponta a ponta e quality gate) e com o roadmap do README atualizado.

## Passo 1 · Spec, plano e roadmap

- [x] Processo de SDD documentado em [`docs/specs/`](../README.md)
- [x] Spec, plano e tarefas da fundação
- [x] Roadmap do README reorganizado em fases
- [x] Decisões em aberto da spec resolvidas

## Passo 2 · Módulos (R5)

- [x] Spring Modulith compatível com o Spring Boot 4
- [x] Código nos módulos `identidade`, `compras`, `painel`, `temporeal`, `demonstracao` e `compartilhado`, cada um com `dominio`, `aplicacao` e `infraestrutura`
- [x] Portas e adaptadores: persistência, token, senha, canal de tempo real e limpeza da demo
- [x] Teste que verifica módulos (Spring Modulith) e camadas (ArchUnit)
- [x] Inventário da API: endpoints que nenhum cliente usa foram removidos, junto com o código por trás deles (OWASP API9)
- [x] Testes do contrato HTTP de compras e do tratamento de erros (cobertura do back-end de 72% para 94% das linhas)
- [x] CI aponta as classes com mais linhas sem teste e o motivo quando o quality gate reprova
- [x] ADR 0013 e seção de arquitetura do README

## Passo 3 · Rastreio e logs (R6)

- [x] `traceId` (OpenTelemetry) em todos os logs da requisição, no cabeçalho `X-Trace-Id` e no corpo dos erros
- [x] Front-end mostra o começo do código de rastreio quando a falha é do servidor
- [x] Logs em JSON (ECS) em produção, conferidos no CI
- [x] Teste que garante que os logs não trazem senha, token, e-mail completo nem conteúdo de negociação
- [x] ADR 0014

## Passo 4 · Pessoas e organizações (R1)

- [x] Migração V3 com os dados existentes preservados, testada no PostgreSQL
- [x] Cadastro cria organização e proprietário juntos
- [x] Token com pessoa, organização, tipo e papel
- [x] Cotação, proposta e mensagem registram a pessoa
- [x] Front-end: cadastro, menu da conta e nome da pessoa nas mensagens
- [x] Demo com duas pessoas na Criare e na Tech (as contas de um clique e as negociações de exemplo)
- [x] ADR 0015

## Passo 5 · Autorização por organização (R4)

- [x] Política de acesso única por módulo (`AcessoCompras`), usada pelo REST e pelo WebSocket
- [x] 404 para recurso de outra organização, com a mesma mensagem de um id inexistente
- [x] Teste de isolamento em todas as rotas com id (no caminho ou no corpo), com trava para rota nova
- [x] ADR 0016, que substitui o 0005

## Passo 6 · Equipe (R2)

- [x] Convite por link de uso único (72 horas, guardado como hash, token fora da URL que vai ao servidor)
- [x] Aceitar convite, listar e remover membros, com revogação das sessões e do WebSocket
- [x] Telas de equipe e de aceitar convite
- [x] Teste ponta a ponta: convidar, aceitar, operar como membro e ser removido
- [x] ADR 0017
- [x] Visual Liquid Glass na camada flutuante, com barra de abas em cápsula (ADR 0018)

## Passo 7 · Superadmin (R3)

- [ ] Criação pela configuração do servidor
- [ ] Área administrativa somente leitura, com a lista de organizações
- [ ] Testes de acesso negado para usuários comuns

## Passo 8 · Padrões de API (R7)

- [ ] Paginação nas listagens, com "Carregar mais" no front-end
- [ ] Erros no formato Problem Details, com o front-end ajustado
- [ ] ADR 0020

## Passo 9 · Política de dados (R8)

- [ ] `docs/dados.md` com a classificação e as regras por destino

## Passo 10 · Fechamento

- [ ] Migração V5 removendo as tabelas antigas, depois da validação em produção
- [ ] Spec marcada como concluída e fase 4 fechada no README
