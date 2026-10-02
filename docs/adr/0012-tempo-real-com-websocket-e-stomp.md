# 0012. Tempo real com WebSocket e STOMP

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

A sala de negociação consultava a API a cada 10 segundos para descobrir mensagens novas. Isso atrasa a conversa, gasta requisições à toa e não permite recursos como "digitando…" ou avisos de proposta nova em qualquer tela. Para a negociação parecer uma conversa, as atualizações precisam chegar na hora.

Restrições do deploy: o front-end fica na Vercel e chama a API pelo proxy dela ([ADR 0006](0006-proxy-da-vercel-para-a-api.md)), que não repassa conexões WebSocket. A API roda no Render, que aceita WebSocket.

## Decisão

- **STOMP sobre WebSocket** com o broker simples do Spring (`/topic` para a negociação, `/queue` para avisos pessoais, `/app` para o que o cliente envia).
- O navegador conecta **direto na API** (`wss://…onrender.com/ws`), sem o proxy da Vercel. O handshake só aceita as origens do `CORS_ALLOWED_ORIGINS`.
- **Autenticação no frame CONNECT**, com o mesmo access token JWT da API (`Authorization: Bearer …`). A identidade fica na sessão e o nome do usuário é o id dele, que endereça a fila pessoal.
- **Autorização no SUBSCRIBE e no SEND**: só as duas partes assinam `/topic/negociacoes/{id}` e enviam "digitando"; avisos só pela fila pessoal (`/user/queue/avisos`).
- **Eventos de domínio depois do commit**: os services publicam `MensagemEnviadaEvento`, `NegociacaoAlteradaEvento` e `PropostaRecebidaEvento`; o `NotificadorTempoReal` escuta com `@TransactionalEventListener` e só envia depois que a transação foi gravada. Uma falha no envio é registrada no log e não desfaz a operação.
- O que já existia por REST continua igual: o tempo real só avisa e entrega os dados novos.
- **Mensagens não lidas por marca de leitura**: cada negociação guarda quando cada parte a abriu pela última vez (`lida_empresa_em`, `lida_fornecedor_em`, migração V2). Não lidas são as mensagens da outra parte enviadas depois disso, contadas numa consulta só para a lista inteira. Abrir a conversa (`PATCH /negociacoes/{id}/leitura`) ou responder nela atualiza a marca. O front soma os avisos que chegam e volta a buscar os totais na API depois de uma queda da conexão.

## Alternativas consideradas

- **Continuar consultando (polling)**: simples, mas atrasa até 10 segundos e multiplica requisições por usuário aberto.
- **Server-Sent Events**: bom para o servidor empurrar dados, mas só num sentido. "Digitando…" exigiria uma requisição HTTP por sinal. O `EventSource` também não envia o cabeçalho `Authorization`, e o token teria de ir na URL.
- **WebSocket puro, sem STOMP**: exigiria inventar um protocolo de assinatura, roteamento e confirmação. O STOMP já resolve isso e tem cliente pronto para o navegador.
- **Broker externo (RabbitMQ, Redis)**: necessário com várias instâncias da API. Com uma instância, o broker em memória basta.

## Consequências

- Mensagens, ofertas, status e avisos chegam na hora, e "digitando…" passa a existir.
- Uma marca de leitura por parte, e não uma por mensagem: não há "visto" individual, mas a contagem é barata e não cresce com o histórico.
- Com mais de uma instância da API, cada uma teria o seu broker em memória: será preciso um broker externo (relay STOMP com RabbitMQ) ou Redis pub/sub. O mesmo vale para o limite de tentativas de login ([ADR 0003](0003-access-token-curto-e-refresh-token-opaco.md)).
- O token vale 15 minutos, mas a sessão STOMP continua aberta depois disso; ao reconectar, o cliente renova o token antes.
- No plano gratuito do Render a API dorme sem uso, e a conexão só volta quando ela acorda. O cliente reconecta sozinho, e a tela continua funcionando pelo REST enquanto isso.
