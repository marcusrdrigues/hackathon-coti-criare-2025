# 001 · Fundação da arquitetura

- **Status:** Em revisão
- **Data:** 2026-10-01
- **Plano:** [plan.md](plan.md) · **Tarefas:** [tasks.md](tasks.md)

## Problema

O projeto nasceu num hackathon de 24 horas e cresceu bem nas fases seguintes (testes, CI, tempo real), mas a base continua a de um protótipo. As próximas fases dependem de coisas que essa base ainda não tem:

- **Uma empresa é um login.** A conta de acesso mora dentro do cadastro da empresa ou do fornecedor. Uma organização real tem várias pessoas, com papéis diferentes, e precisa saber *quem* fez cada ação. A auditoria (fase 5) e o controle de consumo de IA (fase 7) precisam disso.
- **Não existe um perfil de plataforma.** O superadmin, que verá a auditoria de todos, não tem onde existir no modelo atual.
- **O código é organizado por camada técnica** (controllers, services, repositories…). Cada funcionalidade nova se espalha por dez pastas, e nada impede um módulo de mexer nos dados de outro. Isso dificulta crescer com auditoria, IA e modelos de leitura separados (CQRS).
- **Pouca visibilidade em produção.** Os logs são texto livre e não há como seguir uma requisição de ponta a ponta. Sem isso, não dá para medir latência e custo da IA mais tarde.
- **Listagens sem paginação e erros num formato próprio**, em vez de seguir padrões que qualquer cliente conhece.
- **Não há uma política escrita sobre dados**: o que é pessoal, o que é sigilo comercial, o que pode ir para log, auditoria ou IA.

## Objetivos

- Separar **pessoa** (usuário) de **organização** (empresa compradora ou fornecedor), com papéis.
- Ter um **superadmin** que só nasce por configuração do servidor.
- Organizar o back-end em **módulos de negócio** com fronteiras verificadas por teste.
- Garantir, por teste, que **nenhuma organização acessa dados de outra**.
- Conseguir **seguir uma requisição** pelos logs e pela resposta de erro.
- Adotar **paginação** e **Problem Details** (RFC 9457) como padrão da API.
- Escrever a **política de dados** que as fases seguintes vão obedecer.
- Tudo isso **sem perder nada que já funciona**: a demo pública, o tempo real e os testes ponta a ponta continuam passando.

## Fora do escopo

- **Auditoria e console do superadmin**: fase 5, que depende desta.
- **IA**: fase 7.
- **Envio de e-mail** (convite, recuperação de senha). O convite desta fase é por link, que quem convida repassa.
- **Uma pessoa em várias organizações.** O modelo de dados permite, mas a aplicação trata uma organização por pessoa.
- **Banco de leitura separado (CQRS completo)** e **outbox de eventos**: não há carga nem evento crítico que justifique agora. A modularização deixa o caminho aberto.
- **Limite de requisições geral.** Entra junto com a IA, onde o risco de consumo sem limite é real.

## Requisitos

### R1 · Pessoas e organizações (Deve)

Uma **organização** é a empresa compradora ou o fornecedor, com razão social e CNPJ. Um **usuário** é uma pessoa, com nome, e-mail e senha. Um usuário pertence a uma organização com um **papel**:

- **Proprietário**: quem criou a conta. Opera tudo e gerencia a equipe.
- **Membro**: opera cotações, propostas e negociações, mas não gerencia a equipe.

**Critérios de aceite**

- **Dado** o formulário de cadastro, **quando** alguém informa os dados da organização e os seus (nome, e-mail, senha), **então** a organização e o usuário proprietário são criados juntos, ou nenhum dos dois.
- **Dado** um CNPJ já cadastrado, **quando** alguém tenta cadastrá-lo de novo, **então** o cadastro é recusado com a mesma mensagem genérica de hoje.
- **Dado** um usuário logado, **quando** a sessão é criada, **então** o token identifica a pessoa, a organização, o tipo da organização e o papel, sem nenhum dado pessoal.
- **Dado** que uma ação foi feita (publicar cotação, enviar proposta, mandar mensagem), **então** fica registrado qual pessoa a fez, além da organização.
- **Dado** uma mensagem na negociação, **quando** ela aparece na tela, **então** mostra o nome da pessoa e da organização.
- **Dado** os dados que já existem em produção, **quando** a nova versão sobe, **então** cada empresa e cada fornecedor viram uma organização com o seu proprietário, e cotações, propostas e negociações continuam ligadas a eles.

### R2 · Equipe da organização (Deve)

**Critérios de aceite**

- **Dado** um proprietário, **quando** ele convida alguém informando nome e e-mail, **então** o sistema gera um link de convite de uso único, válido por 72 horas, para ele repassar.
- **Dado** um link de convite válido, **quando** a pessoa o abre e define a senha, **então** ela entra na organização como membro e já fica logada.
- **Dado** um link vencido, já usado ou adulterado, **quando** alguém o abre, **então** recebe uma mensagem clara e nenhuma conta é criada.
- **Dado** um proprietário, **quando** ele remove um membro, **então** as sessões desse membro deixam de valer na hora, e o que ele fez continua registrado com o nome dele.
- **Dado** um membro, **quando** ele tenta convidar ou remover alguém, **então** recebe acesso negado.
- **Dado** uma organização, **então** ela nunca fica sem proprietário.

