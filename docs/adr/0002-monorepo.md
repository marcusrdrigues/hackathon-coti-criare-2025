# 0002. Back-end e front-end num monorepo

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

No hackathon, a API (Spring Boot) e a SPA (Angular) estavam em repositórios separados. Toda mudança de contrato, como trocar um campo de DTO ou proteger uma rota, exigia dois PRs sincronizados, e o projeto era apresentado como uma coisa só.

## Decisão

Unir os dois num monorepo (`backend/` e `frontend/`), importando os repositórios originais com o histórico preservado. Um único CI compila e testa os dois lados e roda um teste de fumaça da API.

## Alternativas consideradas

- **Manter dois repositórios**: mudanças de contrato continuariam divididas, e o CI não conseguiria validar o par.
- **Ferramenta de monorepo (Nx, Bazel)**: é demais para dois projetos com builds independentes (Maven e npm).

## Consequências

- Uma mudança que atravessa API e tela vira um único commit, revisado de uma vez.
- O deploy continua separado: o Render observa só `backend/**` (`buildFilter` no `render.yaml`), e a Vercel usa `frontend/` como raiz.
