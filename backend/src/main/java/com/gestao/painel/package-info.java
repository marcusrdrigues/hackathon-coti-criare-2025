/**
 * Módulo Painel: Números dos painéis da empresa e do fornecedor. Só leitura, com consultas próprias
 * (o lado de leitura do CQRS, no mesmo banco).
 *
 * <p>Camadas: {@code dominio} (entidades e regras), {@code aplicacao} (casos de uso e portas) e
 * {@code infraestrutura} (web, persistência e integrações). Outros módulos só usam o que está
 * exposto como interface nomeada.
 */
@ApplicationModule(displayName = "Painel")
package com.gestao.painel;

import org.springframework.modulith.ApplicationModule;
