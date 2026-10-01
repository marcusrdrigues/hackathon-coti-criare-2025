#!/usr/bin/env bash
# Teste de fumaça da API: percorre o fluxo completo via HTTP, como o front-end faz.
# Pré-requisito: API rodando com o profile "demo" (dados de exemplo) e jq instalado.
#
#   ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
#   ./scripts/smoke-test-api.sh            # usa http://localhost:8085
#   API=http://outro-host:8085 ./scripts/smoke-test-api.sh

set -euo pipefail

API="${API:-http://localhost:8085}"
V1="$API/api/v1"
ORIGEM_FRONT="http://localhost:4200"

falhar() {
  # Formato reconhecido pelo GitHub Actions (vira anotação no job)
  echo "::error::$1"
  echo "FALHOU: $1" >&2
  exit 1
}

# Faz a requisição e confere o status HTTP esperado. Imprime o corpo da resposta.
chamar() {
  local metodo="$1" url="$2" esperado="$3" corpo="${4:-}"
  local saida status
  if [[ -n "$corpo" ]]; then
    saida=$(curl -sS -w '\n%{http_code}' -X "$metodo" "$url" -H 'Content-Type: application/json' -d "$corpo")
  else
    saida=$(curl -sS -w '\n%{http_code}' -X "$metodo" "$url")
  fi
  status=$(tail -n1 <<<"$saida")
  local resposta
  resposta=$(sed '$d' <<<"$saida")
  [[ "$status" == "$esperado" ]] || falhar "$metodo $url retornou $status (esperado $esperado): $resposta"
  echo "$resposta"
}

passo() { echo "✔ $1"; }

echo "Testando $API"

# 1. Login dos usuários de demonstração
EMPRESA=$(chamar POST "$V1/auth/login" 200 '{"email":"empresa@demo.com","senha":"demo123"}')
EMPRESA_ID=$(jq -r .id <<<"$EMPRESA")
[[ $(jq -r .tipo <<<"$EMPRESA") == "EMPRESA" ]] || falhar "login da empresa não retornou tipo EMPRESA"
FORNECEDOR=$(chamar POST "$V1/auth/login" 200 '{"email":"limpabem@demo.com","senha":"demo123"}')
FORNECEDOR_ID=$(jq -r .id <<<"$FORNECEDOR")
[[ $(jq -r .tipo <<<"$FORNECEDOR") == "FORNECEDOR" ]] || falhar "login do fornecedor não retornou tipo FORNECEDOR"
chamar POST "$V1/auth/login" 401 '{"email":"empresa@demo.com","senha":"errada"}' >/dev/null
passo "login (empresa, fornecedor e senha errada)"

# 2. CORS para o front-end Angular
CORS=$(curl -sS -o /dev/null -D - -X OPTIONS "$V1/cotacoes" \
  -H "Origin: $ORIGEM_FRONT" -H 'Access-Control-Request-Method: POST')
grep -qi "access-control-allow-origin: $ORIGEM_FRONT" <<<"$CORS" || falhar "CORS não liberou $ORIGEM_FRONT"
passo "CORS liberado para $ORIGEM_FRONT"

# 3. Validação e cadastro
chamar POST "$V1/fornecedores" 400 '{"nomeCompleto":"","cnpj":"1","email":"x","senha":"1"}' >/dev/null
chamar POST "$V1/fornecedores" 400 \
  '{"nomeCompleto":"CNPJ Errado","cnpj":"11.222.333/0001-00","email":"cnpj@teste.com","senha":"123456"}' >/dev/null
chamar POST "$V1/fornecedores" 409 \
  '{"nomeCompleto":"Duplicado","cnpj":"33.445.566/0001-86","email":"empresa@demo.com","senha":"123456"}' >/dev/null
passo "validação de campos, CNPJ e e-mail duplicado"

# 4. Empresa publica uma cotação
LIMITE=$(date -d '+7 days' '+%Y-%m-%dT23:59:59')
CATEGORIAS=$(chamar GET "$V1/cotacoes/categorias" 200)
[[ $(jq length <<<"$CATEGORIAS") -ge 5 ]] || falhar "lista de categorias vazia"
COTACAO=$(chamar POST "$V1/cotacoes" 201 "$(jq -n --arg e "$EMPRESA_ID" --arg d "$LIMITE" \
  '{nomeServico:"Café e descartáveis",requisitos:"Fornecimento mensal",categoria:"ALIMENTOS",orcamentoEstimado:1500,dataLimite:$d,empresaId:$e}')")
COTACAO_ID=$(jq -r .id <<<"$COTACAO")
[[ $(jq -r .status <<<"$COTACAO") == "ABERTA" ]] || falhar "cotação criada sem status ABERTA"
chamar GET "$V1/cotacoes/abertas" 200 | jq -e --arg id "$COTACAO_ID" 'any(.[]; .id == $id)' >/dev/null \
  || falhar "cotação nova não aparece no mural"