### R3 · Superadmin (Deve)

**Critérios de aceite**

- **Dado** o servidor configurado com as credenciais do superadmin, **quando** ele sobe, **então** o superadmin existe. Sem a configuração, não existe.
- **Dado** qualquer tela ou rota pública, **então** não há como se cadastrar ou se promover a superadmin.
- **Dado** o superadmin logado, **quando** ele acessa a área administrativa, **então** vê a lista de organizações com o total de usuários e de cotações de cada uma, sem poder alterar nada nesta fase.
- **Dado** um usuário comum, **quando** ele tenta a área administrativa, **então** recebe acesso negado.
- **Dado** a demo pública, **então** não existe login de demonstração como superadmin.

### R4 · Isolamento entre organizações (Deve)

**Critérios de aceite**

- **Dado** um recurso de outra organização (cotação, proposta, negociação, mensagem), **quando** alguém tenta acessá-lo pelo id, **então** recebe **404**, como se não existisse, para não revelar que o id é válido.
- **Dado** os membros de uma mesma organização, **então** todos enxergam e operam os dados dela, conforme o papel.
- **Dado** uma rota nova da API que recebe um id, **quando** ela não tem teste de isolamento, **então** o build falha.

### R5 · Módulos com fronteiras (Deve)

**Critérios de aceite**

- **Dado** o back-end, **então** o código fica organizado por módulo de negócio (identidade, cotação, proposta, negociação, tempo real, demonstração) mais uma parte compartilhada.
- **Dado** um módulo, **quando** ele usa outro, **então** só usa o que o outro expõe publicamente ou reage aos eventos dele. Um teste falha se essa regra for quebrada.
- **Dado** a reorganização, **então** nenhum comportamento muda: todos os testes existentes continuam passando.

### R6 · Rastreabilidade das requisições (Deve)

**Critérios de aceite**

- **Dado** uma requisição, **então** todas as linhas de log dela têm o mesmo identificador de rastreio, e a resposta devolve esse identificador.
- **Dado** um erro, **quando** a API responde, **então** o corpo traz o identificador de rastreio, para quem relatar o problema.
- **Dado** o ambiente de produção, **então** os logs saem em JSON estruturado.
- **Dado** qualquer log, **então** nele nunca aparecem senha, tokens ou o conteúdo de mensagens e propostas, e e-mails aparecem mascarados.

### R7 · Padrões da API (Deveria)

**Critérios de aceite**

- **Dado** uma listagem, **quando** o cliente pede uma página, **então** recebe os itens e o total, com tamanho máximo de 50 por página e ordenação só pelos campos permitidos.
- **Dado** qualquer erro, **então** a resposta segue o formato Problem Details (RFC 9457), com os erros de validação por campo.
- **Dado** o front-end, **então** as telas continuam funcionando com os novos formatos, com paginação onde a lista pode crescer.

### R8 · Política de dados (Deveria)

**Critérios de aceite**

- **Dado** o repositório, **então** existe um documento que classifica os dados do sistema (credenciais, pessoais, empresariais públicos e comerciais sigilosos) e diz, para cada classe, o que pode ir para logs, auditoria, IA e dados de demonstração, e por quanto tempo é guardado.
- **Dado** as fases 5 e 7, **então** elas seguem esse documento.

## Requisitos não funcionais

- **Compatibilidade:** a demo pública, os logins de demonstração e o tempo real continuam funcionando. As contas de demonstração ganham uma segunda pessoa, para mostrar a equipe.
- **Segurança:** seguir o OWASP API Security Top 10, com atenção especial a acesso a objeto de outra organização (API1), autenticação (API2) e controle por função (API5). Tokens de convite são guardados só como hash, como o refresh token.
- **Qualidade:** o quality gate do SonarCloud continua passando, com pelo menos 80% de cobertura no código novo.
- **Entrega:** em passos pequenos, cada um com o CI verde e o roadmap atualizado.

## Decisões em aberto

1. **Nome dos tipos de organização.** Manter **Empresa** e **Fornecedor**, que já são a linguagem das telas e da API, em vez de termos novos como "compradora" e "fornecedora". *Recomendação: manter.*
2. **404 para recurso de outra organização** em vez do 403 de hoje. *Recomendação: 404.* Evita que alguém descubra ids válidos por tentativa.
3. **Convite por link** que o proprietário repassa, até existir envio de e-mail. *Recomendação: link.* O fluxo é o mesmo que o e-mail vai usar depois.
4. **Problem Details** muda o formato dos erros da API. Hoje o único cliente é o nosso front-end, que será ajustado no mesmo passo. *Recomendação: adotar agora*, enquanto é barato.
