/**
 * Módulo Auditoria: quem fez o quê na plataforma (spec 003). Guarda o histórico de alterações
 * (Hibernate Envers) e, nos passos seguintes, os eventos de segurança e o consumo de IA. Ouve os
 * outros módulos e lê o que eles gravaram; nenhum módulo depende dele para trabalhar.
 *
 * <p>Camadas: {@code dominio}, {@code aplicacao} (consultas e portas) e {@code infraestrutura}
 * (Envers, persistência e web).
 */
@ApplicationModule(displayName = "Auditoria")
package com.gestao.auditoria;

import org.springframework.modulith.ApplicationModule;
