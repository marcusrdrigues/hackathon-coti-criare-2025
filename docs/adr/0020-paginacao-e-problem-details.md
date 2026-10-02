# 0020. Paginação no servidor e erros em Problem Details

- **Status:** Aceita
- **Data:** 2026-10-02

## Contexto

As listas da API devolviam tudo de uma vez: todas as cotações da empresa, o mural inteiro, todas as propostas e negociações. As telas filtravam e contavam na memória do navegador. Com o uso, essas listas crescem sem limite, e cada item ainda custa consultas extras (quantas propostas, a melhor oferta, a negociação da proposta).

Os erros tinham um formato próprio (`status`, `message`, `timestamp`, `traceId` e `errors`), diferente do que as bibliotecas e os clientes HTTP já sabem ler. Os 401 e 403 dos filtros de segurança e os erros do Spring MVC fora da lista (405, 415) não seguiam nem esse formato.

## Decisão

**Paginação no servidor, com os filtros junto:**

- Parâmetros de sempre do Spring: `page` (a partir de 0), `size` (padrão 20, máximo 50; acima disso, vale 50) e `sort=campo,asc|desc`, com uma lista de campos aceitos por endpoint. Campo fora da lista responde 400.
- Resposta no formato estável do `PagedModel` do Spring Data: `{ content, page: { size, number, totalElements, totalPages } }`.
- Os filtros e a busca vão para o servidor, porque filtrar só a página carregada daria um resultado errado. O que as telas calculavam sobre a lista inteira ganhou endpoint próprio: a contagem das cotações por situação, as mensagens não lidas e a proposta do fornecedor em cada item do mural.
- A aplicação não conhece o Spring Data (regra do `ArquiteturaTest`): ela usa `PedidoDePagina` e `Pagina<T>`, do módulo compartilhado, e só a web e a persistência convertem.
- Listas limitadas por natureza (propostas de uma cotação, mensagens de uma negociação, equipe) continuam inteiras. No front-end, as listas paginadas ganham "Carregar mais".

**Erros em Problem Details (RFC 9457)**, em toda resposta de erro, com o tipo `application/problem+json`: `type` (`about:blank`), `title`, `status`, `detail` e `instance`, mais `traceId` (ADR 0014) e, na validação, `erros` por campo. Um record próprio, `Problema`, gera o corpo, e os filtros de segurança usam o mesmo record.

## Alternativas consideradas

- **Paginação por cursor (keyset):** mais eficiente em listas enormes e estável quando entram itens novos, mas não dá o total nem o número da página, que as telas mostram ("20 de 42"). Fica para quando uma lista passar de dezenas de milhares de itens.
- **`Pageable` direto nos controllers:** menos código, mas depende de como cada versão do Spring Boot configura o resolvedor e expõe qualquer propriedade da entidade à ordenação. Os parâmetros lidos à mão, com a lista de campos aceitos, deixam o contrato explícito.
- **`ProblemDetail` do Spring:** as propriedades extras (`traceId`, `erros`) dependem de um mixin do Jackson para sair no nível de cima do JSON, e isso muda entre o Jackson 2 e o 3. O record próprio dá o mesmo JSON sempre.
- **Manter o formato antigo:** cada cliente teria de aprender um formato que só esta API usa.

## Consequências

- Toda listagem nova que pode crescer já nasce paginada, com a lista de campos de ordenação aceitos.
- O front-end lê `detail` e `erros`. Um cliente que lia `message` precisa mudar (a API é usada só pelo front-end deste repositório).
- As telas com busca esperam a pessoa parar de digitar antes de consultar a API, para não fazer uma requisição por tecla.
- A paginação é por deslocamento: se um item entra ou sai da lista entre uma página e outra, o próximo "Carregar mais" pode trazer de novo o último item (o front-end descarta o repetido) ou pular um. Depois de uma ação na própria tela (retirar uma proposta, por exemplo), a lista se realinha com a API. Se pular um item passar a importar, a troca é pela paginação por cursor.
