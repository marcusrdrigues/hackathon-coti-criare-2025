<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:000000,100:241773&height=120&section=header&text=Hackathon%20Coti%20%C3%97%20Criare%202025&fontColor=FFFFFF&fontSize=32&fontAlignY=38" width="100%" />
</div>

<h3 align="center">🏆 1º lugar · 24 horas · equipe Javangers</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Java_21-0A0A0A?style=for-the-badge&logo=openjdk&logoColor=A78BFA" />
  <img src="https://img.shields.io/badge/Spring_Boot_4-0A0A0A?style=for-the-badge&logo=springboot&logoColor=A78BFA" />
  <img src="https://img.shields.io/badge/PostgreSQL-0A0A0A?style=for-the-badge&logo=postgresql&logoColor=A78BFA" />
  <img src="https://img.shields.io/badge/Angular_21-0A0A0A?style=for-the-badge&logo=angular&logoColor=A78BFA" />
  <img src="https://img.shields.io/badge/Bootstrap-0A0A0A?style=for-the-badge&logo=bootstrap&logoColor=A78BFA" />
</p>

---

## 🎯 O desafio

Em dezembro de 2025, a **Coti Informática** e a **Criare Sistemas** propuseram um hackathon de 24 horas de desenvolvimento contínuo. Nossa equipe de três pessoas construiu a solução vencedora: uma **plataforma de cotações e negociação entre empresas e fornecedores**.

## 🧩 A solução

| Fluxo | Como funciona |
|---|---|
| **Perfis** | Empresa e fornecedor entram com perfis diferentes, cada um com seu próprio dashboard |
| **Cotações** | A empresa publica o que precisa comprar e acompanha o status de cada cotação |
| **Propostas** | Fornecedores enviam propostas, e a empresa vê o histórico e compara |
| **Negociação** | Empresa e fornecedor trocam mensagens dentro da plataforma até fechar ou encerrar o negócio |

## 🏗️ Estrutura

```text
hackathon-coti-criare-2025/
├── backend/    API REST · Spring Boot 4 · JPA + PostgreSQL · Swagger · CORS
└── frontend/   SPA · Angular 21 · Bootstrap · telas por perfil
```

**Backend:** entidades `Empresa`, `Fornecedor`, `Cotacao`, `Proposta`, `Negociacao` e `MensagemNegociacao`, com status controlados por *enums*, DTOs de entrada e saída, *mappers*, regras de negócio com `BusinessException` e documentação via Swagger.

**Frontend:** login e cadastro, dashboards de empresa e fornecedor, cadastro e consulta de cotações, detalhe da cotação e histórico de propostas.

---

## ▶️ Como rodar

```bash
# Backend
cd backend
./mvnw spring-boot:run          # Swagger: http://localhost:8080/swagger-ui.html

# Frontend
cd frontend
npm install && ng serve         # http://localhost:4200
```

> ℹ️ A configuração aponta para um PostgreSQL local de desenvolvimento.

---

## 👥 Equipe Javangers

**Marcus Rodrigues** · **Gercinildo Santos** · **Carlos Ferreira**

<p align="center">
  Repositório unificado a partir dos repositórios originais do back-end e do front-end, com o <b>histórico de commits preservado</b>.<br/>
  <a href="https://github.com/marcusrdrigues">← Voltar ao perfil</a>
</p>

<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:241773,100:000000&height=90&section=footer" width="100%" />
</div>
