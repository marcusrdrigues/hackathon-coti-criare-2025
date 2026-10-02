#!/usr/bin/env bash
# Teste de fumaça da API: percorre o fluxo completo via HTTP, como o front-end faz,
# incluindo autenticação JWT, renovação de sessão pelo cookie e regras de acesso.
# Pré-requisito: API rodando com o profile "demo" (dados de exemplo), curl e jq.
#
#   ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
#   ./scripts/smoke-test-api.sh            # usa http://localhost:8085
#   API=http://outro-host:8085 ./scripts/smoke-test-api.sh

set -euo pipefail

API="${API:-http://localhost:8085}"
V1="$API/api/v1"
ORIGEM_FRONT="http://localhost:4200"
SENHA_DEMO="demo1234"
COOKIES=$(mktemp -d)
trap 'rm -rf "$COOKIES"' EXIT

falhar() {
  # Formato reconhecido pelo GitHub Actions (vira anotação no job)
  echo "::error::$1"
  echo "FALHOU: $1" >&2
  exit 1
}

# chamar MÉTODO URL STATUS_ESPERADO [CORPO] [TOKEN] [ARQUIVO_DE_COOKIES]
# Confere o status HTTP e imprime o corpo da resposta.
chamar() {
  local metodo="$1" url="$2" esperado="$3" corpo="${4:-}" token="${5:-}" jar="${6:-}"
  local args=(-sS -w '\n%{http_code}' -X "$metodo" "$url")
  [[ -n "$corpo" ]] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$jar" ]] && args+=(-b "$jar" -c "$jar")
  local saida status resposta
  saida=$(curl "${args[@]}")
  status=$(tail -n1 <<<"$saida")
  resposta=$(sed '$d' <<<"$saida")
  [[ "$status" == "$esperado" ]] || falhar "$metodo $url retornou $status (esperado $esperado): $resposta"
  echo "$resposta"
}

passo() { echo "✔ $1"; }

echo "Testando $API"

# 1. Login: access token no corpo, refresh token só no cookie
EMP_JAR="$COOKIES/empresa"; FORN_JAR="$COOKIES/fornecedor"
LOGIN_E=$(chamar POST "$V1/auth/login" 200 "{\"email\":\"empresa@demo.com\",\"senha\":\"$SENHA_DEMO\"}" "" "$EMP_JAR")
TOKEN_E=$(jq -r .accessToken <<<"$LOGIN_E")
EMPRESA_ID=$(jq -r .usuario.organizacao.id <<<"$LOGIN_E")
[[ $(jq -r .usuario.tipo <<<"$LOGIN_E") == "EMPRESA" ]] || falhar "login da empresa não retornou tipo EMPRESA"
[[ $(jq -r .usuario.papel <<<"$LOGIN_E") == "PROPRIETARIO" ]] || falhar "a conta de exemplo da empresa deveria ser a proprietária"
grep -q refresh_token "$EMP_JAR" || falhar "login não gravou o cookie refresh_token"
jq -e 'has("refreshToken") | not' <<<"$LOGIN_E" >/dev/null || falhar "refresh token não pode vir no corpo"

LOGIN_F=$(chamar POST "$V1/auth/login" 200 "{\"email\":\"limpabem@demo.com\",\"senha\":\"$SENHA_DEMO\"}" "" "$FORN_JAR")
TOKEN_F=$(jq -r .accessToken <<<"$LOGIN_F")
TOKEN_TECH=$(chamar POST "$V1/auth/login" 200 "{\"email\":\"fornecedor@demo.com\",\"senha\":\"$SENHA_DEMO\"}" | jq -r .accessToken)
chamar POST "$V1/auth/login" 401 '{"email":"empresa@demo.com","senha":"errada123"}' >/dev/null
# Segunda pessoa da mesma empresa: outro papel, mesma organização
LOGIN_COLEGA=$(chamar POST "$V1/auth/login" 200 "{\"email\":\"bruno.compras@demo.com\",\"senha\":\"$SENHA_DEMO\"}")
TOKEN_COLEGA=$(jq -r .accessToken <<<"$LOGIN_COLEGA")
[[ $(jq -r .usuario.papel <<<"$LOGIN_COLEGA") == "MEMBRO" ]] || falhar "o colega da empresa deveria ser MEMBRO"
[[ $(jq -r .usuario.organizacao.id <<<"$LOGIN_COLEGA") == "$EMPRESA_ID" ]] || falhar "o colega deveria estar na mesma empresa"
passo "login (empresa, colega da empresa, fornecedores e senha errada)"

