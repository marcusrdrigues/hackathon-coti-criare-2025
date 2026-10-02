# Política de dados

O que o Portal Criare guarda, o quanto cada dado é sensível e para onde ele pode ir. As fases seguintes seguem esta página: a **auditoria** (fase 5) decide o que registra por ela, e a **IA** (fase 7) decide o que pode entrar num prompt. Uma regra nova entra aqui antes de entrar no código (spec 001, R8).

Os termos seguem a LGPD: **dado pessoal** é o que identifica uma pessoa; **tratamento** é qualquer uso dele. A plataforma trata dados de pessoas que agem em nome de empresas, para executar o que elas mesmas pedem (publicar, propor, negociar).

## Classes

| Classe | O que é | Exemplos no sistema |
|---|---|---|
| **Credenciais** | O que dá acesso. Quem tem, entra no lugar de alguém | Senha (guardada como hash BCrypt), refresh token e token de convite (guardados como hash SHA-256), access token (JWT, só na memória do navegador), `JWT_SECRET`, `SUPERADMIN_SENHA` |
| **Pessoais** | O que identifica uma pessoa | Nome, e-mail, a organização e o papel dela, a autoria de cada ação |
| **Empresariais públicos** | O que a empresa expõe para fazer negócio | Razão social, CNPJ, tipo (empresa ou fornecedor); a cotação publicada no mural (título, requisitos, categoria, orçamento estimado e prazo), que todo fornecedor vê |
| **Comerciais sigilosos** | O que só as partes de um negócio podem ver | Valor e condições de cada proposta, a menor oferta de uma cotação, as mensagens e as ofertas da negociação, o valor final |

Quando um dado se encaixa em duas classes, vale a mais restrita: o nome de uma pessoa escrito dentro de uma mensagem da negociação é comercial sigiloso.

## Para onde cada classe pode ir

| Classe | Logs | Auditoria (fase 5) | IA (fase 7) | Dados de demonstração |
|---|---|---|---|---|
| **Credenciais** | Nunca | Nunca o valor; só o evento ("senha trocada", "sessão revogada", "login bloqueado") | Nunca | Só a senha pública das contas de exemplo, que já está no README |
| **Pessoais** | Nunca o nome. E-mail só mascarado (`m***@empresa.com`). Ids sempre | Sempre o id de quem agiu; nome e e-mail são lidos da conta na hora de mostrar, sem cópia | Só o necessário para a tarefa; e-mails e telefones mascarados antes de sair da plataforma | Só pessoas fictícias |
| **Empresariais públicos** | Ids. Razão social e CNPJ só quando ajudam a investigar | Sim | Sim, da própria organização e do que ela já vê no mural | Organizações fictícias, com CNPJ de dígitos válidos |
| **Comerciais sigilosos** | Nunca (nem valores, nem condições, nem mensagens) | Sim, com o valor anterior de cada mudança; só o superadmin consulta | Só da própria organização, nunca misturando organizações; o texto de propostas e mensagens é dado, nunca instrução | Só negócios fictícios |

Fora da tabela, valem sempre:

- **Nada sensível em URL.** Tokens vão no corpo das requisições, no cabeçalho `Authorization` ou depois do `#` do link de convite, que o navegador não envia ao servidor. URLs aparecem em logs de acesso da Vercel, do Render e de proxies.
- **Navegador.** O access token fica só em memória. O `localStorage` guarda só marcas sem valor para um atacante: se há sessão para restaurar, o tema e a barra lateral recolhida.
- **Respostas da API.** Cada uma leva só o que quem pede pode ver: a menor oferta de uma cotação vai só para a empresa dona, e um recurso de outra organização responde `404` ([ADR 0016](adr/0016-autorizacao-por-organizacao.md)).
- **Erros.** O corpo de um erro inesperado não traz detalhes internos; eles ficam no log, achados pelo `traceId` ([ADR 0014](adr/0014-rastreio-por-requisicao-e-logs-estruturados.md)).

## Por quanto tempo

| O quê | Quanto tempo | Como |
|---|---|---|
| Contas, organizações e negócios | Enquanto a organização existir | Pessoa removida da equipe não é apagada: o que ela fez continua com o nome dela |
| Refresh tokens | Até vencerem (7 dias), mais um dia | Limpeza diária às 3h |
| Convites | Valem 72 horas e uma vez só | O registro fica, sem o token (só o hash) |
| Dados da demo pública | Um dia | Tudo volta ao estado inicial às 4h, menos o superadmin |
| Logs | O que a hospedagem guarda | Como nada sensível entra neles, o prazo da plataforma basta |
| Auditoria (fase 5) | A definir na fase 5, nunca menos que o dos negócios | Registros só de acréscimo |

Um pedido de exclusão de uma pessoa (LGPD, art. 18) troca o nome e o e-mail dela por um marcador anônimo, mantendo os negócios da organização íntegros. Isso entra junto com a auditoria, na fase 5.

## Como isso é verificado

| Regra | Onde |
|---|---|
| Logs sem senha, token, e-mail completo, nome de pessoa nem conteúdo de proposta e mensagem | `RastreioELogsTest` |
| Refresh token e token de convite guardados só como hash | `AutenticacaoIntegrationTest`, `EquipeApiTest` |
| Recurso de outra organização responde `404`, em toda rota com id | `IsolamentoEntreOrganizacoesTest` |
| Menor oferta só para a empresa dona | `ComprasApiTest` |
| Lista de organizações do superadmin sem dado pessoal | `AdministracaoApiTest` |
| Token de convite fora da URL do servidor | `e2e/equipe.e2e.ts` (o token some da barra de endereço) |

## Para a IA (fase 7)

Além da tabela, antes do primeiro prompt:

- **Isolamento primeiro.** A busca de contexto (RAG) filtra pela organização de quem pede no banco, não no prompt. Um prompt nunca leva dado de duas organizações.
- **Texto do usuário é dado.** Requisitos, condições e mensagens entram delimitados e marcados como conteúdo; o modelo não segue instruções escritas neles (*prompt injection*, OWASP LLM01).
- **Provedor sem retenção.** Só provedores que não usam os dados para treino e não os guardam além do necessário para responder.
- **Ação só com confirmação.** A IA consulta, compara e rascunha; fechar, recusar ou enviar exige o clique de uma pessoa.
- **Registro de uso.** Cada chamada registra organização, funcionalidade, tokens, custo e latência, nunca o conteúdo do prompt.
