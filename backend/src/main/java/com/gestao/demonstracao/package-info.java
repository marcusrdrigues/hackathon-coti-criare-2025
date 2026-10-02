/**
 * Módulo Demonstração: Contas e dados de exemplo da demo pública, restaurados todo dia.
 *
 * <p>Camadas: {@code dominio} (entidades e regras), {@code aplicacao} (casos de uso e portas) e
 * {@code infraestrutura} (web, persistência e integrações). Outros módulos só usam o que está
 * exposto como interface nomeada.
 */
@ApplicationModule(displayName = "Demonstração")
package com.gestao.demonstracao;

import org.springframework.modulith.ApplicationModule;
