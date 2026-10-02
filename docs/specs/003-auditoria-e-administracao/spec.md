# 003 · Auditoria e administração

- **Status:** Rascunho
- **Data:** 2026-10-02
- **Plano:** [plan.md](plan.md) · **Tarefas:** [tasks.md](tasks.md)

## Problema

O portal registra o estado atual de cada negócio, mas não a história de como ele chegou ali:

- **Ninguém sabe quem mudou o quê.** Uma cotação editada perde o prazo e o orçamento anteriores. Uma proposta retirada some do banco. Numa organização com várias pessoas, o proprietário não consegue responder "quem da equipe cancelou essa cotação, e quando?".
- **Os eventos de segurança só existem no log.** Login errado, bloqueio por força bruta, sessão revogada por reuso de token, pessoa removida da equipe: tudo isso aparece só em linhas de log, que a hospedagem guarda por pouco tempo e que só fazem sentido para quem desenvolve.
- **O superadmin não tem como investigar.** Ele vê a lista de organizações e mais nada.
- **A IA (fase 7) vai consumir dinheiro por chamada.** Sem um registro de consumo por organização e por funcionalidade desde a primeira chamada, não dá para limitar gasto nem saber o que vale a pena.

## Objetivos

- Guardar o **histórico de alterações** dos dados de negócio e da equipe: quem mudou, quando e qual era o valor anterior.
- Registrar os **eventos de segurança** fora do log, de forma consultável.
- Garantir que esses registros sejam **só de acréscimo**, guardados por **5 anos** e sem credenciais, seguindo a [política de dados](../../dados.md).
- Dar ao **proprietário** a atividade da própria organização, em linguagem de negócio.
- Dar ao **superadmin** um console para investigar a plataforma inteira.
- Deixar o **registro de consumo de IA** pronto antes da primeira chamada.

## Fora do escopo

- **O superadmin alterar dados** (suspender uma organização, redefinir uma senha). Nesta fase, o console é só de leitura. Ações administrativas ganham spec própria.
- **Armazenamento externo imutável** (WORM, serviço de log dedicado). Os registros ficam no mesmo banco, protegidos contra alteração pela aplicação, não contra quem administra o banco.
- **Alertas automáticos** por e-mail ou mensagem. O portal não envia e-mail.
- **Exportação** da auditoria (CSV, PDF).
- **A IA em si**: fase 7. Aqui fica só o registro de consumo.

## Requisitos

### R1 · Histórico de alterações (Deve)

Cada criação, alteração e exclusão de organização, pessoa, vínculo de equipe, convite, cotação, proposta e negociação fica registrada, com quem fez, quando e o estado do registro naquele momento. As mensagens da negociação não entram: elas nunca são editadas, e a própria conversa já é o histórico delas.

**Critérios de aceite**

- **Dado** uma cotação, **quando** a empresa muda o prazo e o orçamento, **então** o histórico guarda os valores anteriores e os novos, a pessoa que mudou, a organização dela e o momento.
- **Dado** uma proposta, **quando** o fornecedor a retira, **então** o histórico guarda o último estado dela e quem a retirou, mesmo com a proposta fora da lista.
- **Dado** uma alteração feita pelo sistema (o reset da demo, uma rotina agendada), **então** o histórico registra que foi o sistema, e não uma pessoa.
- **Dado** qualquer registro do histórico, **então** nele nunca aparece hash de senha, token nem hash de token.

### R2 · Eventos de segurança (Deve)

Os eventos que importam para a segurança ficam registrados com o tipo, o momento, a pessoa e a organização (quando se sabe quem é) e o identificador de rastreio da requisição.

**Critérios de aceite**

- **Dado** um login, **quando** ele dá certo, falha por senha errada ou é bloqueado por excesso de tentativas, **então** um evento é registrado. Na falha com um e-mail que não existe, o e-mail aparece só mascarado.
- **Dado** uma falha que desfaz a operação (senha errada, token reutilizado), **então** o evento continua registrado, mesmo com o resto da operação desfeito.
- **Dado** um refresh token reutilizado, **quando** o sistema revoga todas as sessões da pessoa, **então** um evento registra a revogação.
- **Dado** a equipe, **quando** alguém é convidado, aceita o convite, tem o convite cancelado ou é removido, **então** um evento é registrado.
- **Dado** a configuração do servidor, **quando** o superadmin é criado, tem a senha trocada ou é desativado, **então** um evento é registrado.
- **Dado** um pedido negado por perfil (403), **então** um evento é registrado.
- **Dado** um evento de segurança, **então** ele pode trazer o endereço de rede reduzido de quem fez o pedido (sem o último bloco), nunca o endereço completo. *(Pode)*

### R3 · Só acréscimo e retenção (Deve)

**Critérios de aceite**

- **Dado** o histórico e os eventos, **então** nenhuma rota e nenhum caso de uso altera ou apaga um registro.
- **Dado** o banco de produção, **quando** alguém tenta alterar ou apagar um registro de auditoria por um comando comum, **então** o banco recusa. A exceção é a rotina de retenção.
- **Dado** um registro com mais de 5 anos, **quando** a rotina diária de retenção roda, **então** ele é apagado.
- **Dado** a demo pública, **quando** os dados voltam ao estado inicial, **então** a auditoria das organizações de exemplo também recomeça, e a do superadmin continua.