# 1b. Demo de um clique (profile demo) e health check
[[ $(chamar GET "$V1/auth/demo" 200 | jq length) -eq 2 ]] || falhar "esperava 2 contas de demonstração"
[[ $(chamar POST "$V1/auth/demo/FORNECEDOR" 200 | jq -r .usuario.tipo) == "FORNECEDOR" ]] || falhar "login demo do fornecedor falhou"
[[ $(chamar GET "$API/actuator/health" 200 | jq -r .status) == "UP" ]] || falhar "health check não está UP"
passo "login de demonstração em um clique e health check"

# 2. Proteção das rotas
chamar GET "$V1/cotacoes/abertas" 401 >/dev/null
chamar GET "$V1/cotacoes/abertas" 401 "" "token.invalido.aqui" >/dev/null
chamar GET "$V1/cotacoes/categorias" 200 >/dev/null
[[ $(chamar GET "$V1/auth/me" 200 "" "$TOKEN_E" | jq -r .email) == "empresa@demo.com" ]] || falhar "/auth/me não identificou a empresa"
passo "rotas protegidas exigem token válido"

# 3. CORS para o front-end Angular (com credenciais, por causa do cookie)
CORS=$(curl -sS -o /dev/null -D - -X OPTIONS "$V1/cotacoes" \
  -H "Origin: $ORIGEM_FRONT" -H 'Access-Control-Request-Method: POST')
grep -qi "access-control-allow-origin: $ORIGEM_FRONT" <<<"$CORS" || falhar "CORS não liberou $ORIGEM_FRONT"
grep -qi "access-control-allow-credentials: true" <<<"$CORS" || falhar "CORS não liberou credenciais"
passo "CORS liberado para $ORIGEM_FRONT"

# 4. Validação e cadastro (organização e pessoa proprietária juntas)
chamar POST "$V1/cadastro" 400 '{"tipo":"FORNECEDOR","razaoSocial":"","cnpj":"1","nome":"","email":"x","senha":"1"}' >/dev/null
chamar POST "$V1/cadastro" 400 \
  '{"razaoSocial":"Sem Tipo","cnpj":"33.445.566/0001-86","nome":"Fulano","email":"semtipo@teste.com","senha":"senha1234"}' >/dev/null
chamar POST "$V1/cadastro" 400 \
  '{"tipo":"FORNECEDOR","razaoSocial":"Senha Fraca","cnpj":"33.445.566/0001-86","nome":"Fulano","email":"fraca@teste.com","senha":"somenteletras"}' >/dev/null
chamar POST "$V1/cadastro" 400 \
  '{"tipo":"FORNECEDOR","razaoSocial":"CNPJ Errado","cnpj":"11.222.333/0001-00","nome":"Fulano","email":"cnpj@teste.com","senha":"senha1234"}' >/dev/null
chamar POST "$V1/cadastro" 409 \
  '{"tipo":"FORNECEDOR","razaoSocial":"Duplicado","cnpj":"33.445.566/0001-86","nome":"Fulano","email":"empresa@demo.com","senha":"senha1234"}' >/dev/null
chamar POST "$V1/cadastro" 409 \
  '{"tipo":"FORNECEDOR","razaoSocial":"CNPJ Repetido","cnpj":"45.236.789/0001-12","nome":"Fulano","email":"repetido@teste.com","senha":"senha1234"}' >/dev/null
passo "validação de campos, tipo, senha, CNPJ e e-mail ou CNPJ duplicado"

# 5. Regras de perfil
chamar POST "$V1/cotacoes" 403 '{"nomeServico":"X","requisitos":"Y"}' "$TOKEN_F" >/dev/null
chamar GET "$V1/dashboard/empresa" 403 "" "$TOKEN_F" >/dev/null
passo "fornecedor não acessa ações de empresa"

