# 004 · Plano técnico

Como a [spec 004](spec.md) vira código.

## API

| Rota | Corpo | Resposta |
|---|---|---|
| `POST /api/v1/auth/redefinicao` | `{email}` | Sempre `202`, sem corpo |
| `POST /api/v1/auth/redefinicao/consulta` | `{token}` | `204` se o link vale; `404` se não vale (um motivo só) |
| `POST /api/v1/auth/redefinicao/confirmacao` | `{token, senha}` | `204`; `400` para senha fora das regras; `404` para link que não vale |

- As três são públicas e ficam na lista de rotas públicas da configuração de segurança.
- O token vai sempre no corpo, nunca na URL, como no convite.

## Identidade

- Entidade `RedefinicaoDeSenha` (`tb_redefinicao_senha`, migração V8): pessoa, hash do token, criada em, expira em, usada em, substituída em. Sem `@Audited`: é uma credencial temporária.
- `RedefinicaoDeSenhaService`:
  - **pedir(email):** normaliza o e-mail; se a conta existe, está ativa, tem vínculo ativo, não é superadmin, não é conta de exemplo e não passou do limite, invalida os links anteriores, grava o novo e publica `RedefinicaoPedida` com o token. Em qualquer outro caso, não faz nada. O retorno é sempre o mesmo.
  - **consultar(token)** e **confirmar(token, senha):** buscam pelo hash, conferem prazo e uso, trocam o hash da senha (`Usuario.trocarSenha`), marcam o link como usado, revogam todas as sessões e zeram as tentativas de login.
- **Contas de exemplo:** a identidade não conhece o módulo da demo. Uma propriedade lista os domínios que não recebem e-mail (`app.redefinicao.dominios-sem-envio`, com `demo.com` no perfil da demo).
- **Limite de 3 pedidos por hora por e-mail:** contador em memória, como o das tentativas de login, e pelo mesmo motivo (uma instância só).

## Envio de e-mail

- Porta `EnvioDeEmail` na aplicação da identidade, com uma mensagem simples (para, assunto, texto, HTML).
- **O envio acontece depois de confirmar a transação e fora da requisição** (`@TransactionalEventListener(AFTER_COMMIT)` com `@Async`): a resposta sai no mesmo tempo com conta ou sem, e uma falha no provedor não desfaz nada.
- Adaptadores na infraestrutura:
  - `EmailPelaBrevo`: `POST` na API HTTP da Brevo com o `RestClient`, chave e remetente por variável de ambiente (`BREVO_API_KEY`, `EMAIL_REMETENTE`, `EMAIL_REMETENTE_NOME`). Ativo quando a chave existe.
  - `EmailDesligado`: o padrão sem chave. Registra no log que o envio está desligado, só com o e-mail mascarado.
- O link é montado com o endereço do front-end (`APP_URL_FRONTEND`) e o token no fragmento: `/redefinir-senha#token=...`. O fragmento não sai do navegador, então não chega a servidor, proxy nem log.
- Modelo do e-mail em texto e HTML simples, sem imagens externas nem rastreamento.

## Eventos de segurança

Dois tipos novos em `EventoDeSeguranca.Tipo`: `REDEFINICAO_DE_SENHA_PEDIDA` (só para conta que recebe o e-mail) e `SENHA_REDEFINIDA`.

## Front-end

- Link "Esqueci minha senha" no formulário de login, abaixo da senha.
- `/esqueci-a-senha`: no layout das telas de acesso; pede o e-mail e mostra a mesma confirmação em qualquer caso.
- `/redefinir-senha`: lê o token do fragmento e o apaga da barra de endereço (`history.replaceState`), consulta, e mostra senha nova e confirmação, com as regras do cadastro. Link que não vale mostra o aviso e o atalho para pedir outro.
- Ao concluir, vai para o login com o aviso de senha alterada.

## Testes

- API: resposta igual com e sem conta, conta de exemplo e superadmin sem e-mail, limite por hora, link vencido, usado e substituído, troca que derruba as sessões e zera o bloqueio, senha fora das regras sem gastar o link, eventos registrados, token fora do banco e dos logs.
- Adaptador da Brevo com servidor simulado (`MockRestServiceServer`): corpo, cabeçalho da chave e falha do provedor.
- Front-end: as duas telas e o link no login; teste ponta a ponta do fluxo com o e-mail capturado.

## Riscos

| Risco | Como tratar |
|---|---|
| E-mail cair no spam (remetente sem domínio próprio) | Texto simples e sem links além do de redefinição; a tela diz para conferir o spam. Com domínio próprio, configurar SPF e DKIM |
| Esgotar a cota diária do provedor | Limite por e-mail; a cota de 300 por dia sobra para o porte atual |
| Alguém descobrir e-mails pelo tempo de resposta | Envio assíncrono depois da resposta, em todos os casos |
| Token vazar por log ou cabeçalho `Referer` | Token só no fragmento e no corpo, nunca na URL; a tela de redefinição apaga o fragmento ao abrir |
