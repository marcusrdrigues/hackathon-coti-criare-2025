# 0014. Rastreio por requisição e logs estruturados, sem dados sensíveis

- **Status:** Aceita
- **Data:** 2026-10-02

## Contexto

Os logs eram texto livre, sem nada que ligasse as linhas de uma mesma requisição. Quando alguém relatava um erro, não havia como achar o que aconteceu. As próximas fases pioram isso: a migração de pessoas e organizações precisa ser acompanhada em produção, e a fase de IA vai precisar medir latência e custo por chamada.

Os logs também são um destino de dados: o que é sigiloso na API (senha, token, valores e condições de uma proposta, mensagens de uma negociação) não pode reaparecer neles.

## Decisão

- **OpenTelemetry pelo Micrometer Tracing** (`spring-boot-starter-opentelemetry`): cada requisição HTTP ganha um trace, e o `traceId` vai para o MDC, ou seja, para toda linha de log daquela requisição.
- O filtro `RastreioDeRequisicao` devolve o `traceId` no cabeçalho **`X-Trace-Id`**, antes do Spring Security, para valer também nos 401 e 403. Sem tracing ativo (nos testes, por exemplo), ele mesmo gera um id no mesmo formato. **O valor nunca vem do cliente**, para ninguém injetar texto nos logs.
- Os erros da API trazem o mesmo **`traceId` no corpo**, e o front-end mostra o começo dele quando uma falha é do servidor: "Código para o suporte: 4bf92f35".
- **Logs em JSON (formato ECS) em produção**, ligados por variável de ambiente (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`). Em desenvolvimento, texto com o `traceId` entre colchetes.
- **Traces não são exportados** até alguém configurar um coletor (`MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT`). O envio de métricas por OTLP fica desligado.
- **O que nunca vai para log:** senha, tokens, e-mail completo (vai mascarado, `m***@empresa.com`) e o conteúdo de propostas e mensagens. Erros esperados são avisos (`WARN`); só o inesperado é `ERROR`, com a pilha no log e uma mensagem genérica para quem chamou.

Verificação: o `RastreioELogsTest` percorre cadastro, senha errada, proposta e mensagem e falha se a saída tiver qualquer um desses dados, ou se faltar o `traceId`. No CI, a API sobe com logs em JSON e um passo confere o cabeçalho e o formato.

## Alternativas consideradas

- **Só um filtro próprio com id de correlação**: resolveria os logs, mas a fase de IA precisaria de tracing de verdade (spans, latência, exportação), e o Spring AI já publica observações para o Micrometer.
- **Aceitar o `X-Request-Id` enviado pelo cliente**: comum atrás de um proxy confiável, mas aqui o cliente é o navegador, e o valor iria direto para os logs.
- **Mascarar dados num appender de log, por expressão regular**: pega o que foge à regra, mas é caro e frágil. A regra é não logar o dado, e o teste garante.

## Consequências

- Uma reclamação ("deu erro, código 4bf92f35") vira uma busca nos logs.
- Ligar a exportação de traces para um coletor (Grafana, Jaeger, Honeycomb) é só configuração.
- Mensagens STOMP do WebSocket e tarefas agendadas não passam por uma requisição HTTP e não carregam `traceId`. Se for preciso, um interceptor de canal resolve.
- Quem escreve um log novo precisa respeitar a lista do que não pode ir para lá. O teste pega os casos mais comuns, não todos.
