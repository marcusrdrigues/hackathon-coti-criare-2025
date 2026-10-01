# Design system

Guia visual do Portal Criare. As cores ficam todas em [`frontend/src/styles/tokens.css`](../frontend/src/styles/tokens.css), e esta página explica como usá-las.

## Princípios

- **Uma fonte de verdade.** Cor nova entra primeiro nos tokens, com o valor do claro e o do escuro. Componentes usam `var(--cor-*)` e nunca um hexadecimal solto.
- **Claro e escuro desde o início.** Toda tela precisa funcionar nos dois temas. O padrão é seguir o sistema operacional, e a pessoa pode fixar um tema no menu do usuário ou no canto das telas de acesso.
- **Contraste medido, não estimado.** Texto comum tem pelo menos 4,5:1, e bordas de campos e controles pelo menos 3:1 (WCAG 2.x AA). Os valores estão na tabela abaixo.
- **Cor nunca sozinha.** Erro, sucesso e status sempre vêm com texto ou símbolo.
- **Sem emoji na interface.** Ícones vêm do Bootstrap Icons.

## Como o tema funciona

1. Um script curto no `index.html` lê a preferência salva (`localStorage.tema`) ou, se não houver, a do sistema (`prefers-color-scheme`). Ele marca `<html data-bs-theme="light|dark">` antes da página aparecer, então a tela não pisca em branco no modo escuro.
2. O `TemaService` (`core/services/tema.service.ts`) faz a mesma conta com *signals*. Ele acompanha mudanças do sistema e grava a escolha feita no seletor (`app-seletor-tema`).
3. O `tokens.css` define os valores do claro em `:root` e os do escuro em `:root[data-bs-theme='dark']`. No fim do arquivo, as variáveis do Bootstrap 5.3 são ligadas aos tokens. Assim, `.card`, `.dropdown-menu`, `.table`, `.form-control`, `.btn-primary` e as demais classes trocam de tema sozinhas.

## Tokens de cor

Contraste calculado sobre a superfície (`--cor-superficie`), salvo indicação.

### Marca

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `--cor-marca` | `#8f0b11` | `#8f0b11` | Preenchimento de botões e destaques. Branco sobre ele: 9,4:1 |
| `--cor-marca-hover` | `#7a090e` | `#a3121a` | Hover do botão (no escuro, clareia) |
| `--cor-marca-pressionada` | `#5e070c` | `#7a090e` | Estado pressionado |
| `--cor-sobre-marca` | `#ffffff` | `#ffffff` | Texto e ícone sobre a marca |
| `--cor-marca-texto` | `#8f0b11` · 9,4:1 | `#ff8a8f` · 7,5:1 | Marca usada como texto, link, contorno ou anel de foco |
| `--cor-marca-enfase` | `#5e070c` | `#ffb3b6` | Texto forte sobre `--cor-marca-suave` |
| `--cor-marca-suave` | `#fbeceb` | `#3b1013` | Fundo de destaque leve (avatar, mensagem própria) |
| `--cor-marca-foco` | marca a 25% | marca-texto a 35% | Sombra de foco dos campos |

O vermelho-sangue continua o mesmo nos dois temas quando é preenchimento. Quando é texto, ele clareia no escuro, porque `#8f0b11` sobre cinza-escuro não tem contraste.

### Superfícies, texto e linhas

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `--cor-fundo` | `#f5f5f7` | `#000000` | Fundo da página |
| `--cor-superficie` | `#ffffff` | `#1c1c1e` | Cards, barra de navegação, campos, menus |
| `--cor-superficie-2` | `#f2f2f7` | `#2c2c2e` | Bloco dentro de um card, cabeçalho de tabela |
| `--cor-trilho` | `#ececf0` | `#3a3a3c` | Trilho do controle segmentado e das barras de progresso |
| `--cor-segmento-ativo` | `#ffffff` | `#636366` | Segmento selecionado. Texto sobre ele no escuro: 5,5:1 |
| `--cor-texto` | `#1d1d1f` · 16,8:1 | `#f5f5f7` · 15,6:1 | Texto principal |
| `--cor-texto-2` | `#6e6e73` · 5,1:1 | `#98989d` · 5,9:1 | Texto secundário, legendas, placeholders |
| `--cor-borda-campo` | `#8e8e93` · 3,3:1 | `#8e8e93` · 5,2:1 | Borda de campos e controles |
| `--cor-divisoria` | `#e5e5ea` | `#38383a` | Separadores e bordas decorativas |

### Estados

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `--cor-erro` | `#c4320a` · 5,5:1 | `#ff8a65` · 7,4:1 | Borda de campo inválido |
| `--cor-erro-texto` | `#b0300b` · 6,4:1 | `#ff8a65` · 7,4:1 | Mensagem de erro do campo e `.text-danger` |
| `--cor-erro-fundo` / `--cor-erro-fundo-texto` | `#fdf0eb` / `#8a2a08` · 7,8:1 | `#3a1a10` / `#ffb59a` · 9,2:1 | Aviso de erro em bloco |
| `--cor-sucesso` | `#1b7f3b` · 5,1:1 | `#30d158` · 8,4:1 | Requisito cumprido, valores fechados e `.text-success` |

O erro é mais alaranjado que a marca de propósito, para não ser confundido com ela.

### Painel da marca (telas de acesso)

| Token | Claro | Escuro |
|---|---|---|
| `--cor-painel` | `#5e070c` (branco 13,9:1) | `#4a060a` (branco 15,9:1) |
| `--cor-painel-texto-2` | `#f2c8c8` · 9,2:1 | `#f2c8c8` · 10,5:1 |

### Forma

| Token | Valor | Uso |
|---|---|---|
| `--raio` | `10px` | Campos, botões, avisos |
| `--raio-pequeno` | `8px` | Segmentos dentro de um trilho |
| `--altura-controle` | `2.75rem` (44px) | Altura mínima de botões e campos, que é o alvo de toque recomendado |

## Regras para templates

Em HTML, use as classes do Bootstrap que acompanham o tema:

| Em vez de | Use |
|---|---|
| `bg-white` | `bg-body` |
| `bg-light` | `bg-body-tertiary` |
| `text-dark` | `text-body-emphasis` (ou `text-body`) |
| `bg-warning text-dark`, `bg-info text-dark` | `text-bg-warning`, `text-bg-info` |
| `text-warning` sobre `bg-warning-subtle` | `text-warning-emphasis` |

Em CSS de componente, use só `var(--cor-*)`. Se faltar uma cor, crie o token com os dois valores e meça o contraste antes.

## Responsividade

- **Até 959px de largura**, as telas de acesso escondem o painel da marca e mostram a marca compacta no topo.
- **Até 419px**, a marca compacta perde o subtítulo para caber ao lado do seletor de tema.
- **Até 760px de altura**, login e cadastro apertam espaçamentos e escondem textos de apoio, para caberem na tela sem rolagem. Isso foi conferido em 1440×900, 1280×640, 390×844 e 360×640.
- Em telas de toque (`pointer: coarse`), os segmentos do seletor de tema crescem para 44×40px.
- Animações respeitam `prefers-reduced-motion`.
