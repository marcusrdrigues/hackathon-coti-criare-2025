package com.gestao.identidade.aplicacao;

import com.gestao.identidade.aplicacao.dto.PerfilResponse;
import com.gestao.identidade.dominio.Perfil;
import org.springframework.stereotype.Component;

@Component
public class PerfilMapper {

    public PerfilResponse toResponse(Perfil perfil) {
        return new PerfilResponse(
                perfil.getId(),
                perfil.getNome()
        );
    }
}