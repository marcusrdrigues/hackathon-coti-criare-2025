# 0015. Pessoas e organizações separadas

- **Status:** Aceita
- **Data:** 2026-10-02

## Contexto

Desde o hackathon, a conta de acesso morava dentro de `tb_empresa` e `tb_fornecedor`: e-mail e senha eram colunas da empresa. Uma empresa era um login. Isso impedia o que a fase 4 pede (spec 001, R1):

- duas pessoas da mesma empresa não conseguiam trabalhar juntas sem dividir a senha;
- não havia como saber quem, dentro da empresa, criou uma cotação ou escreveu uma mensagem;
- convites, papéis e a auditoria da fase 5 não tinham onde se apoiar.

## Decisão

Três tabelas novas no módulo `identidade`, criadas pela migração V3:

| Tabela | O que é |
|---|---|
| `tb_organizacao` | A empresa compradora ou o fornecedor: tipo, razão social e CNPJ (só dígitos). O CNPJ é único **por tipo**, porque a mesma empresa pode comprar e também fornecer |
| `tb_usuario` | A pessoa que entra no sistema: nome, e-mail (único na plataforma) e hash BCrypt da senha |
| `tb_membro` | A pessoa numa organização, com um papel: `PROPRIETARIO` ou `MEMBRO` |

- **Cadastro único** em `POST /api/v1/cadastro`: a organização e a pessoa proprietária nascem na mesma transação. As rotas `/empresas` e `/fornecedores` saem.
- **Token**: `sub` é a pessoa; as claims `org`, `tipo` e `papel` dizem em nome de quem ela age. O Spring Security recebe `ROLE_EMPRESA`/`ROLE_FORNECEDOR` e `ROLE_PROPRIETARIO`/`ROLE_MEMBRO`. Nenhum dado pessoal vai no token.
- **Posse e autoria**: cotações, propostas e negociações continuam pertencendo à organização (`empresa_id`, `fornecedor_id`). A pessoa fica registrada em `criada_por` (cotação), `enviada_por` (proposta) e `remetente_id` (mensagem).
- **Tempo real**: o principal da conexão STOMP é a organização, então os avisos chegam a toda a equipe conectada.
- **Dados existentes preservados**: cada empresa e cada fornecedor vira uma organização com o **mesmo id**, e ganha um proprietário também com esse id. Tudo o que guardava o id da conta (remetente das mensagens, dono das sessões) continua apontando para o lugar certo sem conversão. As sessões antigas são encerradas, porque o token novo precisa das claims novas.
- `tb_empresa`, `tb_fornecedor` e `tb_perfil` ficam sem uso e só saem numa migração V4, depois da validação em produção.

Verificação: `MigracaoPessoasEOrganizacoesTest` leva um PostgreSQL até a V2, grava dados no formato antigo e confere o resultado da V3 (ids, CNPJ, senha, autoria, chaves estrangeiras e sessões encerradas).

## Alternativas consideradas

- **Renomear para Usuário/Conta e eliminar Empresa/Fornecedor do vocabulário**: o domínio fala em empresa e fornecedor, e as telas também. Os nomes ficam; o que muda é que "empresa" passa a ser um tipo de organização.
- **Uma pessoa em várias organizações já agora**: `tb_membro` permite, mas o login escolhe o único vínculo ativo. Trocar de organização na mesma sessão fica para quando houver demanda.
- **Migrar criando ids novos**: exigiria reescrever o remetente de todas as mensagens e o dono das sessões, com mais chance de erro e nenhum ganho.

## Consequências

- A tela de cadastro pede a razão social e o nome da pessoa. O menu da conta mostra a pessoa, a organização e o papel.
- Na negociação, as mensagens ficam do lado da organização de quem escreveu e mostram o nome da pessoa.
- Quem estava logado na versão anterior precisa entrar de novo uma vez.
- O passo 5 da spec usa a organização do token para a regra de posse única (404 para recurso de outra organização), e o passo 6 usa `tb_membro` para convites e remoção de membros.
