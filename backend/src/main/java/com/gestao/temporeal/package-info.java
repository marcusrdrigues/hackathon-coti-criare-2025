/**
 * Módulo Tempo real: WebSocket e STOMP: leva os eventos de compras a quem está conectado (ver docs/adr/0012).
 *
 * <p>Camadas: {@code dominio} (entidades e regras), {@code aplicacao} (casos de uso e portas) e
 * {@code infraestrutura} (web, persistência e integrações). Outros módulos só usam o que está
 * exposto como interface nomeada.
 */
@ApplicationModule(displayName = "Tempo real")
package com.gestao.temporeal;

import org.springframework.modulith.ApplicationModule;
