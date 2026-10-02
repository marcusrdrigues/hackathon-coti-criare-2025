# 0019. Barra lateral encostada e recolhível

- **Status:** Aceita
- **Data:** 2026-10-02
- **Substitui em parte:** [ADR 0018](0018-liquid-glass-na-camada-flutuante.md) (a barra lateral como bloco de vidro afastado das bordas)

## Contexto

O ADR 0018 levou a barra lateral do computador para um bloco de vidro flutuante, afastado das bordas e com cantos arredondados. Na tela de negociação, que ocupa a largura toda com três colunas, a barra parecia solta, sobreposta ao conteúdo, sem pertencer ao layout. Faltava também um jeito de ganhar espaço para a conversa em telas menores de computador.

Nas próprias apps da Apple para Mac e iPad (Mensagens, Notas, Mail), a barra lateral encosta na borda da janela, ocupa a altura toda e tem um botão no topo para recolher. A cápsula flutuante é da barra de abas do celular, que continua como está.

## Decisão

- **Encostada:** a barra lateral ocupa a altura toda da janela, colada à borda esquerda, sem cantos arredondados e separada do conteúdo por uma linha fina. Continua de vidro (`--vidro-espesso`), com o brilho da marca passando por baixo.
- **Recolhível:** um botão no topo da barra ("Recolher barra lateral" / "Mostrar barra lateral", com `aria-expanded`) reduz a barra aos ícones (`--largura-lateral-recolhida`). Recolhida, cada destino mantém o nome acessível (o rótulo fica visualmente oculto, não removido) e ganha um `title` com o nome.
- **A escolha é lembrada** no navegador (`localStorage`, chave `barra_lateral`). Sem armazenamento disponível, vale até recarregar. Quem nunca escolheu começa com a barra aberta, para descobrir os nomes dos destinos.
- **O celular não muda:** lá a navegação é a barra de abas em cápsula.

## Alternativas consideradas

- **Manter flutuante e só ajustar o espaçamento:** continua disputando espaço com telas de três colunas e não resolve a falta de espaço.
- **Recolher sozinha abaixo de certa largura:** tira a escolha de quem prefere ver os nomes; o botão resolve com menos surpresa.
- **Guardar a escolha na conta (API):** é preferência de dispositivo, não da pessoa; no navegador basta.

## Consequências

- O grid do shell ganha a classe `lateral-recolhida`, que troca a largura da primeira coluna; as telas não precisam saber se a barra está aberta.
- Testes ponta a ponta usam o nome acessível dos destinos, que vale aberta ou recolhida.
