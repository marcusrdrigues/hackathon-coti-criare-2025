# 0017. Equipe com convite por link de uso único

- **Status:** Aceita
- **Data:** 2026-10-02

## Contexto

Com pessoas e organizações separadas ([ADR 0015](0015-pessoas-e-organizacoes-separadas.md)), faltava o jeito de uma pessoa nova entrar numa organização que já existe. A spec 001 (R2) pede convite de uso único, válido por 72 horas, e remoção que corte o acesso na hora. Na spec, a decisão foi não enviar e-mail: o proprietário recebe o link e o repassa pelo canal que preferir.

## Decisão

- **Quem gerencia:** só o proprietário convida, cancela convites e remove pessoas (`@PreAuthorize("hasRole('PROPRIETARIO')")` na rota e a mesma checagem no caso de uso). Todos da organização veem a equipe.
- **O link** carrega um token aleatório de 256 bits. O banco guarda só o hash SHA-256 (`tb_convite`), como nas sessões. O token aparece uma única vez, na resposta de quem criou o convite.
- **O token nunca vai na URL que chega a um servidor.** O link é `/convite#<token>`: a parte depois do `#` não é enviada nem à Vercel nem à API, então não fica em nenhum log de acesso. A tela lê o token, tira-o da barra de endereço e o manda no corpo de um POST (`/api/v1/convites/consulta` e `/aceite`).
- **Validade:** 72 horas e uma vez só. Um convite novo para o mesmo e-mail cancela o anterior. Vencido, usado, cancelado ou adulterado: 404 com uma mensagem que diz o que aconteceu e o que fazer.
- **Aceitar** cria a pessoa, o vínculo de membro e a sessão numa transação só. O e-mail é o do convite; a pessoa confirma o nome e define a senha.
- **Remover** marca `removido_em` no vínculo (o histórico continua com o nome da pessoa), revoga todas as sessões dela e, depois do commit, fecha as conexões WebSocket abertas por ela (`SessoesWebSocket`, que associa cada conexão à pessoa no CONNECT). O access token em uso continua válido até vencer, em no máximo 15 minutos.
- **A organização nunca fica sem proprietário:** só o proprietário remove, ninguém remove a si mesmo, e ainda não existe troca de papel.

## Alternativas consideradas

- **Enviar o convite por e-mail:** exige provedor, domínio verificado e tratamento de entrega. Fica para quando houver motivo; o resto do fluxo não muda.
- **Token no caminho (`/convite/{token}`), como no plano:** mais simples de ler, mas o caminho vai para os logs da Vercel e de qualquer proxy no meio. O `#` resolve sem custo.
- **Bloquear o access token de quem foi removido em toda requisição:** custaria uma consulta ao banco por requisição. Com o token de 15 minutos, a sessão revogada e o WebSocket fechado, o risco restante é pequeno e documentado.

## Consequências

- Uma pessoa removida não pode ser convidada de novo com o mesmo e-mail, porque o e-mail é único na plataforma e a conta continua existindo (sem vínculo). Reativar pessoas fica para quando houver demanda.
- O teste de isolamento ([ADR 0016](0016-autorizacao-por-organizacao.md)) cobre as rotas novas com id: o proprietário de outra organização recebe 404 ao tentar remover um membro ou cancelar um convite que não são dele.
