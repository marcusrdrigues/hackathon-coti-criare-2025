# Front-end · Portal Criare

SPA em **Angular 21** + **Bootstrap 5** da plataforma de cotações do Hackathon Coti × Criare 2025.

A documentação completa (funcionalidades, regras de negócio, como rodar e API) está no [README principal](../README.md).

```bash
npm install
npm start                      # http://localhost:4200 (a API precisa estar em http://localhost:8085)
npm run build                  # build de produção em dist/
npm test -- --watch=false      # testes unitários (Vitest)
```

A URL da API fica em `src/app/core/api.config.ts`.
