# 0011. Componentes próprios no lugar do Bootstrap

- **Status:** Aceita
- **Data:** 2026-10-01
- **Complementa:** [ADR 0010](0010-design-tokens-e-tema-do-sistema.md), que ligava o Bootstrap aos tokens

## Contexto

Mesmo com os tokens de cor, as telas internas tinham "cara de template": card dentro de card, borda colorida, círculo com ícone em cada número, rótulos em caixa alta, pílula para todo status e ícone em todo botão. Boa parte disso vem dos padrões do Bootstrap e de como ele convida a montar telas. O projeto quer um visual sóbrio e premium, no padrão das interfaces da Apple, em que a hierarquia vem da tipografia e do espaço.

Além disso, o Bootstrap trazia peso (CSS, JavaScript do dropdown e do collapse, fonte de ícones por CDN) e componentes que não seguiam o tema nem as regras de acessibilidade que o projeto adotou.

## Decisão

- Remover o Bootstrap, o JavaScript dele e o Bootstrap Icons.
- Base própria em CSS sobre os tokens: `base.css` (normalização e tipografia), `controles.css` (botões, campos, listas agrupadas, métricas, status, notas) e `acesso.css` (login e cadastro).
- Componentes com comportamento em `app/ui`, seguindo os padrões da WAI-ARIA: seletor (listbox), menu, controle segmentado (rádios nativos), painel (`<dialog>` modal), confirmação (`alertdialog`, no lugar do `confirm()`), status e um conjunto próprio de ícones em SVG.
- Estrutura das telas internas num componente só (`app-shell`): barra lateral no computador e barra de abas no celular. As páginas não repetem mais a navbar.

## Alternativas consideradas

- **Manter o Bootstrap e só trocar as classes**: resolveria parte do visual, mas o dropdown, o collapse e os formulários continuariam com o comportamento e o peso dele.
- **Angular Material**: tem componentes acessíveis, mas o visual é o do Material Design, o oposto do que se busca, e personalizar a fundo dá mais trabalho que construir.
- **Angular CDK + CSS próprio**: o CDK (overlay, a11y) ajudaria no posicionamento de menus. Para os poucos componentes necessários, o `<dialog>` nativo e um posicionamento simples resolvem sem dependência nova. Fica como opção se aparecerem casos mais complexos.

## Consequências

- O bundle inicial caiu de cerca de 607 KB para cerca de 320 KB (126 KB → 90 KB transferidos).
- Peça nova sai do catálogo em [docs/design-system.md](../design-system.md). Se não existir, ela é criada em `app/ui` ou em `controles.css`, nunca com estilos soltos numa página.
- Os componentes próprios precisam de testes próprios: seletor, segmentado e confirmação têm testes unitários, e os fluxos principais passam pelos testes ponta a ponta.
