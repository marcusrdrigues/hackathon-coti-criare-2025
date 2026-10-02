# 0005. Autorização por perfil no controller e por posse no service

- **Status:** Substituída por [ADR 0016](0016-autorizacao-por-organizacao.md)
- **Data:** 2026-10-01

## Contexto

Há dois tipos de regra de acesso. Uma depende só do perfil: só empresa publica cotação, só fornecedor envia proposta. A outra depende do dado: a empresa só mexe nas cotações dela, e só as duas partes de uma negociação a enxergam. A segunda exige consultar o banco.

## Decisão

- **Perfil**: `@PreAuthorize("hasRole('EMPRESA')")` nos endpoints.
- **Posse**: verificada nos services, ao lado da regra de negócio, a partir da identidade do token (`UsuarioAtual`), e nunca de um id enviado pelo cliente. As rotas do tipo "os meus" (`/cotacoes/minhas`, `/propostas/minhas`, `/dashboard/empresa`) não recebem id de usuário.
- Violação de posse gera `AcessoNegadoException`, que vira `403` com o mesmo JSON de erro do resto da API.

## Alternativas consideradas

- **Tudo no controller**: o controller precisaria carregar entidades só para checar o dono, duplicando consultas e espalhando regra de negócio.
- **ACL do Spring Security**: poderosa, mas desproporcional para "dono do recurso" e "participante da negociação".

## Consequências

- Um service chamado de outro lugar (agendador, outro service) continua protegido.
- Os testes de posse ficam nos testes de service (`FluxoCotacaoIntegrationTest`) e de HTTP (`SegurancaApiTest`).
