/**
 * Eventos de domínio publicados pelos services dentro da transação.
 * Quem escuta (ex.: o envio em tempo real) só age depois do commit, então
 * nunca avisa sobre algo que acabou sendo desfeito. Ver docs/adr/0012.
 */
package com.gestao.eventos;
