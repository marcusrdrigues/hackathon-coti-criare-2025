/**
 * Entidades, status e eventos de domínio de compras. Os eventos são publicados dentro da
 * transação e quem escuta só age depois do commit (ver docs/adr/0012).
 */
@NamedInterface("dominio")
package com.gestao.compras.dominio;

import org.springframework.modulith.NamedInterface;
