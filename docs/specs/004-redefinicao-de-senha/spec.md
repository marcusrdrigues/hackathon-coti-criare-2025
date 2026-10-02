# 004 · Redefinição de senha

- **Status:** Concluída
- **Data:** 2026-10-02
- **Plano:** [plan.md](plan.md) · **Tarefas:** [tasks.md](tasks.md)

## Problema

Quem esquece a senha não tem como voltar a entrar no portal:

- **A tela de login não tem "Esqueci minha senha".** A única saída é pedir ao proprietário que remova a pessoa e mande um convite novo, o que apaga o vínculo dela com a equipe. Se quem esqueceu é o próprio proprietário, a organização fica sem saída.
- **O portal não envia e-mail.** Nenhum fluxo hoje precisa provar que a pessoa é dona do e-mail depois do cadastro.

## Objetivos

- Qualquer pessoa com conta ativa consegue trocar a senha sozinha, provando que recebe o e-mail da conta.
- O pedido não revela quais e-mails têm conta.
- Um link de redefinição vale por pouco tempo, uma vez só, e não serve para mais nada.
- Depois da troca, toda sessão aberta com a senha antiga deixa de valer.

## Fora do escopo

- **Login com Google ou outro provedor.** Para um portfólio, o custo (configuração do OAuth, tela de consentimento, vínculo com a organização e com as contas de exemplo) não compensa o ganho. Fica para quando houver uso real.
- **Trocar a senha estando logado** (em "Minha conta"). É outro fluxo, que pede a senha atual; ganha spec própria se fizer falta.
- **Confirmar o e-mail no cadastro.** O cadastro continua como está.
- **O superadmin.** A senha dele vem da configuração do servidor e voltaria ao valor configurado na próxima subida.
- **Outros e-mails do portal** (avisos de proposta, de mensagem). O envio de e-mail nasce aqui de forma que outros possam vir depois, cada um com a sua spec.

## Requisitos

### R1 · Pedir a redefinição (Deve)

**Critérios de aceite**

- **Dado** a tela de login, **então** ela tem o link "Esqueci minha senha", que abre uma tela pedindo só o e-mail.
- **Dado** um e-mail com conta ativa, **quando** a pessoa pede a redefinição, **então** recebe um e-mail com um link para criar a senha nova.
- **Dado** qualquer e-mail, com conta ou sem, **quando** a pessoa pede a redefinição, **então** a resposta da tela é a mesma ("Se houver uma conta com este e-mail, enviamos um link"), e o e-mail sai depois, fora da requisição.
- **Dado** um e-mail sem conta ativa, uma conta de exemplo da demo ou o superadmin, **quando** a pessoa pede a redefinição, **então** nenhum e-mail é enviado, e a resposta é a mesma.
- **Dado** um mesmo e-mail, **quando** os pedidos passam de 3 em uma hora, **então** os seguintes não enviam e-mail até a hora passar, e a resposta continua a mesma.

### R2 · O link de redefinição (Deve)

**Critérios de aceite**

- **Dado** um link enviado, **então** ele vale por 30 minutos e uma vez só.
- **Dado** um pedido novo para a mesma conta, **então** o link anterior deixa de valer: só o mais recente funciona.
- **Dado** o banco de dados, **então** ele guarda só o hash do token do link, nunca o token.
- **Dado** o link, **então** o token não vai na parte da URL que chega ao servidor nem aos logs de acesso.
- **Dado** um link vencido, já usado, substituído ou inventado, **quando** a pessoa o abre, **então** vê que o link não vale mais e pode pedir outro, sem saber qual dos casos aconteceu.

### R3 · Criar a senha nova (Deve)

**Critérios de aceite**

- **Dado** um link válido, **quando** a pessoa define a senha nova (com as mesmas regras do cadastro) e confirma, **então** a senha é trocada, o link deixa de valer e a pessoa volta para o login com o aviso "Senha alterada. Entre com a senha nova.".
- **Dado** a troca, **então** todas as sessões abertas da pessoa deixam de valer, em qualquer aparelho, e as conexões em tempo real abertas em nome dela fecham. O acesso já emitido vence sozinho em até 15 minutos.
- **Dado** um aparelho com a sessão de antes da troca, **quando** ele tenta renovar a sessão, **então** recebe "sessão expirada", sem derrubar a sessão aberta com a senha nova.
- **Dado** a troca, **então** o bloqueio por excesso de tentativas de login daquele e-mail é zerado.
- **Dado** uma senha nova que não segue as regras, **então** a tela mostra o erro no campo, e o link continua valendo.

### R4 · Registro (Deve)

**Critérios de aceite**

- **Dado** um pedido de redefinição para uma conta ativa e uma senha redefinida, **então** cada um vira um evento de segurança (spec 003, R2), com o e-mail mascarado.
- **Dado** os registros e os logs, **então** em nenhum deles aparece o token, o link nem a senha.

### R5 · E-mail (Deve)

**Critérios de aceite**

- **Dado** o e-mail de redefinição, **então** ele está em português, diz para que serve, traz o link e o prazo, e avisa que, se a pessoa não pediu, pode ignorar e a senha continua a mesma.
- **Dado** o servidor sem o envio de e-mail configurado, **então** o pedido responde igual, nada é enviado, e o log avisa que o envio está desligado (sem o link).
- **Dado** uma falha no envio, **então** a resposta da tela é a mesma, e a falha vai para o log com o rastreio.

## Requisitos não funcionais

- **Segurança:** token aleatório de 256 bits; comparação pelo hash; links e senhas fora dos logs; a rota de pedido não revela se o e-mail existe, nem pela mensagem, nem pelo código de resposta.
- **Privacidade:** o e-mail da pessoa passa por um provedor de envio de e-mail, que entra na [política de dados](../../dados.md) como operador.
- **Acessibilidade:** as duas telas seguem o padrão das telas de acesso (rótulos, foco, mensagens anunciadas).
- **Qualidade:** cada critério de aceite vira um teste; nenhum teste envia e-mail de verdade.

## Decisões tomadas

Resolvidas antes da implementação, em 2026-10-02:

1. **Provedor de e-mail: Brevo.** A hospedagem gratuita da API bloqueia as portas de SMTP, então o envio é pela API HTTP de um provedor. A Brevo envia 300 e-mails por dia no plano gratuito e aceita um remetente verificado por e-mail, sem domínio próprio. O código fica atrás de uma porta: trocar de provedor é trocar um adaptador.
2. **O link vale por 30 minutos.** Quem pede a redefinição quer entrar agora.
3. **Segurança na medida de um portfólio**, como na spec 003. O tempo de resposta do pedido não é igualado com conta ou sem (o cadastro já diz se um e-mail está em uso), o limite é por e-mail e não por endereço de rede, e o acesso já emitido não é revogado na hora (vence em até 15 minutos). Com um uso real, vale rever os três.
