# 0018. Liquid Glass só na camada que flutua

- **Status:** Aceita; a barra lateral flutuante foi substituída pelo [ADR 0019](0019-barra-lateral-encostada-e-recolhivel.md)
- **Data:** 2026-10-02
- **Complementa:** [ADR 0010](0010-design-tokens-e-tema-do-sistema.md) e [ADR 0011](0011-componentes-proprios-no-lugar-do-bootstrap.md)

## Contexto

O visual do portal segue as interfaces da Apple desde o redesenho. Com o iOS 26, a Apple passou a usar o Liquid Glass: barras e controles de vidro que flutuam sobre o conteúdo, com a barra de abas em cápsula, afastada da borda da tela. O portal é usado principalmente no celular, e a barra de abas encostada na borda já parecia de outra geração.

As diretrizes da Apple (Human Interface Guidelines, páginas *Materials*, *Tab bars* e *Color*) são claras sobre os limites: o vidro pertence só à camada funcional (barras, menus, folhas, alertas), nunca ao conteúdo; deve ser usado com moderação; e precisa de uma versão sólida quando a pessoa pede menos transparência ou mais contraste.

## Decisão

- **Vidro só no que flutua:** barra de abas e topo no celular, barra lateral no computador, menus, listas de opção, painéis, diálogos e avisos. Listas, cartões e o fundo das telas continuam sólidos.
- **Barra de abas em cápsula**, afastada das bordas e acima da área segura, com a aba selecionada numa "lente" mais densa e a cor da marca só no ícone e no rótulo dela. Atrás da cápsula, uma faixa esmaece o conteúdo que rola (o efeito de borda de rolagem), em vez de o conteúdo bater na barra.
- **Barra lateral e painéis** viram blocos de vidro afastados das bordas, com cantos arredondados (`--raio-vidro`). A folha do celular sobe de baixo também afastada das bordas.
- **Tokens, não valores soltos:** `--vidro-fino` (barras pequenas), `--vidro-espesso` (superfícies com texto), `--vidro-menu` (quase opaco, porque o menu abre dentro de outra barra de vidro, onde o desfoque não alcança o conteúdo), `--vidro-filtro` (`blur(24px) saturate(180%)`), `--vidro-brilho`, `--vidro-contorno` e `--vidro-sombra`.
- **A marca mora no conteúdo:** um brilho discreto do vermelho da marca no fundo das telas, que o vidro recolhe ao passar por cima. Os controles continuam monocromáticos; a cor fica para a ação principal e a aba selecionada.
- **Botões em cápsula**, como no iOS 26.
- **Acessibilidade primeiro:** com `prefers-reduced-transparency`, `prefers-contrast: more` ou navegador sem `backdrop-filter`, os tokens de vidro viram a cor sólida das superfícies flutuantes (e o contorno fica mais forte no alto contraste). Os alvos de toque continuam com no mínimo 44px.

## Alternativas consideradas

- **Vidro em tudo (cartões, listas):** é o "glassmorphism" de vitrine. A Apple desaconselha explicitamente: confunde a hierarquia e pesa no desempenho, porque cada `backdrop-filter` recalcula o que está atrás a cada quadro de rolagem.
- **Manter as barras sólidas e só arredondar:** perde o que torna o estilo atual reconhecível (o conteúdo passando por baixo das barras).

## Consequências

- O espaço reservado para a barra de abas (`--altura-abas`) passa a incluir a folga até a borda; telas que se apoiam nele (a sala de negociação e os avisos) continuam corretas.
- Navegadores sem `backdrop-filter` (raros hoje) mostram as barras sólidas, sem perda de função.
- Toda superfície flutuante nova usa os tokens de vidro; uma superfície de conteúdo nova nunca usa.
