# Design system

Guia visual do Portal Criare. O front-end não usa biblioteca de componentes: os valores ficam em [`frontend/src/styles/tokens.css`](../frontend/src/styles/tokens.css), as peças em CSS em [`controles.css`](../frontend/src/styles/controles.css) e as peças com comportamento em [`frontend/src/app/ui/`](../frontend/src/app/ui). Esta página explica como usar cada uma.

## Princípios

- **Hierarquia pela tipografia e pelo espaço, não por caixas.** Sem card dentro de card, sem borda colorida e sem sombra em tudo. O conteúdo fica direto no fundo, e o que é lista vira lista agrupada, como nos Ajustes do iPhone.
- **Um ponto de cor.** A interface é em tons de cinza. O vermelho-sangue aparece na ação principal, no item ativo e no que é urgente. Status é um ponto colorido com texto, não uma pílula.
- **Ícones só onde ajudam.** Na navegação, em botões sem texto (sempre com `aria-label`) e em avisos. Nunca como enfeite ao lado de um título.
- **Texto direto.** Botões dizem o que fazem, com verbo e objeto ("Publicar cotação", "Cancelar convite", "Retirar proposta"), e trazem o valor quando há dinheiro em jogo ("Enviar oferta de R$ 54.000,00", "Fechar por R$ 55.000,00"). O botão de confirmação repete a ação do título ("Encerrar sem acordo?" → "Encerrar sem acordo"), nunca "OK" ou "Sim". Frases normais, sem CAIXA ALTA.
- **Claro e escuro desde o início.** Toda tela funciona nos dois temas. O padrão é seguir o sistema, e a pessoa pode fixar um tema no menu da conta ou no canto das telas de acesso.
- **Contraste medido.** Texto com pelo menos 4,5:1 e bordas de controles com pelo menos 3:1 (WCAG 2.x AA).
- **Teclado e leitor de tela.** Todo controle funciona sem mouse, com foco visível e o papel ARIA certo.
- **Sem emoji na interface.**

## Como o tema funciona

1. Um script curto no `index.html` lê a preferência salva (`localStorage.tema`) ou, se não houver, a do sistema (`prefers-color-scheme`). Ele marca `<html data-theme="light|dark">` antes de a página aparecer, então a tela não pisca em branco no modo escuro.
2. O `TemaService` (`core/services/tema.service.ts`) faz a mesma conta com *signals*, acompanha mudanças do sistema e grava a escolha feita no seletor (`app-seletor-tema`).
3. O `tokens.css` define os valores do claro em `:root` e os do escuro em `:root[data-theme='dark']`.

## Tokens

### Cores

Contraste calculado sobre a superfície (`--cor-superficie`), salvo indicação.

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `--cor-marca` | `#8f0b11` | `#8f0b11` | Preenchimento da ação principal. Branco sobre ele: 9,4:1 |
| `--cor-marca-texto` | `#8f0b11` · 9,4:1 | `#ff8a8f` · 7,5:1 | Marca como texto, link, ícone ativo ou anel de foco |
| `--cor-marca-suave` | `#fbeceb` | `#3b1013` | Fundo de destaque leve |
| `--cor-fundo` | `#f5f5f7` | `#000000` | Fundo da área de conteúdo |
| `--cor-superficie` | `#ffffff` | `#1c1c1e` | Listas, barra lateral, campos |
| `--cor-superficie-2` | `#f2f2f7` | `#2c2c2e` | Bloco dentro de uma superfície |
| `--cor-flutuante` | `#ffffff` | `#2c2c2e` | Menus, listas de opções, painéis, avisos (no escuro, elevado = mais claro) |
| `--cor-trilho` | `#e9e9ee` | `#3a3a3c` | Trilho do controle segmentado e das barras |
| `--cor-preenchimento` | cinza 12% | cinza 24% | Botão secundário e campo de busca |
| `--cor-hover` / `--cor-selecao` | preto 4% / 6% | branco 6% / 10% | Linha sob o cursor / item selecionado |
| `--cor-texto` | `#1d1d1f` · 16,8:1 | `#f5f5f7` · 15,6:1 | Texto principal |
| `--cor-texto-2` | `#6e6e73` · 5,1:1 | `#98989d` · 5,9:1 | Texto secundário, legendas, placeholders |
| `--cor-borda-campo` | `#86868b` · 3,6:1 | `#8e8e93` · 5,2:1 | Borda de campos |
| `--cor-divisoria` | `#e5e5ea` | `#38383a` | Separadores (decorativos) |
| `--cor-erro-texto` | `#b0300b` · 6,4:1 | `#ff8a65` · 7,4:1 | Erro em texto. É mais alaranjado que a marca, para não se confundir com ela |
| `--cor-perigo` | `#c4320a` | `#c4320a` | Botão de ação destrutiva no diálogo; branco 5,5:1 |
| `--cor-sucesso` | `#1b7f3b` · 5,1:1 | `#30d158` · 8,4:1 | Negócio fechado, menor valor, requisito cumprido |
| `--cor-atencao` | `#a15c00` · 5,2:1 | `#ff9f0a` · 8,3:1 | Em negociação, prazo curto |
| `--cor-balao-outro` | `#e9e9eb` | `#2c2c2e` | Mensagem da outra parte na negociação |

