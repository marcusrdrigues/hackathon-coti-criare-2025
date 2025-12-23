package com.gestao.mappers;

import com.gestao.entities.Empresa;
import com.gestao.dtos.empresa.EmpresaCadastroRequest;
import com.gestao.dtos.empresa.EmpresaResponse;
import org.springframework.stereotype.Component;

@Component
public class EmpresaMapper {

    public Empresa toEntity(EmpresaCadastroRequest request) {
        Empresa empresa = new Empresa();
        empresa.setRazaoSocial(request.razaoSocial());
        empresa.setCnpj(request.cnpj());
        empresa.setEmail(request.email());
        empresa.setSenha(request.senha());
        return empresa;
    }

    public EmpresaResponse toResponse(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getRazaoSocial(),
                empresa.getCnpj(),
                empresa.getEmail(),
                empresa.getPerfil().getNome()
        );
    }
}