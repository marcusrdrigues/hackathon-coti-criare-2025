# 0009. Demo pública com contas prontas e dados restaurados todo dia

- **Status:** Aceita
- **Data:** 2026-10-01

## Contexto

O projeto é vitrine: quem visita (recrutador, outro dev) precisa ver a negociação funcionando em segundos, sem se cadastrar duas vezes (empresa e fornecedor) e montar dados. Por ser público, alguém vai apagar ou bagunçar os dados.

## Decisão

- Um profile `demo` do Spring cria duas empresas, quatro fornecedores, cotações, propostas, uma negociação em andamento e um negócio fechado (`DadosDemonstracao`).
- A tela de login oferece os botões "Como empresa" e "Como fornecedor" (`/api/v1/auth/demo/{perfil}`), sem digitar senha. As contas também aceitam login normal (`demo1234`).
- Todo dia às 4h (horário de Brasília) os dados de exemplo são restaurados (`DEMO_RESET_CRON`).
- O controller de demonstração só existe com o profile ativo (`@Profile("demo")`). Fora dele, as rotas não existem.

## Alternativas consideradas

- **Só um vídeo ou GIF**: mostra, mas não deixa explorar. Fica como complemento (roadmap).
- **Credenciais no README**: funciona, mas pede dois logins e digitação, e muita gente desiste antes.

## Consequências

- A demo se recupera sozinha de qualquer bagunça em até um dia.
- O reset apaga tudo, inclusive contas e cotações criadas por visitantes. A demo não guarda dados reais, e a tela de login avisa que os dados são restaurados todo dia.
- O plano gratuito do Render dorme sem uso, e o primeiro acesso pode levar até um minuto. A tela de login avisa quando o servidor está demorando.
