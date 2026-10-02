/**
 * Módulo Identidade: Contas de empresas e fornecedores, login, sessões e segurança da API.
 *
 * <p>Camadas: {@code dominio} (entidades e regras), {@code aplicacao} (casos de uso e portas) e
 * {@code infraestrutura} (web, persistência e integrações). Outros módulos só usam o que está
 * exposto como interface nomeada.
 */
@ApplicationModule(displayName = "Identidade")
package com.gestao.identidade;

import org.springframework.modulith.ApplicationModule;