O vermelho-sangue é o mesmo nos dois temas quando é preenchimento. Como texto, ele clareia no escuro, porque `#8f0b11` sobre cinza-escuro não tem contraste.

### Tipografia

A fonte é a do sistema: San Francisco no Mac e no iPhone, Segoe no Windows. Números de valores e contagens usam algarismos de largura fixa (`.numeros`), para alinharem em coluna.

| Token | Tamanho | Uso |
|---|---|---|
| `--texto-titulo-1` | 32px | Título da página (`.titulo-pagina`) |
| `--texto-numero` | 28px | Números de destaque (`.metrica-valor`) |
| `--texto-titulo-3` | 20px | Título de painel |
| `--texto-destaque` | 17px | Título de seção, nome na conversa |
| `--texto-corpo` | 15px | Texto comum |
| `--texto-pequeno` | 13px | Detalhes, rótulos, botões pequenos |
| `--texto-legenda` | 12px | Datas, rótulos de valores |

Campos usam 16px: abaixo disso o iPhone dá zoom ao focar.

**Título de vitrine** (só no painel da marca das telas de acesso): de 36 a 56px conforme a largura, peso 600, entrelinha 1,02 e letras um pouco mais juntas (−0,035em), como os títulos de produto da Apple. A última frase (`destaque`) ganha uma faixa de luz acinzentada (branco a 50% sobre o vinho) que atravessa o texto sem parar, a cada 5 segundos, e o bloco entra subindo e saindo do desfoque, em cascata com o texto e a negociação. Com `prefers-reduced-motion`, nada se move; com alto contraste ou cores forçadas, o destaque vira texto sólido. Nas telas internas, a tipografia continua a da tabela acima: o efeito é para a primeira impressão, não para o trabalho do dia a dia.

### Espaço, forma e movimento

- Espaços em múltiplos de 4px: `--esp-1` (4px) até `--esp-8` (48px).
- Raios: `--raio-pequeno` 8px, `--raio` 10px, `--raio-grande` 16px, `--raio-vidro` 26px (barras e painéis flutuantes) e `--raio-cheio` (cápsula: botões e barra de abas).
- Controles com `--altura-controle` de 40px no computador e 44px em telas de toque (`pointer: coarse`), o mínimo recomendado pela Apple.
- Movimento curto (`--duracao` 200ms) e discreto. Com `prefers-reduced-motion`, as animações somem.

## Estrutura das telas

- **Computador (a partir de 1024px):** barra lateral de vidro encostada na borda, na altura toda da janela, com a marca, a navegação, a Equipe e o menu da conta (pessoa, organização e papel, aparência e sair). O botão no topo recolhe a barra aos ícones, e a escolha fica lembrada no navegador ([ADR 0019](adr/0019-barra-lateral-encostada-e-recolhivel.md)).
- **Celular e tablet:** barra de topo de vidro com a marca e a conta, e barra de abas em cápsula de vidro, flutuando acima da borda, com 3 ou 4 destinos. A Equipe fica no menu da conta.

### Liquid Glass ([ADR 0018](adr/0018-liquid-glass-na-camada-flutuante.md))

O vidro é só para o que flutua sobre o conteúdo. Listas, cartões e o fundo das telas nunca são de vidro.

| Token | Uso |
|---|---|
| `--vidro-fino` | Barras pequenas: abas e topo |
| `--vidro-espesso` | Superfícies com texto: barra lateral, painéis, diálogos, avisos |
| `--vidro-menu` | Menus e listas de opção, quase opacos (abrem dentro de outra barra de vidro) |
| `--vidro-filtro` | `blur(24px) saturate(180%)` |
| `--vidro-brilho`, `--vidro-contorno`, `--vidro-sombra` | Reflexo na borda de cima, contorno de meio pixel e sombra |
| `--vidro-selecao` | A "lente" da aba ou do item selecionado |
| `--brilho-conteudo` | O brilho da marca no fundo, que o vidro recolhe |

Com "Reduzir transparência", "Aumentar contraste" ou sem suporte a `backdrop-filter`, os tokens de vidro viram superfícies sólidas.
- **Sala de negociação:** conversa no centro; detalhes à direita a partir de 1280px (abaixo disso, num painel); lista de negociações à esquerda a partir de 1536px.

Destinos: a empresa tem Início, Cotações e Negociações. O fornecedor tem Início, Mural, Negociações e Propostas.

## Peças em CSS

