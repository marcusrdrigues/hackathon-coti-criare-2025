# Especificações (Spec-Driven Development)

Toda funcionalidade ou mudança estrutural começa por uma especificação, e só depois vira código. A especificação é a fonte da verdade: se o comportamento precisa mudar, ela muda primeiro.

## Como funciona

Cada trabalho ganha uma pasta `NNN-nome-curto/` com três arquivos:

| Arquivo | Responde | Quem aprova |
|---|---|---|
| `spec.md` | **O quê e por quê**: problema, objetivos, requisitos e critérios de aceite. Não fala de tecnologia. | Dono do produto, antes de qualquer código |
| `plan.md` | **Como**: arquitetura, modelo de dados, decisões técnicas e riscos. Decisões importantes viram [ADR](../adr/README.md). | Revisão técnica |
| `tasks.md` | **Em que ordem**: passos pequenos, cada um entregue com o CI verde. | Acompanhado a cada entrega |

Fluxo:

1. Escrever `spec.md` com critérios de aceite verificáveis (*Dado / Quando / Então*).
2. Aprovar a spec. Dúvidas abertas são resolvidas aqui, não no meio do código.
3. Escrever `plan.md` e `tasks.md`.
4. Implementar passo a passo. Cada critério de aceite vira pelo menos um teste automatizado.
5. Ao terminar, marcar a spec como **Concluída** e atualizar o roadmap do README.

Os requisitos têm prioridade **Deve** (obrigatório para concluir), **Deveria** (importante, pode ficar para um passo seguinte) ou **Pode** (desejável).

Este repositório é público: as specs descrevem comportamento e decisões, nunca credenciais, endereços internos ou detalhes que facilitem um ataque.

## Specs

| # | Spec | Status |
|---|---|---|
| [001](001-fundacao-da-arquitetura/spec.md) | Fundação da arquitetura: módulos, identidade com organizações, autorização, observabilidade e padrões de API | Concluída |
| [002](002-experiencia-da-negociacao/spec.md) | Experiência da negociação e escrita da interface: barra lateral recolhível, oferta com botão próprio e revisão dos textos | Concluída |
| [003](003-auditoria-e-administracao/spec.md) | Auditoria e administração: histórico de alterações, eventos de segurança, atividade da organização para o proprietário, console do superadmin e registro de consumo de IA | Aprovada · em implementação |
| [004](004-redefinicao-de-senha/spec.md) | Redefinição de senha: "Esqueci minha senha" no login, link de uso único por e-mail e sessões encerradas depois da troca | Rascunho |

Modelo para uma spec nova: [template.md](template.md).