# 6. Empresa publica uma cotação (o dono vem do token)
LIMITE=$(date -d '+7 days' '+%Y-%m-%dT23:59:59')
COTACAO=$(chamar POST "$V1/cotacoes" 201 "$(jq -n --arg d "$LIMITE" \
  '{nomeServico:"Café e descartáveis",requisitos:"Fornecimento mensal",categoria:"ALIMENTOS",orcamentoEstimado:1500,dataLimite:$d}')" "$TOKEN_E")
COTACAO_ID=$(jq -r .id <<<"$COTACAO")
[[ $(jq -r .empresaId <<<"$COTACAO") == "$EMPRESA_ID" ]] || falhar "cotação não ficou em nome da empresa do token"
chamar GET "$V1/cotacoes/abertas" 200 "" "$TOKEN_F" | jq -e --arg id "$COTACAO_ID" 'any(.[]; .id == $id)' >/dev/null \
  || falhar "cotação nova não aparece no mural"
passo "cotação publicada e visível no mural"

# 7. Propostas: uma por fornecedor, e um não vê a do outro
PROPOSTA=$(chamar POST "$V1/propostas" 201 "{\"valor\":1400,\"descricao\":\"Entrega semanal\",\"cotacaoId\":\"$COTACAO_ID\"}" "$TOKEN_F")
PROPOSTA_ID=$(jq -r .id <<<"$PROPOSTA")
chamar POST "$V1/propostas" 400 "{\"valor\":1300,\"descricao\":\"De novo\",\"cotacaoId\":\"$COTACAO_ID\"}" "$TOKEN_F" >/dev/null
chamar GET "$V1/propostas/$PROPOSTA_ID" 403 "" "$TOKEN_TECH" >/dev/null
chamar GET "$V1/propostas/cotacao/$COTACAO_ID" 403 "" "$TOKEN_F" >/dev/null
[[ $(chamar GET "$V1/propostas/cotacao/$COTACAO_ID" 200 "" "$TOKEN_E" | jq length) -eq 1 ]] || falhar "empresa não viu a proposta"
passo "proposta enviada; duplicada e espionagem bloqueadas"

# 8. Negociação
NEGOCIACAO=$(chamar POST "$V1/negociacoes" 201 "{\"propostaId\":\"$PROPOSTA_ID\"}" "$TOKEN_E")
NEGOCIACAO_ID=$(jq -r .id <<<"$NEGOCIACAO")
[[ $(jq -r .cotacaoStatus <<<"$NEGOCIACAO") == "EM_NEGOCIACAO" ]] || falhar "cotação não foi para EM_NEGOCIACAO"
chamar GET "$V1/negociacoes/$NEGOCIACAO_ID" 403 "" "$TOKEN_TECH" >/dev/null
passo "negociação aberta e restrita aos participantes"

# 9. Mensagens e contrapropostas (remetente vem do token)
chamar POST "$V1/mensagens" 201 "{\"negociacaoId\":\"$NEGOCIACAO_ID\",\"mensagem\":\"Fecha por 1.300?\",\"valorOfertado\":1300}" "$TOKEN_E" >/dev/null
chamar POST "$V1/mensagens" 201 "{\"negociacaoId\":\"$NEGOCIACAO_ID\",\"valorOfertado\":1350}" "$TOKEN_F" >/dev/null
chamar POST "$V1/mensagens" 201 "{\"negociacaoId\":\"$NEGOCIACAO_ID\",\"mensagem\":\"Pode ser 1.350.\"}" "$TOKEN_COLEGA" >/dev/null
chamar POST "$V1/mensagens" 403 "{\"negociacaoId\":\"$NEGOCIACAO_ID\",\"mensagem\":\"intruso\"}" "$TOKEN_TECH" >/dev/null
MENSAGENS=$(chamar GET "$V1/mensagens/negociacao/$NEGOCIACAO_ID" 200 "" "$TOKEN_F")
[[ $(jq length <<<"$MENSAGENS") -eq 4 ]] || falhar "esperava 4 mensagens no histórico, veio $(jq length <<<"$MENSAGENS")"
[[ $(jq -r '.[2].tipoRemetente' <<<"$MENSAGENS") == "FORNECEDOR" ]] || falhar "remetente não veio do token"
[[ $(jq -r '.[1].remetentePessoa' <<<"$MENSAGENS") == "Ana Ribeiro" ]] || falhar "a mensagem deveria trazer a pessoa que escreveu"
[[ $(jq -r '.[3].remetentePessoa' <<<"$MENSAGENS") == "Bruno Costa" ]] || falhar "a mensagem do colega deveria trazer o nome dele"
ULTIMA=$(chamar GET "$V1/negociacoes/$NEGOCIACAO_ID" 200 "" "$TOKEN_E" | jq -r .ultimaOferta)
[[ "$ULTIMA" == "1350" || "$ULTIMA" == "1350.00" || "$ULTIMA" == "1350.0" ]] || falhar "última oferta deveria ser 1350, veio $ULTIMA"
passo "mensagens, contraproposta e bloqueio de intruso"

