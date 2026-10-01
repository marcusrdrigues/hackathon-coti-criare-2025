# 0010. Design tokens e tema que segue o sistema

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

As cores estavam espalhadas: hexadecimais em componentes, classes do Bootstrap que só funcionam no claro (`bg-white`, `bg-light`, `text-dark`) e um vermelho da marca repetido em vários lugares. Isso impedia um modo escuro e tornava qualquer ajuste de cor uma busca pelo projeto inteiro.

## Decisão

- Todas as cores ficam em `frontend/src/styles/tokens.css`, com um valor para o claro (`:root`) e outro para o escuro (`:root[data-bs-theme='dark']`). As variáveis do Bootstrap 5.3 são ligadas aos tokens, e componentes usam só `var(--cor-*)`.
- O tema segue o sistema operacional (`prefers-color-scheme`). A pessoa pode fixar claro ou escuro num seletor, e a escolha fica no `localStorage`.
- Um script no `index.html` aplica o tema antes de a página aparecer, para não piscar. O `TemaService` acompanha as mudanças depois.
- Cada par de texto e fundo tem o contraste medido e anotado (WCAG AA). A marca como preenchimento é a mesma nos dois temas. Como texto, ela clareia no escuro.

## Alternativas consideradas

- **Variáveis Sass do Bootstrap**: exigiria recompilar o Bootstrap e não troca de tema em tempo de execução.
- **Só seguir o sistema, sem seletor**: é o padrão recomendado pelas diretrizes da Apple, mas quem usa o sistema no escuro e prefere ler a aplicação no claro ficaria sem opção.
- **Biblioteca de componentes (Angular Material)**: reescreveria todas as telas e trocaria uma dependência visual por outra.

## Consequências

- Cor nova entra primeiro nos tokens, com os dois valores e o contraste medido. A referência está em [docs/design-system.md](../design-system.md).
- Templates usam classes que acompanham o tema (`bg-body`, `bg-body-tertiary`, `text-body-emphasis`).
