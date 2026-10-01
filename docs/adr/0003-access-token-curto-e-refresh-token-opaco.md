# 0003. Access token JWT curto e refresh token opaco em cookie

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

A versão do hackathon não tinha autenticação de verdade: o front-end mandava o id do usuário nas requisições, e qualquer pessoa podia se passar por outra. A nova sessão precisava sobreviver ao F5 sem pedir login de novo a cada poucos minutos, e um script injetado na página (XSS) não podia conseguir roubá-la.

## Decisão

- **Access token**: JWT de 15 minutos, guardado **só em memória** no front-end. Leva apenas `sub` (id) e `tipo` (perfil).
- **Refresh token**: 32 bytes aleatórios (opacos, não JWT), entregues num cookie `HttpOnly`, `SameSite=Strict`, restrito ao caminho `/api/v1/auth` e `Secure` em produção. No banco fica só o hash SHA-256.
- **Rotação**: cada renovação troca o refresh token. Se um token já usado aparecer de novo, todas as sessões daquele usuário são revogadas (detecção de reuso).
- **Força bruta**: 5 senhas erradas em 15 minutos para o mesmo e-mail bloqueiam novas tentativas, com resposta `429` e `Retry-After`.
- No front-end, um interceptor renova o token quando recebe `401`, com uma única renovação em andamento por vez. No carregamento, a sessão é restaurada pelo cookie.

## Alternativas consideradas

- **JWT longo no `localStorage`**: qualquer XSS lê o token, e não há como revogar antes de expirar.
- **Sessão no servidor (`JSESSIONID`)**: é simples, mas prende o estado no servidor e não exercita o modelo de API sem estado que o projeto quer mostrar.
- **Refresh token também em JWT**: sem estado no banco, não dá para revogar no logout nem detectar reuso.

## Consequências

- Um access token roubado vale no máximo 15 minutos. O refresh token não é legível por JavaScript.
- O banco ganha a tabela `tb_refresh_token`, e a revogação passa a ser possível.
- O limite de tentativas fica em memória: com mais de uma instância da API, ele precisa ir para um armazenamento compartilhado, como Redis (está no roadmap).
