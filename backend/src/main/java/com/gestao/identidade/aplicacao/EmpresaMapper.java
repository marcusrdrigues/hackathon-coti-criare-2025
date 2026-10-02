package com.gestao.identidade.aplicacao;

import com.gestao.identidade.aplicacao.dto.EmpresaCadastroRequest;
import com.gestao.identidade.aplicacao.dto.EmpresaResponse;
import com.gestao.identidade.dominio.Empresa;
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
                empresa.getPerfil() != null ? empresa.getPerfil().getNome() : "EMPRESA"
        );
    }
}
