# 0001. Registrar decisões de arquitetura em ADRs

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

O projeto nasceu num hackathon de 24 horas e virou vitrine. Várias escolhas depois disso (autenticação, deploy, banco, testes) não são óbvias lendo só o código. Sem um registro, cada pessoa que chega refaz as mesmas perguntas, ou desfaz uma decisão sem saber por que ela existia.

## Decisão

Registrar cada decisão relevante em `docs/adr/`, em arquivos curtos e numerados, no formato de [template.md](template.md): contexto, decisão, alternativas e consequências.

## Alternativas consideradas

- **Só o README**: ele já explica *o que* o sistema faz. Com o *porquê* de cada escolha, ficaria longo demais para quem só quer rodar o projeto.
- **Wiki do GitHub**: fica fora do repositório e não passa por revisão junto com o código.

## Consequências

- Uma decisão nova de arquitetura vem acompanhada do seu ADR no mesmo PR.
- ADRs não são editados depois de aceitos. Quando a decisão muda, um ADR novo substitui o antigo.
