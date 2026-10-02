# 0021. Auditoria com Hibernate Envers e eventos de segurança em tabela própria

- **Status:** Aceita
- **Data:** 2026-10-02
- **Spec:** [003 · Auditoria e administração](../specs/003-auditoria-e-administracao/spec.md)

## Contexto

O portal guardava só o estado atual de cada negócio. Uma cotação editada perdia o prazo anterior, uma proposta retirada sumia do banco, e ninguém sabia qual pessoa da equipe tinha feito o quê. A spec 003 pede o histórico de alterações (quem, quando, o valor anterior), visto pelo proprietário em linguagem de negócio e pelo superadmin por inteiro, e também os eventos de segurança fora do log.

São dois tipos de registro com naturezas diferentes:

- **Mudança de dado**: acontece dentro da transação do negócio e precisa do estado do registro.
- **Evento de segurança**: login, falha, bloqueio. Muitas vezes acontece numa operação que é desfeita (a senha errada desfaz o login) e não muda dado nenhum.

## Decisão

**Histórico de alterações com o Hibernate Envers:**

- `@Audited` nas entidades de organização, pessoa, vínculo de equipe, convite, cotação, proposta e negociação. `@NotAudited` nas credenciais (hash da senha, hash do token do convite) e nas marcas de leitura da negociação, que mudam a cada visita.
- **Uma revisão por transação**, numa entidade própria (`tb_revisao`): momento, pessoa, organização dela, identificador de rastreio e origem (`PESSOA`, `PUBLICO` para o cadastro e o aceite de convite, `SISTEMA` para rotinas). Um `RevisionListener` lê o usuário do contexto de segurança, como o resto da API.
- A revisão guarda quais entidades mudaram (`tb_revisao_entidade`), para a atividade da organização ler por revisão, sem varrer todas as tabelas.
- Na exclusão, o histórico guarda o último estado do registro (`store_data_at_delete`).
- As tabelas `_aud` nascem por migração do Flyway (V6), como o resto do esquema, sem chave estrangeira para as tabelas de negócio: o histórico continua válido depois que o registro some.
- A anotação do Envers no domínio é aceita pela regra de camadas, como as do JPA.

**Eventos de segurança em tabela própria** (`tb_evento_seguranca`, migração V7):

- Quem sabe do fato publica um `EventoDeSeguranca` (módulo compartilhado): o login e a renovação de sessão, a equipe, o superadmin da configuração e as duas origens do 403 (o `@PreAuthorize`, pelo tratador de exceções, e a regra da rota, pelo filtro do Spring Security).
- O módulo `auditoria` ouve e grava **na hora, numa transação separada** (`REQUIRES_NEW`). Assim, o evento fica registrado mesmo quando a operação que o causou é desfeita: a senha errada desfaz o login, o token reutilizado responde 401.
- Se a gravação falhar, a ação principal segue e a falha vai para o log com o rastreio. Derrubar um login porque a auditoria caiu tiraria o portal do ar por um problema secundário.
- O evento traz a pessoa e a organização quando se sabe quem é; sem isso, o ouvinte completa com quem está autenticado na requisição. O e-mail entra só mascarado, e nenhum campo guarda credencial.
- A porta de gravação só acrescenta: não tem método de alterar nem de apagar.

## Alternativas consideradas

- **Tabela de auditoria escrita à mão nos services:** controle total do formato, mas cada caso de uso precisaria lembrar de registrar, e um esquecimento passaria em silêncio. O Envers registra tudo o que muda nas entidades auditadas, sem depender de quem escreve o caso de uso.
- **Gatilhos no banco (triggers):** pegam até alteração fora da aplicação, mas não sabem quem é a pessoa nem o rastreio da requisição, e espalham regra de negócio pelo SQL.
- **Captura de mudanças (CDC, Debezium):** robusto para muitos sistemas consumindo, mas pede infraestrutura que um portal deste porte não justifica.
- **Eventos de segurança também pelo Envers:** não há entidade mudando num login errado, e a revisão seria desfeita junto com a operação.

## Consequências

- Toda entidade nova que represente uma decisão de negócio entra com `@Audited` e com a tabela `_aud` na mesma migração. Campo de credencial entra com `@NotAudited`.
- Testes do histórico não podem ser `@Transactional`: o Envers grava na confirmação da transação.
- Um evento de segurança fica gravado mesmo que a transação de quem publicou falhe depois de publicar. Para os eventos de sucesso (convite criado), isso é raro e aceitável; para as falhas, é justamente o que se quer.
- Cada ação com evento abre uma segunda conexão com o banco por um instante, para a transação própria.
- Uma alteração feita direto pelo SQL (como o reset da demo) não aparece no histórico. É uma escolha: o histórico conta o que a aplicação fez.
- O histórico fica no mesmo banco. O mesmo vale para os eventos. A proteção contra alteração é responsabilidade do passo 4 da spec 003, e protege contra erro da aplicação, não contra quem administra o banco.