# 10. Só a empresa fecha o negócio
chamar PATCH "$V1/negociacoes/$NEGOCIACAO_ID/finalizar" 403 '{"valorFinal":1350}' "$TOKEN_F" >/dev/null
FINAL=$(chamar PATCH "$V1/negociacoes/$NEGOCIACAO_ID/finalizar" 200 '{"valorFinal":1350}' "$TOKEN_E")
[[ $(jq -r .status <<<"$FINAL") == "FINALIZADA" ]] || falhar "negociação não finalizou"
[[ $(chamar GET "$V1/cotacoes/$COTACAO_ID" 200 "" "$TOKEN_E" | jq -r .status) == "FECHADA" ]] || falhar "cotação não fechou"
passo "negócio fechado e cotação encerrada"

# 11. Dashboards
[[ $(chamar GET "$V1/dashboard/fornecedor" 200 "" "$TOKEN_F" | jq -r .cotacoesGanhas) -ge 1 ]] || falhar "dashboard do fornecedor sem cotação ganha"
DASH_E=$(chamar GET "$V1/dashboard/empresa" 200 "" "$TOKEN_E")
[[ $(jq -r .cotacoesFechadas <<<"$DASH_E") -ge 1 ]] || falhar "dashboard da empresa sem cotação fechada"
[[ $(jq '.categorias | length' <<<"$DASH_E") -ge 1 ]] || falhar "dashboard da empresa sem categorias"
passo "dashboards de empresa e fornecedor"

# 12. Sessão: refresh pelo cookie, rotação, reuso e logout
cp "$EMP_JAR" "$COOKIES/antigo"
RENOVADO=$(chamar POST "$V1/auth/refresh" 200 "" "" "$EMP_JAR")
[[ $(jq -r .accessToken <<<"$RENOVADO") != "null" ]] || falhar "refresh não devolveu access token"
chamar GET "$V1/auth/me" 200 "" "$(jq -r .accessToken <<<"$RENOVADO")" >/dev/null
chamar POST "$V1/auth/refresh" 401 "" "" "$COOKIES/antigo" >/dev/null     # cookie antigo: reuso detectado
chamar POST "$V1/auth/refresh" 401 "" "" "$EMP_JAR" >/dev/null            # e a sessão inteira caiu
chamar POST "$V1/auth/logout" 204 "" "" "$FORN_JAR" >/dev/null
chamar POST "$V1/auth/refresh" 401 "" "" "$FORN_JAR" >/dev/null
passo "refresh com rotação, detecção de reuso e logout"

# 13. Força bruta: 5 erros bloqueiam o login daquele e-mail
for _ in 1 2 3 4 5; do
  chamar POST "$V1/auth/login" 401 '{"email":"infoworld@demo.com","senha":"errada123"}' >/dev/null
done
chamar POST "$V1/auth/login" 429 "{\"email\":\"infoworld@demo.com\",\"senha\":\"$SENHA_DEMO\"}" >/dev/null
passo "bloqueio de força bruta no login"

# 14. Erros tratados e documentação
chamar GET "$V1/cotacoes/nao-e-uuid" 400 "" "$TOKEN_E" >/dev/null
chamar GET "$V1/cotacoes/00000000-0000-0000-0000-000000000000" 404 "" "$TOKEN_E" >/dev/null
chamar GET "$API/api-docs" 200 >/dev/null
passo "erros 400/404 e documentação OpenAPI pública"

echo "Todos os testes de fumaça passaram."
