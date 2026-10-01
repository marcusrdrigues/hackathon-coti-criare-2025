# 🎬 Como gravar o GIF da demo

Um GIF curto no topo do README mostra o projeto funcionando em segundos, sem o visitante precisar abrir nada.

## Ferramenta

No Windows, use o [ScreenToGif](https://www.screentogif.com/) (gratuito e de código aberto). Ele grava uma área da tela, permite cortar quadros e exporta direto em GIF.

## Preparação

1. Rode o projeto com os dados de demonstração (`-Dspring-boot.run.profiles=demo`) ou use a demo publicada.
2. Deixe o navegador numa janela de **1280 × 800**, com zoom em 100%, sem favoritos nem extensões à mostra.
3. Abra duas janelas lado a lado: uma normal e uma anônima. Assim dá para mostrar empresa e fornecedor ao mesmo tempo.

## Roteiro (cerca de 30 segundos)

| # | Janela | Ação | O que o visitante entende |
|---|---|---|---|
| 1 | Empresa | Clicar em **Entrar como empresa** | Dá para testar sem cadastro |
| 2 | Empresa | Mostrar o dashboard por 2 segundos | Há indicadores reais |
| 3 | Empresa | **Minhas Cotações → Aquisição de 10 notebooks** | Cotação com propostas e melhor oferta |
| 4 | Empresa | Clicar em **Abrir negociação** | Existe uma negociação em andamento |
| 5 | Fornecedor | Clicar em **Entrar como fornecedor → Propostas em Andamento → Negociar** | O outro lado vê a mesma conversa |
| 6 | Fornecedor | Enviar contraproposta de **R$ 56.500** | Contraproposta com valor |
| 7 | Empresa | Esperar a mensagem aparecer (atualiza sozinha) e clicar em **Fechar por R$ 56.500,00** | Fluxo completo até o negócio fechado |
| 8 | Fornecedor | Abrir **Histórico** | A proposta aparece como vencida |

## Exportação

- Corte os quadros parados no editor do ScreenToGif.
- Exporte em GIF com **até 15 quadros por segundo** e largura de **1000 px**, para o arquivo ficar abaixo de 10 MB.
- Salve como `docs/demo.gif` e adicione no README, logo depois dos badges:

```html
<p align="center">
  <img src="docs/demo.gif" alt="Negociação entre empresa e fornecedor no Portal Criare" width="900" />
</p>
```
