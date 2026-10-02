# 0016. Autorização por organização

- **Status:** Aceita
- **Data:** 2026-10-02
- **Substitui:** [ADR 0005](0005-autorizacao-por-perfil-e-por-posse.md)

## Contexto

O ADR 0005 separava o perfil (no controller, com `@PreAuthorize`) da posse (nos services). Com pessoas e organizações separadas ([ADR 0015](0015-pessoas-e-organizacoes-separadas.md)), três problemas apareceram:

- A posse estava espalhada: cada service tinha o próprio `verificarDona`, `verificarAutor` ou `verificarParticipante`, e o WebSocket repetia a regra da negociação por conta própria. Uma rota nova podia esquecer a checagem sem que nada acusasse.
- Recurso de outra organização respondia **403**, enquanto um id inexistente respondia **404**. A diferença revela que o id é válido (enumeração de recursos, OWASP API1).
- Um fornecedor via qualquer cotação pelo id, inclusive as canceladas e as fechadas com um concorrente, mesmo sem nunca ter participado delas.

## Decisão

- **Uma política de acesso por módulo.** Em compras, a classe `AcessoCompras` responde a todas as perguntas de acesso: quem vê uma cotação, quem a altera, quem vê, recusa ou retira uma proposta, quem participa de uma negociação e quem a fecha. Os services, a autorização do WebSocket e o que vier depois (as ferramentas de IA da fase 7, por exemplo) passam por ela.
- **A posse é da organização do token.** Qualquer pessoa ativa da organização vê e opera os dados dela. Restrições por papel, quando houver, entram na mesma política.
- **Fora da organização, o recurso não existe.** A resposta é 404, com a mesma mensagem de um id inexistente. O 403 fica para quem enxerga o recurso mas não pode fazer aquela ação, como o fornecedor de uma negociação tentando fechá-la, ou para o perfil errado (`@PreAuthorize`, mantido).
- **Cotação para o fornecedor:** visível enquanto está aberta (é o mural) ou se ele enviou proposta para ela. As demais respondem 404, inclusive ao tentar enviar proposta.
- **Trava de rota nova.** O `IsolamentoEntreOrganizacoesTest` lê do Spring MVC todas as rotas da aplicação que recebem id, no caminho ou num campo UUID do corpo, e exige um caso de isolamento para cada uma. Cada caso faz a tentativa com o id real e com um id aleatório e confere que as duas respostas são 404 com a mesma mensagem. Rota nova sem caso, ou caso de rota que não existe mais, quebra o build.
- **Uso interno sem usuário** (os avisos em tempo real, que reagem a eventos depois do commit) continua com os `buscarPorId` dos services, documentados como "sem checar quem pede".

## Alternativas consideradas

- **Filtro por organização no Hibernate** (`@Filter`/`@TenantId`): isola as consultas automaticamente, mas aqui o mesmo recurso é compartilhado entre duas organizações (a empresa e o fornecedor de uma proposta), e as regras são de relação, não de dono único.
- **Manter 403 para recurso de outra organização**: é mais fácil de depurar, mas confirma a existência do id. O `traceId` no corpo do erro já dá o caminho para investigar sem revelar nada.
- **`@PostAuthorize` com expressões SpEL**: tira a regra do código Java testável e roda depois de carregar o recurso, o que não protege as ações que alteram dados.

## Consequências

- Uma regra de acesso nova é escrita uma vez e vale para o REST e o WebSocket.
- O front-end trata recurso de outra organização como "não encontrado", o mesmo caminho de um link quebrado.
- As rotas STOMP (`/app/negociacoes/{id}/digitando` e a assinatura do tópico da negociação) usam a mesma política, mas não entram na trava automática. Quem as cobre é o `TempoRealIntegrationTest`.
- Os passos seguintes da spec (equipe e superadmin) acrescentam papéis à mesma política, em vez de criar checagens novas nos services.
