package com.gestao.identidade.dominio;

import java.util.UUID;

/** Uma pessoa saiu da organização: o que estiver aberto em nome dela precisa ser encerrado. */
public record MembroRemovidoEvento(UUID usuarioId) {
}
