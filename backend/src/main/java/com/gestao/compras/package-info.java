/**
 * Módulo Compras: O negócio: cotações, propostas, negociações e mensagens. Cotação, proposta e negociação
 * ficam no mesmo módulo porque uma mesma transação altera as três (ver docs/adr/0013).
 *
 * <p>Camadas: {@code dominio} (entidades e regras), {@code aplicacao} (casos de uso e portas) e
 * {@code infraestrutura} (web, persistência e integrações). Outros módulos só usam o que está
 * exposto como interface nomeada.
 */
@ApplicationModule(displayName = "Compras")
package com.gestao.compras;

import org.springframework.modulith.ApplicationModule;
