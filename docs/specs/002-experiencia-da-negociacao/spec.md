# 002 · Experiência da negociação e escrita da interface

- **Status:** Concluída
- **Data:** 2026-10-02
- **Plano:** [plan.md](plan.md) · **Tarefas:** [tasks.md](tasks.md)

## Problema

Usando a demo depois do visual Liquid Glass ([ADR 0018](../../adr/0018-liquid-glass-na-camada-flutuante.md)), três coisas ficaram estranhas na sala de negociação, que é a tela mais importante do produto:

- **A barra lateral parece solta.** No computador, ela flutua afastada das bordas ao lado de colunas sólidas que vão até a borda, e não há como recolhê-la para ganhar espaço.
- **Fazer uma oferta não é óbvio.** O campo de valor fica ao lado do campo de mensagem, sem um botão que diga o que acontece. Quem quer só conversar não sabe se precisa preencher o valor; quem quer ofertar não vê um "Enviar oferta".
- **"Encerrar sem acordo" parece um texto, não um botão,** solto no fim do painel de detalhes.

Além disso, uma revisão da escrita da interface pelas diretrizes da Apple (*Writing*, *Buttons*, *Alerts*) encontrou rótulos genéricos ("Abrir", "Retirar", "Contrapropor") e confirmações cujo botão não repete o nome da ação.

## Objetivos

- A barra lateral do computador faz parte da janela e pode ser recolhida.
- Fazer uma oferta é uma ação própria, com um botão que diz exatamente o que vai acontecer.
- A oferta que está na mesa e a decisão sobre ela ficam claras para os dois lados.
- Ações destrutivas têm cara de botão e ficam onde se espera encontrá-las.
- Os textos seguem um padrão: verbo no início do botão, o mesmo nome da ação do botão até a confirmação e o aviso de sucesso.

## Fora do escopo

- As mensagens de erro do back-end (pontuação, tom). Elas mudam no passo de Problem Details da spec 001, junto com o formato.
- Mudanças nas regras de negócio da negociação.

## Requisitos

### R1 · Barra lateral integrada e recolhível (Deve)

- **Dado** o computador, **então** a barra lateral ocupa a altura toda da janela, encostada na borda, e o conteúdo começa logo depois dela.
- **Dado** a barra aberta, **quando** a pessoa usa "Recolher barra lateral", **então** ela vira uma coluna estreita só com os ícones, que continuam navegáveis e com nome para o leitor de tela; "Mostrar barra lateral" a abre de novo.
- **Dado** a escolha feita, **quando** a pessoa volta ao portal no mesmo navegador, **então** a barra está como ela deixou. A barra nunca começa recolhida para quem ainda não escolheu.

### R2 · Fazer uma oferta (Deve)

- **Dado** a negociação em andamento, **então** o campo de mensagem serve só para texto, e um botão "Oferta" abre a área de nova oferta.
- **Dado** a área de nova oferta, **quando** a pessoa digita um valor, **então** vê a diferença para a oferta na mesa e o botão diz o que vai acontecer: "Enviar oferta de R$ 54.000,00". Sem valor válido, o botão fica desabilitado.
- **Dado** a área de nova oferta, **então** a pessoa pode juntar uma mensagem à oferta e pode desistir sem enviar nada.

### R3 · A oferta na mesa (Deve)

- **Dado** a última oferta veio da outra parte, **então** a tela mostra quem ofereceu e quanto, com "Fazer contraproposta" e a decisão: "Fechar por R$ X" para a empresa, "Aceitar R$ X" para o fornecedor.
- **Dado** a última oferta é do meu lado, **então** a tela diz que estamos aguardando a resposta da outra parte.

### R4 · Ações destrutivas (Deve)

- **Dado** a empresa numa negociação em andamento, **então** "Encerrar sem acordo" aparece como botão destrutivo no painel de detalhes, com uma explicação do que acontece, e também no menu de mais ações da conversa.
- **Dado** qualquer confirmação, **então** o botão de confirmar repete o nome da ação ("Encerrar sem acordo", e não "Encerrar" ou "OK").

### R5 · Escrita da interface (Deveria)

- **Dado** um botão, **então** ele começa com um verbo e diz o objeto quando o contexto não deixa óbvio ("Retirar proposta", "Ver negociação").
- **Dado** uma mensagem de quem é da mesma organização que o nome dela, **então** o nome não aparece repetido.
