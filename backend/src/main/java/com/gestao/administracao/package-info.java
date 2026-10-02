/**
 * Módulo Administração: a visão do superadmin sobre a plataforma. Nesta fase, só leitura
 * (organizações com os totais), com consultas próprias no mesmo banco, como o painel.
 * A auditoria e o console completo vêm na fase 5.
 *
 * <p>Camadas: {@code aplicacao} (casos de uso e portas) e {@code infraestrutura} (web e
 * persistência). Não há regras de escrita, por isso não há domínio próprio.
 */
@ApplicationModule(displayName = "Administração")
package com.gestao.administracao;

import org.springframework.modulith.ApplicationModule;
