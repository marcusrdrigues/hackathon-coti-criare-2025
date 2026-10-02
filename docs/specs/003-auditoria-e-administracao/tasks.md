# 003 · Tarefas

Cada passo termina com o CI verde e o roadmap do README atualizado.

## Passo 1 · Spec

- [x] Spec, plano e tarefas
- [ ] Spec aprovada

## Passo 2 · Histórico de alterações (R1)

- [ ] Hibernate Envers nas entidades, sem credenciais
- [ ] Revisão com pessoa, organização, rastreio e origem
- [ ] Migração V6 com as tabelas do histórico
- [ ] Testes: valores anteriores, exclusão, origem `SISTEMA`, nenhum hash no histórico
- [ ] ADR 0021

## Passo 3 · Eventos de segurança (R2)

- [ ] Evento de domínio e gravação em transação própria
- [ ] Login, renovação, equipe, superadmin e 403 publicando eventos
- [ ] Testes: evento registrado mesmo com a operação desfeita, e-mail mascarado

## Passo 4 · Só acréscimo e retenção (R3)

- [ ] Gatilho no PostgreSQL e as três rotinas de manutenção
- [ ] Rotina diária de retenção de 5 anos
- [ ] Reset da demo com a auditoria
- [ ] Teste no PostgreSQL real: alterar e apagar são recusados

## Passo 5 · Atividade do proprietário (R4)

- [ ] API paginada com filtros e tradução para linguagem de negócio
- [ ] Aba Atividade na tela Equipe
- [ ] Testes: só a própria organização, membro recebe 403, campos permitidos

## Passo 6 · Console do superadmin (R5)

- [ ] Eventos, atividade por organização e histórico de um registro
- [ ] Consultas registradas como evento
- [ ] Telas Segurança e Organização
- [ ] Testes e teste ponta a ponta

## Passo 7 · Consumo de IA (R6)

- [ ] Tabela, porta e totais
- [ ] Tela Consumo de IA

## Passo 8 · Exclusão a pedido (R7)

- [ ] Anonimização da pessoa na conta e no histórico
- [ ] Testes: proprietário único recusado, evento registrado

## Passo 9 · Fechamento

- [ ] Política de dados e README atualizados
- [ ] Spec marcada como concluída e fase 5 fechada no README
