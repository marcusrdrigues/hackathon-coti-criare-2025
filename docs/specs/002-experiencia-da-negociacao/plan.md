# 002 · Plano

## Barra lateral (R1)

- Encostada na borda, com a altura da janela, em vidro espesso e uma linha fina à direita. Isso substitui o bloco flutuante do ADR 0018 só no computador; a barra de abas do celular continua em cápsula. Decisão registrada no **ADR 0019**.
- Botão com o ícone de painel no topo da barra, com `aria-expanded` e rótulo que muda ("Recolher barra lateral" / "Mostrar barra lateral").
- Recolhida: 4,5rem, só ícones (os textos continuam no DOM, escondidos visualmente, para o leitor de tela), logo sem o nome, conta só com as iniciais. `title` em cada destino para quem usa o mouse.
- A escolha fica no `localStorage` do navegador (`barra_lateral`), com `try/catch`: sem armazenamento, a barra simplesmente começa aberta.

## Oferta (R2, R3)

- O compositor vira: botão "Oferta" (ícone de etiqueta; só o ícone no celular, com o mesmo nome acessível), campo de mensagem e botão de enviar mensagem.
- A área de nova oferta abre acima do compositor, dentro da conversa (não é um modal: a conversa continua visível). Campo de valor grande, a diferença para a oferta na mesa ("R$ 1.000,00 abaixo da oferta na mesa, −1,8%"), mensagem opcional e o botão "Enviar oferta de R$ …".
- O cartão da oferta na mesa substitui a faixa de decisão: rótulo "Oferta na mesa", valor, de quem é, e as ações.
- Nenhuma mudança na API: oferta continua sendo uma mensagem com `valorOfertado`.

## Ações destrutivas (R4)

- Novo estilo `botao-perigo`: fundo de erro suave e texto de erro, a mesma altura dos outros botões, largura toda no painel.
- Menu "Mais ações" (ícone de reticências) no topo da conversa, com "Ver cotação" (empresa) e "Encerrar sem acordo".

## Escrita (R5)

Revisão de todos os rótulos de botão, confirmações e avisos das telas, com a lista de termos do produto: cotação, proposta, oferta, contraproposta, negociação, fechar negócio, encerrar sem acordo.

## Testes

- Ponta a ponta: a negociação passa a usar "Oferta" → "Enviar oferta de …"; a barra recolhe e volta.
- Unitário: o cálculo da diferença para a oferta na mesa.
