# 🚀 Deploy da demo pública

Guia passo a passo para publicar o projeto de graça, usando três serviços:

| Parte | Onde | Por quê |
|---|---|---|
| Banco PostgreSQL | [Neon](https://neon.tech) | PostgreSQL gerenciado com plano gratuito que não expira |
| API Spring Boot | [Render](https://render.com) | Roda o `Dockerfile` do back-end direto do GitHub |
| Front-end Angular | [Vercel](https://vercel.com) | Hospeda o build estático e repassa `/api/*` para a API |

## Como as peças se conectam

```mermaid
flowchart LR
    N[Navegador] -->|"https://seu-app.vercel.app"| V[Vercel<br/>Angular]
    V -->|"/api/* (rewrite)"| R[Render<br/>Spring Boot]
    R -->|JDBC + SSL| DB[(Neon<br/>PostgreSQL)]
```

O navegador só conversa com o domínio da Vercel. Quando o Angular chama `/api/v1/...`, a Vercel repassa a chamada para o Render. Isso traz duas vantagens:

- **O cookie da sessão é do próprio site.** O refresh token usa `SameSite=Strict`, e com front e API no mesmo domínio o navegador o envia normalmente.
- **Não há chamada entre domínios no navegador.** Mesmo assim, a API precisa conhecer o endereço da Vercel (variável `CORS_ALLOWED_ORIGINS`), porque o navegador manda o header `Origin` nas requisições e a Vercel o repassa.

> Os planos gratuitos têm limitações: a API do Render "dorme" depois de 15 minutos sem uso e leva cerca de 1 minuto para acordar. A tela de login avisa o visitante quando isso acontece.

---

## 1. Banco de dados no Neon

1. Crie uma conta em [neon.tech](https://neon.tech) (dá para entrar com o GitHub).
2. Crie um projeto:
   - **Name:** `portal-criare`
   - **Region:** `AWS US East (N. Virginia)`, perto da região do Render que vamos usar
3. No painel do projeto, clique em **Connect**. O Neon mostra uma string neste formato:

   ```text
   postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require&channel_binding=require
   ```

4. Separe as partes em três variáveis. O Spring não aceita usuário e senha dentro da URL, e o driver Java usa o prefixo `jdbc:`:

   | Variável | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | o `USUARIO` (por padrão `neondb_owner`) |
   | `SPRING_DATASOURCE_PASSWORD` | a `SENHA` (o trecho entre `:` e `@`) |

   Dois ajustes no `HOST`:
   - **Tire o `-pooler` do nome**, se aparecer (ex.: `ep-nome-123-pooler.us-east-1...` vira `ep-nome-123.us-east-1...`). O pooler do Neon serve para apps sem pool de conexões; a API já tem o Hikari, e a conexão direta evita problemas com os *prepared statements* do Hibernate.
   - O `channel_binding=require` pode ficar de fora: `sslmode=require` já garante a conexão criptografada.

Não é preciso criar tabelas: a API cria o esquema na primeira inicialização.

## 2. API no Render

1. Crie uma conta em [render.com](https://render.com) entrando com o GitHub.
2. Clique em **New → Blueprint** e selecione o repositório `hackathon-coti-criare-2025`.
3. O Render lê o arquivo [`render.yaml`](../render.yaml) da raiz e mostra o serviço `portal-criare-api`. Ele pede os valores marcados como secretos:

   | Variável | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | a URL JDBC do passo 1.4 |
   | `SPRING_DATASOURCE_USERNAME` | o usuário do Neon |
   | `SPRING_DATASOURCE_PASSWORD` | a senha do Neon |
   | `CORS_ALLOWED_ORIGINS` | por enquanto `https://localhost`; você troca no passo 4 |

   O `JWT_SECRET` é gerado pelo próprio Render (256 bits aleatórios), e o profile `demo` já vem ligado.
4. Clique em **Apply**. O primeiro build leva alguns minutos (o Maven baixa as dependências).
5. Quando o status ficar **Live**, anote o endereço do serviço, por exemplo `https://portal-criare-api.onrender.com`, e teste:
   - `https://portal-criare-api.onrender.com/actuator/health` deve responder `{"status":"UP"}`
   - `https://portal-criare-api.onrender.com/swagger-ui.html` abre a documentação

## 3. Front-end na Vercel

1. Abra o arquivo [`frontend/vercel.json`](../frontend/vercel.json) e troque `SEU-BACKEND.onrender.com` pelo endereço do passo 2.5. Faça o commit e o push.

   ```json
   "destination": "https://portal-criare-api.onrender.com/api/:caminho*"
   ```

2. Crie uma conta em [vercel.com](https://vercel.com) entrando com o GitHub.
3. Clique em **Add New → Project** e importe o repositório.
4. Em **Root Directory**, escolha `frontend`. O resto a Vercel lê do `vercel.json` (comando de build e pasta de saída).
5. Clique em **Deploy**. Ao terminar, anote o endereço, por exemplo `https://portal-criare.vercel.app`.

## 4. Ligar o front-end à API

1. No Render, abra o serviço `portal-criare-api` → **Environment**.
2. Troque `CORS_ALLOWED_ORIGINS` pelo endereço da Vercel (sem barra no final), por exemplo `https://portal-criare.vercel.app`.
3. Salve. O Render reinicia a API sozinho.

## 5. Testar

1. Abra o endereço da Vercel. A tela de login deve mostrar os botões **Entrar como empresa** e **Entrar como fornecedor**.
2. Clique em um deles. Se a API estava dormindo, aparece o aviso de que o servidor está iniciando.
3. Recarregue a página (F5): você deve continuar logado. Isso confirma que o cookie da sessão está funcionando pelo proxy da Vercel.

## 6. Divulgar

Com tudo funcionando, adicione o link no topo do README, logo abaixo dos badges:

```html
<p align="center">
  <a href="https://portal-criare.vercel.app"><b>🔗 Testar a demo</b></a> ·
  <a href="https://portal-criare-api.onrender.com/swagger-ui.html">Documentação da API</a>
</p>
```

---

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| Login responde `403 Invalid CORS request` | `CORS_ALLOWED_ORIGINS` diferente do endereço da Vercel | Copie o endereço exato, com `https://` e sem barra no final |
| Tudo funciona, mas o F5 desloga | Cookie não está chegando à API | Confirme que o front chama `/api/v1` (build de produção) e que o `vercel.json` aponta para o Render |
| API não sobe no Render | Variáveis do banco erradas | Veja os logs do serviço; a URL precisa começar com `jdbc:postgresql://` e terminar com `?sslmode=require` |
| Primeira requisição demora cerca de 1 minuto | Plano gratuito do Render dorme sem uso | Comportamento esperado; a tela de login avisa o visitante |
| Dados da demo bagunçados por visitantes | Uso normal da demo pública | Os dados voltam ao estado inicial todo dia às 4h (variável `DEMO_RESET_CRON`) |