| Classe | Uso |
|---|---|
| `.pagina` (+ `.pagina-estreita`) | Conteúdo centralizado, com espaçamento que se ajusta ao celular |
| `.cabecalho`, `.titulo-pagina`, `.subtitulo-pagina`, `.cabecalho-acoes` | Topo da página |
| `.voltar` | Link de volta, com o ícone `voltar` |
| `.secao`, `.secao-cabecalho`, `.secao-titulo`, `.secao-acao` | Blocos da página |
| `.colunas` | Duas colunas no computador, uma no celular |
| `.lista` + `.linha` | Lista agrupada. A linha pode ser `<a>`/`<button>` (clicável) ou `<li>`. Partes: `.linha-principal`, `.linha-titulo`, `.linha-detalhe`, `.linha-texto` (2 linhas), `.linha-lateral`, `.linha-valor`, `.linha-acoes`, `.linha-seta` |
| `.metricas` (`<dl>`) + `.metrica` | Números de destaque numa linha, separados por divisórias finas |
| `.botao` + `.botao-primario` / `-secundario` / `-simples` / `-destrutivo` / `-perigo` | Botões. `-destrutivo` é só texto vermelho (ação destrutiva discreta numa linha); `-perigo` tem fundo vermelho claro, para a ação destrutiva principal de um painel ("Encerrar sem acordo"). Modificadores: `.botao-pequeno`, `.botao-icone`, `.botao-largo` |
| `.campo`, `.rotulo`, `.entrada`, `.grupo-entrada` + `.prefixo`, `.ajuda`, `.erro-campo` | Campos de formulário |
| `.busca` | Campo de busca, com a lupa dentro |
| `.formulario`, `.formulario-linha`, `.formulario-acoes` | Formulário em coluna, com campos lado a lado no computador |
| `.nota` (+ `.nota-erro`) | Aviso dentro da página |
| `.etiqueta` (+ `.etiqueta-sucesso`) | Rótulo curto só em texto ("Novo", "Menor valor") |
| `.inicial` | Iniciais do nome num círculo neutro |
| `.vazio`, `.vazio-titulo` | Estado vazio |
| `.carregando-pagina`, `.girando` | Carregando |
| `.urgente` | Prazo curto (o texto já diz "encerra amanhã"; a cor reforça) |
| `.visually-hidden` | Some da tela, continua para o leitor de tela |

## Componentes (`app/ui`)

| Componente | Uso | Acessibilidade |
|---|---|---|
| `<ui-icone nome="..." />` | Conjunto próprio de ícones em SVG, traço de 1,75 | Decorativo (`aria-hidden`); o nome vai no controle |
| `<ui-seletor [opcoes] [(valor)] idRotulo="..." />` | Escolha numa lista (o "select") | Padrão listbox: setas, Home/End, Enter, Esc, busca pela primeira letra |
| `<ui-menu rotulo="...">` + `[uiMenuItem]` | Menu de ações num botão. `tipo="painel"` para conteúdo livre (ex.: conta) | Padrão menu: setas e Esc; fecha ao clicar fora |
| `<ui-segmentado rotulo [opcoes] [(valor)] />` | Escolha entre poucas opções (filtros, tipo de conta, aparência) | Rádios nativos com legenda |
| `<ui-painel titulo [(aberto)]>` + `[rodape]` | Painel lateral; vira folha de baixo para cima no celular | `<dialog>` modal: foco preso, Esc fecha |
| `ConfirmacaoService.confirmar({...})` | Confirmação antes de ações importantes (no lugar do `confirm()`) | `alertdialog`; foco em "Cancelar" nas ações destrutivas |
| `<ui-carregar-mais [mostrando] [total] singular plural (carregar)>` | Rodapé das listas paginadas: "20 de 42 organizações" e o botão "Carregar mais organizações", que some quando a lista está inteira | A contagem é anunciada (`aria-live`) quando muda |
| `<ui-status [tom]>` | Status como ponto + texto. Tons: `neutro`, `sucesso`, `atencao`, `marca`, `erro` | A cor nunca vai sozinha |

Exemplo de seletor com rótulo visível:

```html
<span class="rotulo" id="rotulo-categoria">Categoria</span>
<ui-seletor idRotulo="rotulo-categoria" [opcoes]="categorias()" [(valor)]="categoria" />
```

Exemplo de confirmação:

```ts
const confirmou = await this.confirmacao.confirmar({
  titulo: 'Recusar a proposta?',
  mensagem: 'O fornecedor verá a proposta como recusada.',
  confirmar: 'Recusar',
  destrutivo: true,
});
```

## Regras

- Componente novo usa só `var(--cor-*)` e os demais tokens. Se faltar um valor, ele entra primeiro no `tokens.css`, com os dois temas e o contraste medido.
- Antes de criar uma peça, procure nesta página: lista, botão, campo, seletor e painel cobrem quase tudo.
- Toda tela é conferida em 1440×900, 1280×640, 390×844 e 360×640, no claro e no escuro.
