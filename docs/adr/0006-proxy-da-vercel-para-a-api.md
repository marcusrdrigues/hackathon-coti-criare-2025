# 0006. Front-end e API na mesma origem pelo proxy da Vercel

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

O front-end fica na Vercel e a API no Render, em domínios diferentes. O cookie do refresh token é `SameSite=Strict`, e muitos navegadores bloqueiam cookies de terceiros. Chamar a API direto de outro domínio exigiria `SameSite=None` e ainda assim falharia em parte dos navegadores.

## Decisão

O front-end chama sempre `/api/v1/...` no próprio domínio. O `frontend/vercel.json` reescreve `/api/*` para a API no Render. Para o navegador, o cookie é *first-party*. Em desenvolvimento, o `environment.development.ts` aponta direto para `http://localhost:8085/api/v1`.

## Alternativas consideradas

- **Chamada direta entre domínios com `SameSite=None`**: enfraquece a proteção contra CSRF e quebra em navegadores que bloqueiam cookies de terceiros.
- **Domínio próprio com subdomínios (`app.` e `api.`)**: resolveria o problema, mas exige comprar e configurar um domínio.

## Consequências

- O cookie continua `Strict`, e o front-end não precisa saber o endereço da API em produção.
- O navegador ainda envia `Origin` nas requisições `POST`, então o domínio da Vercel precisa estar em `CORS_ALLOWED_ORIGINS` no Render.
- Cada requisição passa pela borda da Vercel, um salto a mais que é desprezível perto do *cold start* do plano gratuito do Render.
