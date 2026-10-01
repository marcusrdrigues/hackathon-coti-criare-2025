package com.gestao.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoriaCotacao {
    TECNOLOGIA("Informática e TI"),
    MOBILIARIO("Mobiliário"),
    LIMPEZA_MANUTENCAO("Limpeza e Manutenção"),
    ALIMENTOS("Copa e Cozinha"),
    SUPRIMENTOS("Suprimentos de Escritório"),
    SERVICOS("Serviços Gerais"),
    OUTROS("Outros");

    private final String descricao;
}