### R4 · Atividade da organização para o proprietário (Deve)

O proprietário é o administrador da organização: vê o que as pessoas da equipe fizeram, em linguagem de negócio, sem os registros técnicos que servem a quem desenvolve.

**Critérios de aceite**

- **Dado** o proprietário, **quando** ele abre a atividade da organização, **então** vê, das mais recentes para as mais antigas, as ações das pessoas da equipe: quem fez, o quê, em qual cotação, proposta, negociação ou pessoa da equipe, e quando. Exemplo: "Bruno mudou o prazo de 10/10 para 15/10 em *Cadeiras para o escritório*".
- **Dado** uma alteração, **então** a atividade mostra só os campos de negócio que mudaram (título, prazo, orçamento, valor, situação), com o antes e o depois. Ids e campos internos não aparecem.
- **Dado** a atividade, **então** ela não mostra eventos de login, endereços de rede, identificadores de rastreio nem ações de outras organizações, nem mesmo da outra parte de uma negociação.
- **Dado** um membro que não é proprietário, **quando** ele tenta ver a atividade, **então** recebe acesso negado.
- **Dado** a atividade, **quando** o proprietário filtra por pessoa ou por período, **então** a lista mostra só o que corresponde, paginada.

### R5 · Console do superadmin (Deve)

**Critérios de aceite**

- **Dado** o superadmin, **quando** ele abre os eventos de segurança, **então** vê os eventos da plataforma inteira, filtráveis por tipo, organização e período, paginados.
- **Dado** o superadmin, **quando** ele abre uma organização, **então** vê a atividade dela (a mesma do R4) e os eventos de segurança das pessoas dela.
- **Dado** o superadmin, **quando** ele abre o histórico de um registro (uma cotação, por exemplo), **então** vê todas as versões, com o autor e o momento de cada uma.
- **Dado** o console, **então** os e-mails aparecem mascarados e nunca aparece credencial.
- **Dado** o superadmin, **quando** ele consulta a atividade ou os eventos de uma organização, **então** a própria consulta vira um evento de segurança. Quem vigia também é vigiado.
- **Dado** uma pessoa de organização, **quando** ela tenta usar o console, **então** recebe acesso negado.

### R6 · Registro de consumo de IA (Deveria)

**Critérios de aceite**

- **Dado** uma chamada a um modelo de IA, **quando** ela termina, com sucesso ou erro, **então** fica registrado: organização, pessoa, funcionalidade, modelo, tokens de entrada e de saída, custo estimado, latência, resultado e identificador de rastreio. Nunca o texto do prompt nem o da resposta.
- **Dado** o superadmin, **quando** ele abre o consumo de IA, **então** vê os totais por organização, por funcionalidade e por período.
- **Dado** que a IA ainda não existe, **então** o registro e o console funcionam com uma chamada de teste, e a tela mostra o estado vazio enquanto não houver consumo.

### R7 · Exclusão de dados pessoais a pedido (Deveria)

**Critérios de aceite**

- **Dado** um pedido de exclusão de uma pessoa (LGPD, art. 18), **quando** o superadmin o atende, **então** o nome e o e-mail dela são trocados por um marcador anônimo na conta e em todo o histórico, e os negócios da organização continuam íntegros.
- **Dado** o atendimento do pedido, **então** ele é a única alteração permitida em registros antigos, e fica registrado como evento de segurança.
- **Dado** o proprietário da organização, **quando** ele é o único proprietário, **então** o pedido só é atendido depois de outra pessoa assumir a organização, que nunca fica sem proprietário (spec 001, R2).

## Requisitos não funcionais

- **Desempenho:** registrar a auditoria não pode deixar uma ação perceptivelmente mais lenta. As consultas são paginadas e indexadas por organização, pessoa e momento.
- **Segurança:** o isolamento da spec 001 vale aqui. A atividade de outra organização responde 404, e a trava de rota nova cobre as rotas de auditoria.
- **Privacidade:** cada campo segue a [política de dados](../../dados.md). Nenhum dado novo entra na auditoria sem estar classificado ali.
- **Qualidade:** cada critério de aceite vira um teste. O quality gate do SonarCloud continua passando.
- **Compatibilidade:** a demo, o tempo real e os testes ponta a ponta continuam funcionando. O reset diário da demo continua rápido.

## Decisões tomadas

Resolvidas antes da escrita, em 2026-10-02:

1. **Retenção de 5 anos**, o prazo comum para registros comerciais. Depois disso, a rotina diária apaga.
2. **O proprietário vê a atividade da própria organização**, como administrador dela, só com informação de negócio. Os registros técnicos (logins, endereços de rede, rastreio, eventos de segurança) ficam com o superadmin.
3. **Membros comuns não veem a atividade.** Eles continuam vendo a equipe, como hoje.
