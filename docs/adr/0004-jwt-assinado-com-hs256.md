# 0004. JWT assinado com HS256 pelo resource server do Spring

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

O mesmo serviço emite e valida os tokens, e não existe outro serviço que precise validá-los. A validação (assinatura, expiração, emissor) é a parte mais sensível e a mais fácil de errar à mão.

## Decisão

Usar o `spring-boot-starter-security-oauth2-resource-server` com `NimbusJwtEncoder`/`NimbusJwtDecoder` e uma chave simétrica HS256 vinda de `JWT_SECRET` (base64, com pelo menos 32 bytes). Sem a variável, a API gera uma chave aleatória a cada inicialização e avisa no log. Isso serve para desenvolvimento, mas derruba as sessões a cada reinício. O perfil vira autoridade (`ROLE_EMPRESA`, `ROLE_FORNECEDOR`) por um conversor do claim `tipo`.

## Alternativas consideradas

- **RS256 (par de chaves)**: necessário quando outros serviços validam o token sem poder emitir. Aqui só acrescentaria a gestão de chaves.
- **Biblioteca à parte (jjwt, java-jwt) e filtro próprio**: mais código para manter, e a validação ficaria por nossa conta.

## Consequências

- Todo o pipeline de validação é o do Spring Security.
- `JWT_SECRET` é obrigatório em produção. O `render.yaml` o gera automaticamente (`generateValue`).
- Se surgir um segundo serviço que precise validar tokens, a troca para RS256 fica restrita à configuração da chave.
