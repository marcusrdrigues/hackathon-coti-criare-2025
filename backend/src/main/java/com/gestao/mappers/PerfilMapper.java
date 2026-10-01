package com.gestao.mappers;

import com.gestao.dtos.perfil.PerfilResponse;
import com.gestao.entities.Perfil;
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