passo "cotação publicada e visível no mural"

# 5. Fornecedor envia proposta (e não pode enviar duas)
PROPOSTA_JSON=$(jq -n --arg f "$FORNECEDOR_ID" --arg c "$COTACAO_ID" \
  '{valor:1400,descricao:"Entrega semanal",fornecedorId:$f,cotacaoId:$c}')
PROPOSTA=$(chamar POST "$V1/propostas" 201 "$PROPOSTA_JSON")
PROPOSTA_ID=$(jq -r .id <<<"$PROPOSTA")
chamar POST "$V1/propostas" 400 "$PROPOSTA_JSON" >/dev/null
passo "proposta enviada (duplicada bloqueada)"

# 6. Empresa abre a negociação
NEGOCIACAO=$(chamar POST "$V1/negociacoes" 201 "{\"propostaId\":\"$PROPOSTA_ID\"}")
NEGOCIACAO_ID=$(jq -r .id <<<"$NEGOCIACAO")
[[ $(jq -r .cotacaoStatus <<<"$NEGOCIACAO") == "EM_NEGOCIACAO" ]] || falhar "cotação não foi para EM_NEGOCIACAO"
passo "negociação aberta"

# 7. Troca de mensagens e contrapropostas
chamar POST "$V1/mensagens" 201 "$(jq -n --arg n "$NEGOCIACAO_ID" --arg r "$EMPRESA_ID" \
  '{negociacaoId:$n,mensagem:"Fecha por 1.300?",valorOfertado:1300,tipoRemetente:"EMPRESA",remetenteId:$r}')" >/dev/null
chamar POST "$V1/mensagens" 201 "$(jq -n --arg n "$NEGOCIACAO_ID" --arg r "$FORNECEDOR_ID" \
  '{negociacaoId:$n,mensagem:null,valorOfertado:1350,tipoRemetente:"FORNECEDOR",remetenteId:$r}')" >/dev/null
chamar POST "$V1/mensagens" 400 "$(jq -n --arg n "$NEGOCIACAO_ID" --arg r "$EMPRESA_ID" \
  '{negociacaoId:$n,mensagem:"intruso",tipoRemetente:"FORNECEDOR",remetenteId:$r}')" >/dev/null
MENSAGENS=$(chamar GET "$V1/mensagens/negociacao/$NEGOCIACAO_ID" 200)
[[ $(jq length <<<"$MENSAGENS") -eq 3 ]] || falhar "esperava 3 mensagens no histórico, veio $(jq length <<<"$MENSAGENS")"
ULTIMA=$(chamar GET "$V1/negociacoes/$NEGOCIACAO_ID" 200 | jq -r .ultimaOferta)
[[ "$ULTIMA" == "1350" || "$ULTIMA" == "1350.00" || "$ULTIMA" == "1350.0" ]] || falhar "última oferta deveria ser 1350, veio $ULTIMA"
passo "mensagens, contraproposta e bloqueio de remetente"

# 8. Fechamento do negócio
FINAL=$(chamar PATCH "$V1/negociacoes/$NEGOCIACAO_ID/finalizar" 200 '{"valorFinal":1350}')
[[ $(jq -r .status <<<"$FINAL") == "FINALIZADA" ]] || falhar "negociação não finalizou"
[[ $(jq -r .status <<<"$(chamar GET "$V1/cotacoes/$COTACAO_ID" 200)") == "FECHADA" ]] || falhar "cotação não fechou"
passo "negócio fechado e cotação encerrada"

# 9. Dashboards
DASH_F=$(chamar GET "$V1/dashboard/fornecedor/$FORNECEDOR_ID" 200)
[[ $(jq -r .cotacoesGanhas <<<"$DASH_F") -ge 1 ]] || falhar "dashboard do fornecedor sem cotação ganha"
DASH_E=$(chamar GET "$V1/dashboard/empresa/$EMPRESA_ID" 200)
[[ $(jq -r .cotacoesFechadas <<<"$DASH_E") -ge 1 ]] || falhar "dashboard da empresa sem cotação fechada"
[[ $(jq '.categorias | length' <<<"$DASH_E") -ge 1 ]] || falhar "dashboard da empresa sem categorias"
passo "dashboards de empresa e fornecedor"

# 10. Erros tratados
chamar GET "$V1/cotacoes/nao-e-uuid" 400 >/dev/null
chamar GET "$V1/cotacoes/00000000-0000-0000-0000-000000000000" 404 >/dev/null
chamar GET "$API/api-docs" 200 >/dev/null
passo "erros 400/404 e documentação OpenAPI"

echo "Todos os testes de fumaça passaram."
